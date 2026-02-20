package com.sam.audioroutine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sam.audioroutine.ui.AudioRoutineRoot
import com.sam.audioroutine.ui.theme.AudioRoutineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioRoutineTheme {
                AudioRoutineRoot()
            }
        }
    }
}
