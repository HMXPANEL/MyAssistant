package com.gitsync.core.network

import com.gitsync.data.remote.dto.ArtifactDto
import com.gitsync.data.remote.dto.CreateRepoRequestDto
import com.gitsync.data.remote.dto.FileContentRequestDto
import com.gitsync.data.remote.dto.FileContentResponseDto
import com.gitsync.data.remote.dto.GitHubUserDto
import com.gitsync.data.remote.dto.RepoDto
import com.gitsync.data.remote.dto.WorkflowRunDto
import com.gitsync.data.remote.dto.WorkflowRunsResponse
import com.gitsync.data.remote.dto.ArtifactsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {

    @GET("user")
    suspend fun getUser(): GitHubUserDto

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): RepoDto

    @GET("repos/{owner}/{repo}/branches/{branch}")
    suspend fun getBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): RepoDto.Branch

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): WorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{runId}")
    suspend fun getWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): WorkflowRunDto

    @GET("repos/{owner}/{repo}/actions/artifacts")
    suspend fun getArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): ArtifactsResponse

    @GET("repos/{owner}/{repo}/actions/artifacts/{artifactId}/zip")
    suspend fun downloadArtifact(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifactId") artifactId: Long
    ): okhttp3.ResponseBody

    @POST("user/repos")
    @Headers("Accept: application/vnd.github+json")
    suspend fun createRepository(
        @Body request: CreateRepoRequestDto
    ): RepoDto

    @GET("repos/{owner}/{repo}")
    suspend fun checkRepositoryExists(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<RepoDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): FileContentResponseDto

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: FileContentRequestDto
    ): FileContentResponseDto

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: com.gitsync.data.remote.dto.CreateBlobRequestDto
    ): com.gitsync.data.remote.dto.CreateBlobResponseDto

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: com.gitsync.data.remote.dto.CreateTreeRequestDto
    ): com.gitsync.data.remote.dto.CreateTreeResponseDto

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: com.gitsync.data.remote.dto.CreateCommitRequestDto
    ): com.gitsync.data.remote.dto.CreateCommitResponseDto

    @GET("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun getRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): com.gitsync.data.remote.dto.RefResponseDto

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body request: com.gitsync.data.remote.dto.UpdateRefRequestDto
    ): com.gitsync.data.remote.dto.RefResponseDto

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: com.gitsync.data.remote.dto.CreateRefRequestDto
    ): com.gitsync.data.remote.dto.RefResponseDto
}
