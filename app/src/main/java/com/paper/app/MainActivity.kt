package com.paper.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paper.app.data.JournalRepository
import com.paper.app.data.PasswordManager
import com.paper.app.data.ScheduleStore
import com.paper.app.notifications.ReminderScheduler
import com.paper.app.ui.screens.ChangePasswordScreen
import com.paper.app.ui.screens.EditorScreen
import com.paper.app.ui.screens.InfoScreen
import com.paper.app.ui.screens.JournalScreen
import com.paper.app.ui.screens.ScheduleScreen
import com.paper.app.ui.screens.SetupPasswordScreen
import com.paper.app.ui.screens.UnlockScreen
import com.paper.app.ui.theme.PaperTheme

object Routes {
    const val SETUP_PASSWORD = "setup_password"
    const val SETUP_SCHEDULE = "setup_schedule"
    const val UNLOCK = "unlock"
    const val JOURNAL = "journal"
    const val EDITOR = "editor"
    const val CHANGE_PASSWORD = "change_password"
    const val INFO = "info"
}

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val passwordManager = PasswordManager(this)
        val repository = JournalRepository(this)

        setContent {
            PaperTheme {
                var unlocked by remember { mutableStateOf(false) }
                var pendingEditor by remember {
                    mutableStateOf(intent?.getBooleanExtra(EXTRA_OPEN_EDITOR, false) == true)
                }
                val navController = rememberNavController()

                val startDestination = when {
                    !passwordManager.isPasswordSet() -> Routes.SETUP_PASSWORD
                    else -> Routes.UNLOCK
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable(Routes.SETUP_PASSWORD) {
                        SetupPasswordScreen(
                            onPasswordCreated = { password ->
                                passwordManager.setPassword(password)
                                unlocked = true
                                navController.navigate(Routes.SETUP_SCHEDULE) {
                                    popUpTo(Routes.SETUP_PASSWORD) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.SETUP_SCHEDULE) {
                        ScheduleScreen(
                            initial = ScheduleStore.load(this@MainActivity),
                            onSaved = { config ->
                                ScheduleStore.save(this@MainActivity, config)
                                ReminderScheduler.scheduleNext(this@MainActivity)
                                navController.navigate(Routes.JOURNAL) {
                                    popUpTo(Routes.SETUP_SCHEDULE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Routes.UNLOCK) {
                        UnlockScreen(
                            verify = passwordManager::verify,
                            onUnlocked = {
                                unlocked = true
                                val next = if (pendingEditor) Routes.EDITOR else Routes.JOURNAL
                                pendingEditor = false
                                navController.navigate(next) {
                                    popUpTo(Routes.UNLOCK) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.JOURNAL) {
                        if (!unlocked) return@composable
                        JournalScreen(
                            repository = repository,
                            onWrite = { navController.navigate(Routes.EDITOR) },
                            onEditSchedule = { navController.navigate(Routes.SETUP_SCHEDULE) },
                            onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                            onInfo = { navController.navigate(Routes.INFO) }
                        )
                    }

                    composable(Routes.CHANGE_PASSWORD) {
                        if (!unlocked) return@composable
                        ChangePasswordScreen(
                            verify = passwordManager::verify,
                            onChanged = { newPassword ->
                                passwordManager.setPassword(newPassword)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.INFO) {
                        if (!unlocked) return@composable
                        InfoScreen(onClose = { navController.popBackStack() })
                    }

                    composable(Routes.EDITOR) {
                        if (!unlocked) return@composable
                        EditorScreen(
                            onSave = { text ->
                                if (text.isNotBlank()) repository.addEntry(text)
                                navController.navigate(Routes.JOURNAL) {
                                    popUpTo(Routes.EDITOR) { inclusive = true }
                                }
                            },
                            onDiscard = {
                                navController.navigate(Routes.JOURNAL) {
                                    popUpTo(Routes.EDITOR) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Notification tapped while the app is alive: relaunch flow so the
        // unlock gate still applies before the editor opens.
        setIntent(intent)
        recreate()
    }

    companion object {
        const val EXTRA_OPEN_EDITOR = "open_editor"
    }
}
