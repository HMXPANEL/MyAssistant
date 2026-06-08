package com.gitsync.presentation.devdiagnostics

import android.app.Application
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.GitRepository
import com.gitsync.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DevDiagnosticsState(
    val entries: List<Pair<String, String>> = emptyList()
)

@HiltViewModel
class DevDiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val gitRepository: GitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DevDiagnosticsState())
    val state: StateFlow<DevDiagnosticsState> = _state.asStateFlow()

    init {
        collectDiagnostics()
    }

    private fun collectDiagnostics() {
        viewModelScope.launch {
            val entries = mutableListOf<Pair<String, String>>()

            // App info
            entries.add("App Version" to "1.0.0")
            entries.add("Android SDK" to "${android.os.Build.VERSION.SDK_INT}")
            entries.add("Device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

            // Auth info
            entries.add("GitHub Username" to authRepository.getUsername())
            entries.add("Default Branch" to authRepository.getDefaultBranch())
            entries.add("Token Set" to "${authRepository.getToken().isNotBlank()}")

            // Storage info
            entries.add("External Storage State" to Environment.getExternalStorageState())
            entries.add("External Storage Dir" to (Environment.getExternalStorageDirectory()?.absolutePath ?: "N/A"))
            entries.add("Data Dir" to context.filesDir.absolutePath)
            entries.add("Cache Dir" to context.cacheDir.absolutePath)

            // Storage volumes
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            storageManager?.storageVolumes?.forEachIndexed { i, vol ->
                entries.add("Storage Volume $i" to "${vol.getDescription(context) ?: "unknown"} (${vol.uuid ?: "no-uuid"})")
            }

            // Projects
            val projects = projectRepository.getAllProjects().first()
            entries.add("Total Projects" to "${projects.size}")

            for (project in projects) {
                entries.add("---" to "---")
                entries.add("Project: ${project.name}" to "")
                entries.add("  Local Path" to project.localPath)
                entries.add("  SAF URI" to project.safUri.ifEmpty { "N/A" })
                entries.add("  Repo" to "${project.repoOwner}/${project.repoName}")
                entries.add("  Stored Branch" to project.branch)

                val dir = File(project.localPath)
                entries.add("  Dir Exists" to "${dir.exists()}")
                entries.add("  Dir Is Directory" to "${dir.isDirectory}")

                val gitDir = File(project.localPath, ".git")
                entries.add("  .git Exists" to "${gitDir.exists()}")

                val isRepo = gitRepository.isGitRepository(project.localPath)
                entries.add("  Is Git Repo" to "$isRepo")

                if (isRepo) {
                    val branch = gitRepository.getCurrentBranch(project.localPath)
                        .getOrDefault("ERROR")
                    entries.add("  Detected Branch" to branch)

                    val status = gitRepository.getSyncStatus(project.localPath)
                    entries.add("  Sync Status" to "${status.name}")

                    val remoteUrl = gitRepository.getOriginUrl(project.localPath)
                        .getOrDefault("N/A")
                    entries.add("  Remote Origin" to remoteUrl)
                }

                entries.add("  Last Sync" to if (project.lastSyncTime > 0)
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(project.lastSyncTime)) else "Never")
                entries.add("  Last Commit" to project.lastCommitHash.ifEmpty { "N/A" })
            }

            _state.value = DevDiagnosticsState(entries = entries)
        }
    }
}
