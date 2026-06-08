package com.gitsync.data.repository

import com.gitsync.data.local.dao.ProjectDao
import com.gitsync.data.local.entity.ProjectEntity
import com.gitsync.domain.model.Project
import com.gitsync.domain.model.SyncStatus
import com.gitsync.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProjectById(id: Long): Flow<Project?> {
        return projectDao.getProjectByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getProjectByIdOnce(id: Long): Project? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    override suspend fun addProject(
        name: String,
        localPath: String,
        safUri: String,
        repoOwner: String,
        repoName: String,
        branch: String,
        uriPermission: String
    ): Long {
        val entity = ProjectEntity(
            name = name,
            localPath = localPath,
            safUri = safUri,
            repoOwner = repoOwner,
            repoName = repoName,
            branch = branch,
            uriPermission = uriPermission
        )
        return projectDao.insert(entity)
    }

    override suspend fun removeProject(id: Long) {
        projectDao.deleteById(id)
    }

    override suspend fun updateProject(project: Project) {
        val existing = projectDao.getProjectById(project.id)
        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            localPath = project.localPath,
            safUri = project.safUri.ifEmpty { existing?.safUri ?: "" },
            repoOwner = project.repoOwner,
            repoName = project.repoName,
            branch = project.branch,
            uriPermission = existing?.uriPermission ?: "",
            lastSyncTime = project.lastSyncTime,
            lastCommitHash = project.lastCommitHash,
            lastCommitMessage = project.lastCommitMessage
        )
        projectDao.update(entity)
    }

    override suspend fun getProjectByPath(path: String): Project? {
        return projectDao.getProjectByPath(path)?.toDomain()
    }

    private fun ProjectEntity.toDomain(): Project {
        return Project(
            id = id,
            name = name,
            localPath = localPath,
            safUri = safUri,
            repoOwner = repoOwner,
            repoName = repoName,
            branch = branch,
            lastSyncTime = lastSyncTime,
            lastCommitHash = lastCommitHash,
            lastCommitMessage = lastCommitMessage
        )
    }
}
