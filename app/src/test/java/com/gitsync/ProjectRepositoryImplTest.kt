package com.gitsync

import com.gitsync.data.local.dao.ProjectDao
import com.gitsync.data.local.entity.ProjectEntity
import com.gitsync.data.repository.ProjectRepositoryImpl
import com.gitsync.domain.model.Project
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectRepositoryImplTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var repository: ProjectRepositoryImpl

    @Before
    fun setup() {
        projectDao = mockk(relaxed = true)
        repository = ProjectRepositoryImpl(projectDao)
    }

    @Test
    fun `getAllProjects returns mapped projects`() = runTest {
        val entities = listOf(
            ProjectEntity(1, "Project1", "/path1", "owner1", "repo1", "main"),
            ProjectEntity(2, "Project2", "/path2", "owner2", "repo2", "dev")
        )

        coEvery { projectDao.getAllProjects() } returns flowOf(entities)

        val projects = repository.getAllProjects().first()

        assertEquals(2, projects.size)
        assertEquals("Project1", projects[0].name)
        assertEquals("Project2", projects[1].name)
        assertEquals("main", projects[0].branch)
        assertEquals("dev", projects[1].branch)
    }

    @Test
    fun `getProjectById returns mapped project`() = runTest {
        val entity = ProjectEntity(
            id = 1,
            name = "TestProject",
            localPath = "/test",
            repoOwner = "owner",
            repoName = "repo",
            branch = "main",
            lastCommitHash = "abc123",
            lastCommitMessage = "Initial commit"
        )

        coEvery { projectDao.getProjectById(1) } returns entity
        coEvery { projectDao.getProjectByIdFlow(1) } returns flowOf(entity)

        val project = repository.getProjectByIdOnce(1)

        assertNotNull(project)
        assertEquals("TestProject", project?.name)
        assertEquals("abc123", project?.lastCommitHash)
        assertEquals("Initial commit", project?.lastCommitMessage)
    }

    @Test
    fun `addProject delegates to DAO and returns id`() = runTest {
        coEvery { projectDao.insert(any()) } returns 42L

        val id = repository.addProject(
            name = "NewProject",
            localPath = "/new/path",
            repoOwner = "owner",
            repoName = "new-repo",
            branch = "main",
            uriPermission = "content://uri"
        )

        assertEquals(42L, id)
        coVerify { projectDao.insert(any()) }
    }

    @Test
    fun `removeProject delegates to DAO`() = runTest {
        repository.removeProject(1L)

        coVerify { projectDao.deleteById(1L) }
    }

    @Test
    fun `updateProject maps domain to entity correctly`() = runTest {
        val project = Project(
            id = 1,
            name = "UpdatedProject",
            localPath = "/updated",
            repoOwner = "owner",
            repoName = "repo",
            branch = "dev",
            lastSyncTime = 123456789L,
            lastCommitHash = "def456",
            lastCommitMessage = "Updated commit"
        )

        repository.updateProject(project)

        coVerify {
            projectDao.update(
                withArg { entity ->
                    assertTrue(entity.id == 1L)
                    assertTrue(entity.name == "UpdatedProject")
                    assertTrue(entity.lastCommitHash == "def456")
                }
            )
        }
    }
}
