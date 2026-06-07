package com.gitsync.core.di

import android.content.Context
import androidx.room.Room
import com.gitsync.core.util.Constants
import com.gitsync.data.local.AppDatabase
import com.gitsync.data.local.dao.CommitDao
import com.gitsync.data.local.dao.ProjectDao
import com.gitsync.data.local.dao.WorkflowRunDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideCommitDao(database: AppDatabase): CommitDao = database.commitDao()

    @Provides
    fun provideWorkflowRunDao(database: AppDatabase): WorkflowRunDao = database.workflowRunDao()
}
