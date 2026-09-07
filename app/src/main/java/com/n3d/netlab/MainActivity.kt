package com.n3d.netlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.n3d.netlab.ui.AppRoot
import com.n3d.netlab.ui.theme.NetLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Called before super so the window is already edge-to-edge when the
        // first frame is composed; doing it after leaves a system-bar-coloured
        // band on the first frame.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            // The last save of a session would otherwise be sitting in the sync
            // debounce when the app is put away — and a phone is put away far
            // more abruptly than a browser tab is closed.
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushSync() }
            NetLabTheme(vm.settings.theme) {
                AppRoot(vm)
            }
        }
    }
}
