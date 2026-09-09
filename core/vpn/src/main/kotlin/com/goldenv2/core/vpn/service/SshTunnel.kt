package com.goldenv2.core.vpn.service

import android.content.Context
import android.util.Log
import com.goldenv2.core.domain.model.Server
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.future.AuthFuture
import org.apache.sshd.client.future.ConnectFuture
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.common.util.security.SecurityUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Establishes an SSH session to [server] and exposes it as a local SOCKS
 * proxy on 127.0.0.1:[LOCAL_SOCKS_PORT] via dynamic port forwarding
 * (equivalent of `ssh -D`). The Xray SOCKS outbound for SSH servers routes
 * through this tunnel.
 *
 * Android notes:
 * - Requires API 26+ for NIO2 (AsynchronousSocketChannel) used by MINA SSHD.
 * - All blocking I/O runs on Dispatchers.IO; callers must launch on IO.
 */
class SshTunnel(
    private val server: Server,
    private val context: Context
) {

    private var client: SshClient? = null
    private var session: ClientSession? = null
    private var boundAddress: SshdSocketAddress? = null

    private val TAG = "SshTunnel"

    @Volatile
    var isRunning: Boolean = false
        private set

    /**
     * Blocks until the SSH session authenticates and the local SOCKS proxy is bound.
     * Returns null on success, or an error message on failure (never throws).
     */
    fun start(): String? {
        val username = server.uuid
        if (username.isNullOrBlank()) return "SSH username is required"

        Log.d(TAG, "Starting SSH tunnel to ${server.address}:${server.port} as $username")

        // Set user.home to app's files directory to avoid "No user home" error
        // This must be done BEFORE SshClient.setUpDefaultClient() is called
        val homeDir = context.filesDir.absolutePath
        System.setProperty("user.home", homeDir)

        // Use setUpDefaultClient() for proper configuration
        val sshClient = SshClient.setUpDefaultClient()
        sshClient.serverKeyVerifier = AcceptAllServerKeyVerifier.INSTANCE

        try {
            sshClient.start()
            Log.d(TAG, "SSH client started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SSH client", e)
            return "Failed to start SSH client: ${e.message}"
        }

        // Connect with timeout
        val connectFuture: ConnectFuture
        try {
            Log.d(TAG, "Connecting to ${server.address}:${server.port}...")
            connectFuture = sshClient.connect(username, server.address, server.port)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate SSH connection", e)
            sshClient.stop()
            return "Failed to initiate SSH connection: ${e.message}"
        }

        val sess = try {
            connectFuture
                .verify(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .clientSession
                ?: run {
                    Log.w(TAG, "SSH connection timed out after ${CONNECT_TIMEOUT_SECONDS}s")
                    sshClient.stop()
                    return "SSH connection timed out after ${CONNECT_TIMEOUT_SECONDS}s"
                }
        } catch (e: Exception) {
            Log.e(TAG, "SSH connection failed", e)
            sshClient.stop()
            return "SSH connection failed: ${e.message}"
        }

        Log.d(TAG, "SSH connection established, authenticating...")

        try {
            // Password auth
            server.password?.takeIf { it.isNotEmpty() }?.let { sess.addPasswordIdentity(it) }

            // Key auth (if key file provided)
            server.path?.takeIf { it.isNotEmpty() }?.let { keyFile ->
                try {
                    val keyPair = SecurityUtils.loadKeyPairIdentities(
                        sess,
                        NamedResource.ofName(keyFile),
                        Files.newInputStream(Paths.get(keyFile)),
                        FilePasswordProvider.of(server.host ?: "")
                    ).firstOrNull() ?: return "Failed to load SSH key pair from $keyFile"
                    sess.addPublicKeyIdentity(keyPair)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load SSH key", e)
                    return "Failed to load SSH key: ${e.message}"
                }
            }

            // Authenticate
            val authFuture: AuthFuture = sess.auth()
            val authResult = authFuture.verify(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!authResult.isSuccess) {
                Log.w(TAG, "SSH authentication failed (timeout or rejected)")
                return "SSH authentication failed (timeout or rejected)"
            }

            Log.d(TAG, "SSH authentication successful, starting SOCKS proxy...")

            // Start dynamic port forwarding (SOCKS proxy)
            val bound = try {
                sess.startDynamicPortForwarding(
                    SshdSocketAddress(SshdSocketAddress.LOCALHOST_NAME, LOCAL_SOCKS_PORT)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SOCKS proxy", e)
                return "Failed to start SOCKS proxy: ${e.message}"
            }

            client = sshClient
            session = sess
            boundAddress = bound
            isRunning = true
            Log.d(TAG, "SSH tunnel started successfully on port $LOCAL_SOCKS_PORT")
            return null
        } catch (e: Exception) {
            // Clean up on any failure
            Log.e(TAG, "SSH setup failed", e)
            sess.close()
            sshClient.stop()
            return "SSH setup failed: ${e.message}"
        }
    }

    fun stop() {
        isRunning = false
        val sess = session
        val bound = boundAddress
        session = null
        boundAddress = null

        sess?.let { s ->
            bound?.let { runCatching { s.stopDynamicPortForwarding(it) } }
            runCatching { s.close() }
        }
        client?.let { runCatching { it.stop() } }
        client = null
    }

    companion object {
        const val LOCAL_SOCKS_PORT = 10808

        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val AUTH_TIMEOUT_SECONDS = 10L
    }
}