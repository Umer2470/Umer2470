package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.StoreViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: StoreViewModel by viewModels {
        StoreViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val accentColorIndex by viewModel.accentColorIndex.collectAsState()
            val fontScale by viewModel.fontSizeScale.collectAsState()
            val fontFamily by viewModel.fontFamilyChoice.collectAsState()

            MyApplicationTheme(
                themeMode = themeMode,
                accentColorIndex = accentColorIndex,
                fontScale = fontScale,
                fontFamilyChoice = fontFamily
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
