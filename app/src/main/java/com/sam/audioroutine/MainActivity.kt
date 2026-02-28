package com.sam.audioroutine

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.sam.audioroutine.ui.AudioRoutineRoot
import com.sam.audioroutine.ui.theme.AudioRoutineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var openPlaybackRequestNonce by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumePlaybackIntent(intent)
        enableEdgeToEdge()
        setContent {
            AudioRoutineTheme {
                AudioRoutineRoot(openPlaybackRequestNonce = openPlaybackRequestNonce)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumePlaybackIntent(intent)
    }

    private fun consumePlaybackIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYBACK, false) == true) {
            openPlaybackRequestNonce += 1
            intent.removeExtra(EXTRA_OPEN_PLAYBACK)
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYBACK = "extra_open_playback"
    }
}
