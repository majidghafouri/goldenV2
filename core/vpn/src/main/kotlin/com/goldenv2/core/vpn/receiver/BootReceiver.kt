package com.goldenv2.core.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goldenv2.core.vpn.service.VpnServiceImpl

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            // Check if auto-connect is enabled in settings
            // TODO: Get from SettingsRepository
            val autoConnect = true // Placeholder

            if (autoConnect) {
                // Start VPN service
                val serviceIntent = Intent(context, VpnServiceImpl::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}