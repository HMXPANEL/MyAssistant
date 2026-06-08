package com.gitsync

import com.gitsync.data.repository.GitRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class GitRepositoryImplTest {

    private lateinit var repository: GitRepositoryImpl
    private lateinit var tempDir: File
    private lateinit var gitDir: File

    @Before
    fun setup() {
        repository = GitRepositoryImpl()
        tempDir = createTempDir("gitsync-test-")
        gitDir = File(tempDir, ".git")
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `isGitRepository returns false for non-existent directory`() = runTest {
        val result = repository.isGitRepository("/nonexistent/path")
        assertFalse(result)
    }

    @Test
    fun `isGitRepository returns false for directory without git`() = runTest {
        val result = repository.isGitRepository(tempDir.absolutePath)
        assertFalse(result)
    }

    @Test
    fun `isGitRepository returns false for empty git directory`() = runTest {
        gitDir.mkdir()
        val result = repository.isGitRepository(tempDir.absolutePath)
        assertFalse(result)
    }

    @Test
    fun `isGitRepository returns true for valid git repository`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        val result = repository.isGitRepository(tempDir.absolutePath)
        assertTrue(result)
    }

    @Test
    fun `hasGitDirectory returns false for non-existent dir`() = runTest {
        assertFalse(repository.hasGitDirectory("/nonexistent"))
    }

    @Test
    fun `hasGitDirectory returns true when dotgit exists`() = runTest {
        gitDir.mkdir()
        assertTrue(repository.hasGitDirectory(tempDir.absolutePath))
    }

    @Test
    fun `initRepository creates valid git repo`() = runTest {
        val result = repository.initRepository(tempDir.absolutePath)
        assertTrue(result.isSuccess)
        assertTrue(gitDir.exists())
    }

    @Test
    fun `initRepository creates repo with valid structure`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        assertTrue(File(gitDir, "config").exists())
        assertTrue(File(gitDir, "objects").exists())
        assertTrue(File(gitDir, "refs").exists())
    }

    @Test
    fun `validateRepository succeeds for valid repo`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        val result = repository.validateRepository(tempDir.absolutePath)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateRepository fails for missing dir`() = runTest {
        val result = repository.validateRepository("/nonexistent")
        assertTrue(result.isFailure)
    }

    @Test
    fun `validateRepository fails for dir without git`() = runTest {
        val result = repository.validateRepository(tempDir.absolutePath)
        assertTrue(result.isFailure)
    }

    @Test
    fun `hasChanges returns false for clean repo`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        val result = repository.hasChanges(tempDir.absolutePath)
        assertTrue(result.isSuccess)
        assertFalse(result.getOrDefault(true))
    }

    @Test
    fun `hasChanges returns true for modified repo`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "test.txt").writeText("hello")
        val result = repository.hasChanges(tempDir.absolutePath)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }

    @Test
    fun `commit fails when nothing to commit`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        val result = repository.commit(tempDir.absolutePath, "test commit")
        assertTrue(result.isFailure)
    }

    @Test
    fun `commit succeeds with changes`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "test.txt").writeText("hello")
        repository.addAll(tempDir.absolutePath)
        val result = repository.commit(tempDir.absolutePath, "test commit")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getCurrentBranch returns initial branch`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "test.txt").writeText("hello")
        repository.addAll(tempDir.absolutePath)
        repository.commit(tempDir.absolutePath, "init")
        val branch = repository.getCurrentBranch(tempDir.absolutePath)
        assertTrue(branch.isSuccess)
        assertTrue(branch.getOrNull() in listOf("main", "master"))
    }

    @Test
    fun `getBranches returns list with at least one branch after commit`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "test.txt").writeText("hello")
        repository.addAll(tempDir.absolutePath)
        repository.commit(tempDir.absolutePath, "init")
        val branches = repository.getBranches(tempDir.absolutePath)
        assertTrue(branches.isSuccess)
        assertTrue(branches.getOrDefault(emptyList()).isNotEmpty())
    }

    @Test
    fun `getRecentCommits returns commits`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "test.txt").writeText("hello")
        repository.addAll(tempDir.absolutePath)
        repository.commit(tempDir.absolutePath, "first commit")
        val commits = repository.getRecentCommits(tempDir.absolutePath, 10)
        assertTrue(commits.isSuccess)
        assertEquals(1, commits.getOrNull()?.size)
        assertEquals("first commit", commits.getOrNull()?.first()?.message)
    }

    @Test
    fun `getStatus returns modified files`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "newfile.txt").writeText("content")
        val status = repository.getStatus(tempDir.absolutePath)
        assertTrue(status.isSuccess)
        val files = status.getOrDefault(emptyList())
        assertTrue(files.any { it.contains("newfile.txt") })
    }

    @Test
    fun `getModifiedFiles returns changed file paths`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "changed.txt").writeText("content")
        val files = repository.getModifiedFiles(tempDir.absolutePath)
        assertTrue(files.isSuccess)
        assertTrue(files.getOrDefault(emptyList()).contains("changed.txt"))
    }

    @Test
    fun `getSyncStatus returns ERROR for non-repo`() = runTest {
        val status = repository.getSyncStatus(tempDir.absolutePath)
        assertEquals(com.gitsync.domain.model.SyncStatus.ERROR, status)
    }

    @Test
    fun `getSyncStatus returns SYNCED for clean repo`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "f.txt").writeText("x")
        repository.addAll(tempDir.absolutePath)
        repository.commit(tempDir.absolutePath, "init")
        val status = repository.getSyncStatus(tempDir.absolutePath)
        assertEquals(com.gitsync.domain.model.SyncStatus.SYNCED, status)
    }

    @Test
    fun `getSyncStatus returns PENDING for dirty repo`() = runTest {
        repository.initRepository(tempDir.absolutePath)
        File(tempDir, "f.txt").writeText("x")
        repository.addAll(tempDir.absolutePath)
        repository.commit(tempDir.absolutePath, "init")
        File(tempDir, "f.txt").writeText("modified")
        val status = repository.getSyncStatus(tempDir.absolutePath)
        assertEquals(com.gitsync.domain.model.SyncStatus.PENDING, status)
    }
}
