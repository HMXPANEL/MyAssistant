package com.gitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gitsync.data.local.entity.WorkflowRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowRunDao {

    @Query("SELECT * FROM workflow_runs WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getWorkflowRunsForProject(projectId: Long): Flow<List<WorkflowRunEntity>>

    @Query("SELECT * FROM workflow_runs WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentRuns(projectId: Long, limit: Int = 10): List<WorkflowRunEntity>

    @Query("SELECT * FROM workflow_runs WHERE runId = :runId")
    suspend fun getRunById(runId: Long): WorkflowRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: WorkflowRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(runs: List<WorkflowRunEntity>)

    @Query("DELETE FROM workflow_runs WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)

    @Query("SELECT * FROM workflow_runs WHERE projectId = :projectId AND status = 'completed' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCompletedRun(projectId: Long): WorkflowRunEntity?
}
