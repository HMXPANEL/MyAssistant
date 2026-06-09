package com.gitsync.data.repository

import com.gitsync.domain.model.GitCommit
import com.gitsync.domain.model.SyncStatus
import com.gitsync.domain.repository.GitRepository
import kotlinx.coroutines.Dispatchers
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

@Singleton
class GitRepositoryImpl @Inject constructor() : GitRepository {

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
            val gitDir = File(repoDir, ".git")
            if (!gitDir.exists()) return SyncStatus.ERROR

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
    ): Result<String> {
        return try {
            val repoDir = File(projectPath)
            
            // Validate the path is accessible
            if (!repoDir.exists()) {
                repoDir.mkdirs()
            }
            if (!repoDir.canRead()) {
                return Result.failure(Exception(
                    "Cannot read folder. Grant 'All Files Access' in:\n" +
                    "Settings > Apps > GitSync > Permissions > Files and media > Allow management of all files"
                ))
            }
            if (!repoDir.canWrite()) {
                return Result.failure(Exception(
                    "Cannot write to folder. Grant 'All Files Access' in:\n" +
                    "Settings > Apps > GitSync > Permissions > Files and media"
                ))
            }

            // Step 1: Init or open repo
            val isExistingRepo = File(repoDir, ".git").exists()
            val git = if (isExistingRepo) {
                Git.open(repoDir)
            } else {
                Git.init().setDirectory(repoDir).call()
            }

            git.use { g ->
                // Step 2: Set or update remote
                val remotes = g.remoteList().call()
                val hasOrigin = remotes.any { it.name == "origin" }
                if (hasOrigin) {
                    g.remoteSetUrl()
                        .setRemoteName("origin")
                        .setRemoteUri(URIish(remoteUrl))
                        .call()
                } else {
                    g.remoteAdd()
                        .setName("origin")
                        .setUri(URIish(remoteUrl))
                        .call()
                }

                // Step 3: Stage all files
                g.add().addFilepattern(".").call()

                // Step 4: Also stage deletions
                g.add().addFilepattern(".").setUpdate(true).call()

                // Step 5: Check if anything was staged
                val status = g.status().call()
                val hasAnythingToCommit = !status.isClean ||
                    status.added.isNotEmpty() ||
                    status.modified.isNotEmpty() ||
                    status.untracked.isNotEmpty()

                val commitHash = if (hasAnythingToCommit) {
                    val commit = g.commit()
                        .setMessage("Initial commit from GitSync")
                        .setAuthor(username.ifBlank { "GitSync" }, "gitsync@local.dev")
                        .call()
                    commit.name
                } else {
                    // Repo is clean — check if there are existing commits to push
                    try {
                        val logs = g.log().setMaxCount(1).call().toList()
                        if (logs.isEmpty()) {
                            // Truly empty repo — create a README and commit it
                            val readme = File(repoDir, "README.md")
                            if (!readme.exists()) {
                                readme.writeText("# ${repoDir.name}\n\nCreated by GitSync")
                            }
                            g.add().addFilepattern("README.md").call()
                            val commit = g.commit()
                                .setMessage("Initial commit from GitSync")
                                .setAuthor(username.ifBlank { "GitSync" }, "gitsync@local.dev")
                                .call()
                            commit.name
                        } else {
                            logs[0].name
                        }
                    } catch (_: Exception) {
                        // org.eclipse.jgit.api.errors.NoHeadException — totally empty repo
                        val readme = File(repoDir, "README.md")
                        if (!readme.exists()) {
                            readme.writeText("# ${repoDir.name}\n\nCreated by GitSync")
                        }
                        g.add().addFilepattern("README.md").call()
                        val commit = g.commit()
                            .setMessage("Initial commit from GitSync")
                            .setAuthor(username.ifBlank { "GitSync" }, "gitsync@local.dev")
                            .call()
                        commit.name
                    }
                }

                // Step 6: Push
                val credentials = UsernamePasswordCredentialsProvider(username, token)
                g.push()
                    .setCredentialsProvider(credentials)
                    .setRemote("origin")
                    .setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch"))
                    .call()

                Result.success(commitHash)
            }
        } catch (e: org.eclipse.jgit.api.errors.TransportException) {
            val msg = e.message ?: ""
            when {
                msg.contains("401") || msg.contains("not authorized", ignoreCase = true) ->
                    Result.failure(Exception("Push failed: Invalid GitHub token. Check your PAT has 'repo' scope."))
                msg.contains("not found", ignoreCase = true) || msg.contains("404") ->
                    Result.failure(Exception("Push failed: Repository not found on GitHub. Create it first or check owner/name."))
                else ->
                    Result.failure(Exception("Push failed: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Setup failed: ${e.message}"))
        }
    }
}
