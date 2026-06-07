package com.gitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gitsync.data.local.entity.CommitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitDao {

    @Query("SELECT * FROM commits WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getCommitsForProject(projectId: Long): Flow<List<CommitEntity>>

    @Query("SELECT * FROM commits WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCommits(projectId: Long, limit: Int = 10): List<CommitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commit: CommitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commits: List<CommitEntity>)

    @Query("DELETE FROM commits WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)

    @Query("SELECT COUNT(*) FROM commits WHERE projectId = :projectId")
    suspend fun getCommitCount(projectId: Long): Int
}
