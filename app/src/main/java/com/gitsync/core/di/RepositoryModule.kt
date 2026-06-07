package com.gitsync.core.di

import com.gitsync.data.repository.AuthRepositoryImpl
import com.gitsync.data.repository.GitRepositoryImpl
import com.gitsync.data.repository.ProjectRepositoryImpl
import com.gitsync.data.repository.SettingsRepositoryImpl
import com.gitsync.data.repository.WorkflowRepositoryImpl
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.GitRepository
import com.gitsync.domain.repository.ProjectRepository
import com.gitsync.domain.repository.SettingsRepository
import com.gitsync.domain.repository.WorkflowRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindGitRepository(impl: GitRepositoryImpl): GitRepository

    @Binds
    @Singleton
    abstract fun bindWorkflowRepository(impl: WorkflowRepositoryImpl): WorkflowRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
