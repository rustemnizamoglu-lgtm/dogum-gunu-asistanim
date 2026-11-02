package com.dogumgunu.asistanim

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dogumgunu.asistanim.notify.NotificationHelper
import com.dogumgunu.asistanim.ui.AppRoot
import com.dogumgunu.asistanim.ui.AppTheme
import com.dogumgunu.asistanim.ui.AppViewModel

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }
            // Uygulama içi tema seçimine göre durum/gezinti çubuğu simgelerini ayarla.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xff, 0xff, 0xff),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { dark }
                )
                onDispose { }
            }
            AppTheme(dark = dark) {
                AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Bildirime dokunulduysa ilgili kişinin sayfasını aç. */
    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(NotificationHelper.EXTRA_PERSON_ID, -1L) ?: -1L
        if (id >= 0L) {
            vm.openFromNotification(id)
            intent?.removeExtra(NotificationHelper.EXTRA_PERSON_ID)
        }
    }
}
