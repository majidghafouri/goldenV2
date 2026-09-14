package com.goldenv2.feature.servers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Bottom sheet for sharing a server config: copy the share URI to the
 * clipboard, open the Android share picker, or show the config as a QR
 * code scannable by another device.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ShareServerSheet(
    configUri: String,
    serverName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("server config", configUri))
        copied = true
    }

    fun shareViaApps() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, configUri)
            putExtra(Intent.EXTRA_TITLE, serverName)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share server config"))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Share \"$serverName\"",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = configUri,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                minLines = 2,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ListItem(
                    headlineContent = { Text(if (copied) "Copied to clipboard" else "Copy to clipboard") },
                    supportingContent = { Text("Paste it anywhere to share the config") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { copyToClipboard() }
                )
                ListItem(
                    headlineContent = { Text("Share via apps") },
                    supportingContent = { Text("Open the system share sheet") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { shareViaApps() }
                )
                ListItem(
                    headlineContent = { Text(if (showQr) "Hide QR code" else "Show as QR code") },
                    supportingContent = { Text("Scan with another device camera") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showQr = !showQr }
                )
            }

            if (showQr) {
                Spacer(modifier = Modifier.height(16.dp))
                QrCodeCard(configUri = configUri)
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun QrCodeCard(configUri: String) {
    val qrBitmap = remember(configUri) { generateQrBitmap(configUri) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Config QR code",
                modifier = Modifier.size(280.dp)
            )
        } else {
            Text(
                text = "Config too long for a QR code",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Generates a QR code bitmap for [content], or null when the content
 * exceeds the QR code capacity for the chosen error-correction level.
 */
private fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val matrix: BitMatrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 840, 840, hints
        )
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x, y]) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
    } catch (e: Exception) {
        null
    }
}
