package com.washinson.yaradio3.common

import androidx.appcompat.app.AlertDialog
import android.content.*
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.annotation.RequiresApi

class DisableBatterySaverDialog {
    companion object {
        @RequiresApi(Build.VERSION_CODES.M)
        fun create(context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setCancelable(false)
                setTitle(android.R.string.dialog_alert_title)
                setMessage("Android оптимизирует работу батареи. Для стабильной работы рекомендуется разрешить работу в фоне.\nРазрешить?")
                setPositiveButton(android.R.string.yes) { _: DialogInterface, _: Int ->
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + context.getPackageName())
                        )
                    )
                }
                setNegativeButton(android.R.string.no) { _: DialogInterface, _: Int -> }
            }.create()
        }
    }
}