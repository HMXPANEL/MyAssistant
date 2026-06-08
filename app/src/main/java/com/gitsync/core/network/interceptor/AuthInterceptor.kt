package com.gitsync.core.network.interceptor

import com.gitsync.core.security.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = secureStorage.githubToken
        val original = chain.request()

        if (token.isBlank()) {
            return chain.proceed(original)
        }

        val request = original.newBuilder()
            .header("Authorization", "token $token")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return chain.proceed(request)
    }
}
