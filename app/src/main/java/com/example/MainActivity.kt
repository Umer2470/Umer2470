package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.StoreManagerTheme
import com.example.ui.viewmodel.StoreViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val storeViewModel: StoreViewModel = viewModel()
            val settings by storeViewModel.settings.collectAsState()

            StoreManagerTheme(
                themeMode = settings.themeMode,
                accentColorName = settings.accentColor
            ) {
                AppNavigation(viewModel = storeViewModel)
            }
        }
    }
}
