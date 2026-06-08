package com.gitsync

import com.gitsync.core.util.SafUriHelper
import org.junit.Test

class SafUriHelperTest {

    @Test
    fun `resolveLocalPath handles invalid uri gracefully`() {
        val result = SafUriHelper.resolveLocalPath(
            io.mockk.mockk(relaxed = true),
            android.net.Uri.parse("invalid://uri")
        )
        // Should return null for invalid URIs, not crash
    }

    @Test
    fun `isValidDirectory handles invalid uri gracefully`() {
        SafUriHelper.isValidDirectory(
            io.mockk.mockk(relaxed = true),
            android.net.Uri.parse("invalid://uri")
        )
        // Should not crash
    }

    @Test
    fun `getDirectoryName handles invalid uri gracefully`() {
        SafUriHelper.getDirectoryName(
            io.mockk.mockk(relaxed = true),
            android.net.Uri.parse("invalid://uri")
        )
        // Should not crash
    }
}
