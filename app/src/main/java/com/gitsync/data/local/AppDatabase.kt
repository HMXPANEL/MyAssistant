package com.gitsync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gitsync.data.local.dao.CommitDao
import com.gitsync.data.local.dao.ProjectDao
import com.gitsync.data.local.dao.WorkflowRunDao
import com.gitsync.data.local.entity.CommitEntity
import com.gitsync.data.local.entity.ProjectEntity
import com.gitsync.data.local.entity.WorkflowRunEntity

@Database(
    entities = [
        ProjectEntity::class,
        CommitEntity::class,
        WorkflowRunEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun commitDao(): CommitDao
    abstract fun workflowRunDao(): WorkflowRunDao
}
