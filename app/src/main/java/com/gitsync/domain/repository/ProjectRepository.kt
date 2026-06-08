package com.gitsync.domain.repository

import com.gitsync.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    suspend fun getProjectByIdOnce(id: Long): Project?
    suspend fun addProject(
        name: String,
        localPath: String,
        safUri: String,
        repoOwner: String,
        repoName: String,
        branch: String,
        uriPermission: String
    ): Long
    suspend fun removeProject(id: Long)
    suspend fun updateProject(project: Project)
    suspend fun getProjectByPath(path: String): Project?
}
