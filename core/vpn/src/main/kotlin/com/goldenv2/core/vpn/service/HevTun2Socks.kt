package com.goldenv2.core.vpn.service

/**
 * JNI wrapper for hev-socks5-tunnel (MIT licensed, https://github.com/heiher/hev-socks5-tunnel).
 *
 * The native library reads packets from the VPN interface fd passed via
 * [TProxyStartService] and forwards them to the Xray process via a SOCKS5
 * connection on 127.0.0.1.
 *
 * Native method signatures (hev-jni.c RegisterNatives):
 *   TProxyStartService(String configPath, int fd): Boolean
 *   TProxyStopService(): Boolean
 *   TProxyIsRunning(): Boolean
 *   TProxyGetStats(): LongArray  -- [txPackets, txBytes, rxPackets, rxBytes]
 */
object HevTun2Socks {

    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStartService(configPath: String, fd: Int): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStopService(): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyIsRunning(): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyGetStats(): LongArray?
}