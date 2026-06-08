package com.gitsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.gitsync.core.ui.theme.GitSyncTheme
import com.gitsync.presentation.splash.SplashState
import com.gitsync.domain.model.SyncInterval
import com.gitsync.domain.repository.SettingsRepository
import com.gitsync.presentation.navigation.NavGraph
import com.gitsync.presentation.splash.SplashViewModel
import com.gitsync.worker.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsRepository.isDarkTheme.collectAsStateWithLifecycle(
                initialValue = true
            )

            GitSyncTheme(darkTheme = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val splashViewModel: SplashViewModel = hiltViewModel()
                    val splashState by splashViewModel.state.collectAsStateWithLifecycle(
                        initialValue = SplashState()
                    )

                    NavGraph(
                        navController = navController,
                        startDestination = "splash"
                    )
                }
            }

            // Observe sync interval changes to schedule/unschedule work
            val syncInterval by settingsRepository.syncInterval.collectAsStateWithLifecycle(
                initialValue = SyncInterval.DISABLED
            )

            LaunchedEffect(syncInterval) {
                SyncScheduler.scheduleSync(
                    context = this@MainActivity,
                    interval = syncInterval
                )
            }
        }
    }
}
