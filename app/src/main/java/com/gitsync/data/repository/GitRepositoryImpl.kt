package com.gitsync.data.repository

import android.util.Base64
import com.gitsync.core.network.GitHubApi
import com.gitsync.core.security.SecureStorage
import com.gitsync.data.remote.dto.CreateBlobRequestDto
import com.gitsync.data.remote.dto.CreateCommitRequestDto
import com.gitsync.data.remote.dto.CreateRefRequestDto
import com.gitsync.data.remote.dto.CreateRepoRequestDto
import com.gitsync.data.remote.dto.CreateTreeRequestDto
import com.gitsync.data.remote.dto.FileContentRequestDto
import com.gitsync.data.remote.dto.RefObjectDto
import com.gitsync.data.remote.dto.RefResponseDto
import com.gitsync.data.remote.dto.TreeEntryDto
import com.gitsync.data.remote.dto.UpdateRefRequestDto
import kotlin.runCatching
import com.gitsync.domain.model.GitCommit
import com.gitsync.domain.model.SyncStatus
import com.gitsync.domain.repository.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class GitRepositoryImpl @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val secureStorage: SecureStorage
) : GitRepository {

    override suspend fun isGitRepository(projectPath: String): Boolean {
        return try {
            val repoDir = File(projectPath)
            if (!repoDir.isDirectory) return false
            val gitDir = File(repoDir, ".git")
            if (!gitDir.exists()) return false
            // Check read permission explicitly before JGit tries
            if (!repoDir.canRead()) return false
            Git.open(repoDir).use { true }
        } catch (e: SecurityException) {
            false
        } catch (e: java.io.IOException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun hasGitDirectory(projectPath: String): Boolean {
        return try {
            val repoDir = File(projectPath)
            if (!repoDir.isDirectory) return false
            val gitDir = File(repoDir, ".git")
            gitDir.exists()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun validateRepository(projectPath: String): Result<Unit> {
        return try {
            val repoDir = File(projectPath)
            if (!repoDir.isDirectory) {
                return Result.failure(Exception("Directory does not exist: $projectPath"))
            }
            val gitDir = File(repoDir, ".git")
            if (!gitDir.exists()) {
                return Result.failure(Exception("Not a Git repository: .git directory not found"))
            }
            Git.open(repoDir).use { repo ->
                repo.status().call()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Corrupted or invalid repository: ${e.message}"))
        }
    }

    override suspend fun initRepository(projectPath: String): Result<Unit> {
        return try {
            val repoDir = File(projectPath)
            if (!repoDir.exists()) {
                repoDir.mkdirs()
            }
            Git.init().setDirectory(repoDir).call()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatus(projectPath: String): Result<List<String>> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val status: Status = git.status().call()
                val changes = mutableListOf<String>()
                changes.addAll(status.getModified().map { "M  $it" })
                changes.addAll(status.getUntracked().map { "?  $it" })
                changes.addAll(status.getAdded().map { "A  $it" })
                changes.addAll(status.getChanged().map { "C  $it" })
                changes.addAll(status.getRemoved().map { "D  $it" })
                Result.success(changes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasChanges(projectPath: String): Result<Boolean> {
        return try {
            val repoDir = File(projectPath)
            val gitDir = File(repoDir, ".git")
            // No local .git means REST API project — track changes by file modification times
            if (!gitDir.exists()) return Result.success(false)
            
            Git.open(repoDir).use { git ->
                val status = git.status().call()
                Result.success(!status.isClean)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addAll(projectPath: String): Result<Unit> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                git.add().addFilepattern(".").call()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun commit(projectPath: String, message: String): Result<String> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val status = git.status().call()
                if (status.isClean) {
                    return Result.failure(Exception("Nothing to commit - working tree clean"))
                }
                val commit = git.commit()
                    .setMessage(message)
                    .setAuthor("GitSync", "gitsync@local.dev")
                    .call()
                Result.success(commit.name)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun push(
        projectPath: String,
        username: String,
        token: String,
        branch: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val repoDir = File(projectPath)
                Git.open(repoDir).use { git ->
                    val credentials = UsernamePasswordCredentialsProvider(username, token)
                    git.push()
                        .setCredentialsProvider(credentials)
                        .setRemote("origin")
                        .setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch"))
                        .setTimeout(60_000)
                        .call()
                }
                Result.success(Unit)
            } catch (e: NoRemoteRepositoryException) {
                Result.failure(Exception("Remote repository not found. Check owner/repo name."))
            } catch (e: TransportException) {
                val msg = e.message ?: ""
                when {
                    msg.contains("not authorized", ignoreCase = true) ||
                    msg.contains("401", ignoreCase = true) ->
                        Result.failure(Exception("Authentication failed. Check your GitHub token."))
                    msg.contains("not found", ignoreCase = true) ||
                    msg.contains("404", ignoreCase = true) ->
                        Result.failure(Exception("Remote repository not found. Check owner/repo name."))
                    msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("timed out", ignoreCase = true) ->
                        Result.failure(Exception("Connection timed out. Check your internet connection."))
                    else ->
                        Result.failure(Exception("Push failed: ${e.message}"))
                }
            } catch (e: UnknownHostException) {
                Result.failure(Exception("No internet connection. Check your network."))
            } catch (e: ConnectException) {
                Result.failure(Exception("Cannot connect to GitHub. Check your internet connection."))
            } catch (e: Exception) {
                Result.failure(Exception("Push failed: ${e.message}"))
            }
        }
    }

    override suspend fun pull(
        projectPath: String,
        username: String,
        token: String,
        branch: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val repoDir = File(projectPath)
                Git.open(repoDir).use { git ->
                    val credentials = UsernamePasswordCredentialsProvider(username, token)
                    git.pull()
                        .setCredentialsProvider(credentials)
                        .setRemoteBranchName(branch)
                        .setTimeout(60_000)
                        .call()
                }
                Result.success(Unit)
            } catch (e: NoRemoteRepositoryException) {
                Result.failure(Exception("Remote repository not found. Check owner/repo name."))
            } catch (e: TransportException) {
                val msg = e.message ?: ""
                when {
                    msg.contains("not authorized", ignoreCase = true) ||
                    msg.contains("401", ignoreCase = true) ->
                        Result.failure(Exception("Authentication failed. Check your GitHub token."))
                    msg.contains("not found", ignoreCase = true) ||
                    msg.contains("404", ignoreCase = true) ->
                        Result.failure(Exception("Remote repository not found. Check owner/repo name."))
                    msg.contains("timeout", ignoreCase = true) ->
                        Result.failure(Exception("Connection timed out. Check your internet connection."))
                    else ->
                        Result.failure(Exception("Pull failed: ${e.message}"))
                }
            } catch (e: UnknownHostException) {
                Result.failure(Exception("No internet connection. Check your network."))
            } catch (e: ConnectException) {
                Result.failure(Exception("Cannot connect to GitHub. Check your internet connection."))
            } catch (e: Exception) {
                Result.failure(Exception("Pull failed: ${e.message}"))
            }
        }
    }

    override suspend fun getCurrentBranch(projectPath: String): Result<String> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val fullBranch = git.repository.fullBranch
                val branch = when {
                    fullBranch == null || fullBranch.isEmpty() ->
                        return Result.failure(Exception("No branch found"))
                    fullBranch.startsWith("refs/heads/") ->
                        fullBranch.removePrefix("refs/heads/")
                    else -> fullBranch
                }
                Result.success(branch)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBranches(projectPath: String): Result<List<String>> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val branches = git.branchList().call().map { ref ->
                    ref.name.removePrefix("refs/heads/")
                }
                Result.success(branches)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentCommits(
        projectPath: String,
        maxCount: Int
    ): Result<List<GitCommit>> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val commits = git.log()
                    .setMaxCount(maxCount)
                    .call()
                    .map { rev ->
                        GitCommit(
                            hash = rev.name.take(7),
                            message = rev.shortMessage,
                            author = rev.authorIdent.name,
                            timestamp = rev.commitTime * 1000L
                        )
                    }
                Result.success(commits)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSyncStatus(projectPath: String): SyncStatus {
        return try {
            val repoDir = File(projectPath)
            if (!repoDir.isDirectory) return SyncStatus.ERROR
            // If no .git folder exists, this project uses REST API sync — no local changes possible
            val gitDir = File(repoDir, ".git")
            if (!gitDir.exists()) return SyncStatus.SYNCED

            Git.open(repoDir).use { git ->
                val status: Status = git.status().call()
                if (status.isClean) SyncStatus.SYNCED else SyncStatus.PENDING
            }
        } catch (e: Exception) {
            SyncStatus.ERROR
        }
    }

    override suspend fun getModifiedFiles(projectPath: String): Result<List<String>> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val status: Status = git.status().call()
                val modified = mutableListOf<String>()
                modified.addAll(status.getModified())
                modified.addAll(status.getUntracked())
                modified.addAll(status.getChanged())
                modified.addAll(status.getAdded())
                modified.addAll(status.getRemoved())
                modified.addAll(status.getMissing())
                Result.success(modified)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOriginUrl(projectPath: String): Result<String> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                val remoteConfig = git.remoteList().call()
                val origin = remoteConfig.find { it.name == "origin" }
                val url = origin?.getURIs()?.firstOrNull()?.toString() ?: ""
                Result.success(url)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setOriginUrl(projectPath: String, url: String): Result<Unit> {
        return try {
            val repoDir = File(projectPath)
            Git.open(repoDir).use { git ->
                git.remoteSetUrl()
                    .setRemoteName("origin")
                    .setRemoteUri(org.eclipse.jgit.transport.URIish(url))
                    .call()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRemoteUrl(projectPath: String): Result<String> {
        return getOriginUrl(projectPath)
    }

    override suspend fun setupAndPushProject(
        projectPath: String,
        remoteUrl: String,
        username: String,
        token: String,
        branch: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val repoDir = File(projectPath)

            if (!repoDir.exists()) repoDir.mkdirs()
            if (!repoDir.canRead()) {
                return@withContext Result.failure(Exception(
                    "Cannot read folder.\nGo to: Settings > Apps > GitSync > Permissions\n" +
                    "Enable: Files and media > Allow management of all files"
                ))
            }

            val urlParts = remoteUrl
                .removePrefix("https://github.com/")
                .removeSuffix(".git")
                .split("/")
            if (urlParts.size < 2) {
                return@withContext Result.failure(Exception("Invalid URL: $remoteUrl"))
            }
            val owner = urlParts[0]
            val repo  = urlParts[1]

            // Step 1: Create repo if it doesn't exist
            var repoIsNew = false
            try {
                gitHubApi.getRepository(owner, repo)
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    try {
                        gitHubApi.createRepository(
                            CreateRepoRequestDto(
                                name = repo,
                                description = "Synced by GitSync",
                                isPrivate = false
                            )
                        )
                        repoIsNew = true
                        delay(2000) // GitHub needs time to provision the Git DB
                    } catch (ce: HttpException) {
                        return@withContext Result.failure(Exception(
                            "Could not create repo '$owner/$repo'.\n" +
                            "Ensure your PAT has 'repo' scope.\nError: ${ce.code()}"
                        ))
                    }
                }
            }

            // Step 2: Check if repo has any existing commits (could be pre-existing empty repo)
            val existingParentSha: String? = try {
                gitHubApi.getRef(owner, repo, branch).refObject?.sha
            } catch (e: HttpException) {
                if (e.code() == 404 || e.code() == 409) null else throw e
            } catch (_: Exception) {
                null
            }

            val repoHasNoCommits = existingParentSha == null

            // Step 3: If repo has no commits at all (new or pre-existing empty),
            // we MUST create an initial commit via Contents API first.
            // The Git Tree API (createBlob/createTree) returns 409 on repos with no commits.
            val initialCommitSha: String? = if (repoHasNoCommits) {
                try {
                    // Use Contents API to create README - this initializes the Git DB
                    val readmeContent = android.util.Base64.encodeToString(
                        "# $repo\n\nSynced by GitSync".toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    val result = gitHubApi.putFileContent(
                        owner, repo, "README.md",
                        com.gitsync.data.remote.dto.FileContentRequestDto(
                            message = "Initial commit via GitSync",
                            content = readmeContent,
                            sha = null
                        )
                    )
                    delay(1000) // Give GitHub time to update the ref
                    result.commit?.sha
                } catch (e: Exception) {
                    android.util.Log.w("GitSync", "Initial README commit failed: ${e.message}")
                    null
                }
            } else null

            // Step 4: Get the current HEAD sha (after potential initial commit)
            val parentSha: String? = initialCommitSha ?: existingParentSha ?: try {
                gitHubApi.getRef(owner, repo, branch).refObject?.sha
            } catch (_: Exception) { null }

            // If repo has no commits and we failed to create initial commit, we cannot proceed
            // because Git Tree API (createBlob/createTree) returns 409 on repos with no commits
            if (repoHasNoCommits && initialCommitSha == null && parentSha == null) {
                return@withContext Result.failure(Exception(
                    "Repository is empty and failed to initialize. " +
                    "Please delete the GitHub repo and try again, or ensure your PAT has 'repo' scope."
                ))
            }

            // Step 5: Collect files to push
            val filesToPush = repoDir.walkTopDown()
                .filter { it.isFile }
                .filter { file ->
                    val rel = file.relativeTo(repoDir).path.replace("\\", "/")
                    !rel.startsWith(".git/") &&
                    !rel.startsWith(".gradle/") &&
                    !rel.startsWith("build/") &&
                    !rel.startsWith(".idea/") &&
                    !rel.startsWith("captures/") &&
                    !rel.endsWith(".class") &&
                    !rel.endsWith(".dex") &&
                    file.length() < 5 * 1024 * 1024
                }
                .toList()

            if (filesToPush.isEmpty()) {
                // Nothing to push — the README we created counts as the sync
                return@withContext Result.success(parentSha?.take(7) ?: "empty")
            }

            // Step 6: Upload blobs in chunks of 20 (rate limit protection)
            val treeEntries = mutableListOf<TreeEntryDto>()
            var lastBlobError: Exception? = null

            for (chunk in filesToPush.chunked(20)) {
                coroutineScope {
                    chunk.map { file ->
                        async {
                            try {
                                val rel = file.relativeTo(repoDir).path.replace("\\", "/")
                                val base64 = android.util.Base64.encodeToString(
                                    file.readBytes(), android.util.Base64.NO_WRAP
                                )
                                val blobResp = gitHubApi.createBlob(
                                    owner, repo,
                                    CreateBlobRequestDto(content = base64, encoding = "base64")
                                )
                                synchronized(treeEntries) {
                                    treeEntries.add(TreeEntryDto(path = rel, sha = blobResp.sha))
                                }
                            } catch (e: HttpException) {
                                lastBlobError = e
                                android.util.Log.e("GitSync", "Blob failed ${file.name}: ${e.code()}")
                            } catch (e: Exception) {
                                lastBlobError = e
                                android.util.Log.e("GitSync", "Blob failed ${file.name}: ${e.message}")
                            }
                        }
                    }.awaitAll()
                }
            }

            if (treeEntries.isEmpty()) {
                val msg = when (val err = lastBlobError) {
                    is HttpException -> when (err.code()) {
                        401 -> "Authentication failed (401). Check your Personal Access Token."
                        403 -> "Access forbidden (403). Ensure PAT has 'repo' scope."
                        409 -> "GitHub error 409: Repository initialization failed. Please delete the GitHub repo and try again."
                        else -> "GitHub error ${err.code()}: ${err.response()?.errorBody()?.string() ?: err.message}"
                    }
                    null -> "No uploadable files found. Project may only contain build artifacts."
                    else -> "Upload failed: ${err.message}"
                }
                return@withContext Result.failure(Exception(msg))
            }

            // Step 7: Create tree
            val treeResp = gitHubApi.createTree(
                owner, repo,
                CreateTreeRequestDto(
                    baseTree = parentSha, // null = new tree from scratch
                    tree = treeEntries
                )
            )

            // Step 8: Create commit
            val commitResp = gitHubApi.createCommit(
                owner, repo,
                CreateCommitRequestDto(
                    message = "Sync from GitSync\n\n${treeEntries.size} files",
                    tree = treeResp.sha,
                    parents = if (parentSha != null) listOf(parentSha) else emptyList()
                )
            )

            // Step 9: Create or update the branch ref
            // New repo (no prior ref) = POST to CREATE. Existing repo = PATCH to UPDATE.
            try {
                if (repoHasNoCommits && initialCommitSha == null) {
                    // Truly brand new ref — create it
                    gitHubApi.createRef(
                        owner, repo,
                        CreateRefRequestDto(
                            ref = "refs/heads/$branch",
                            sha = commitResp.sha
                        )
                    )
                } else {
                    // Ref exists — update it
                    gitHubApi.updateRef(owner, repo, branch,
                        UpdateRefRequestDto(sha = commitResp.sha, force = true)
                    )
                }
            } catch (e: HttpException) {
                // 422 = ref already exists (race condition) — try update instead
                if (e.code() == 422) {
                    try {
                        gitHubApi.updateRef(owner, repo, branch,
                            UpdateRefRequestDto(sha = commitResp.sha, force = true)
                        )
                    } catch (ue: Exception) {
                        android.util.Log.w("GitSync", "updateRef fallback failed: ${ue.message}")
                    }
                } else {
                    android.util.Log.w("GitSync", "ref operation failed: ${e.code()} ${e.message}")
                }
            }

            android.util.Log.i("GitSync", "Push complete: ${treeEntries.size} files, commit ${commitResp.sha.take(7)}")
            Result.success(commitResp.sha.take(7))

        } catch (e: HttpException) {
            val code = e.code()
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: ""
            when (code) {
                401 -> Result.failure(Exception("GitHub auth failed (401). Check your Personal Access Token."))
                403 -> Result.failure(Exception("Access forbidden (403). Ensure PAT has 'repo' scope."))
                404 -> Result.failure(Exception("Repository not found (404). Check owner/repo name."))
                409 -> Result.failure(Exception("Repository not ready (409). Wait a moment and try again."))
                422 -> Result.failure(Exception("GitHub rejected request (422): $body"))
                else -> Result.failure(Exception("GitHub error $code: $body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Push failed: ${e.message}"))
        }
    }
}
