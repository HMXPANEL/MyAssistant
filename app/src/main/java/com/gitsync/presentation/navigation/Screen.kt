package com.gitsync.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Setup : Screen("setup")
    data object Dashboard : Screen("dashboard")
    data object Projects : Screen("projects")
    data object Settings : Screen("settings")

    data object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: Long) = "project_detail/$projectId"
    }

    data object Workflow : Screen("workflow/{projectId}") {
        fun createRoute(projectId: Long) = "workflow/$projectId"
    }

    data object Artifacts : Screen("artifacts/{projectId}") {
        fun createRoute(projectId: Long) = "artifacts/$projectId"
    }

    data object Commits : Screen("commits/{projectId}") {
        fun createRoute(projectId: Long) = "commits/$projectId"
    }
}
