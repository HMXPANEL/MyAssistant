package com.gitsync

import android.net.Uri
import com.gitsync.core.util.SafUriHelper
import io.mockk.mockk
import org.junit.Test

class SafUriHelperTest {

    @Test
    fun `resolveLocalPath handles invalid uri gracefully`() {
        val result = SafUriHelper.resolveLocalPath(
            mockk(relaxed = true),
            mockk<Uri>(relaxed = true)
        )
        // Should return null for invalid URIs, not crash
    }

    @Test
    fun `isValidDirectory handles invalid uri gracefully`() {
        SafUriHelper.isValidDirectory(
            mockk(relaxed = true),
            mockk<Uri>(relaxed = true)
        )
        // Should not crash
    }

    @Test
    fun `getDirectoryName handles invalid uri gracefully`() {
        SafUriHelper.getDirectoryName(
            mockk(relaxed = true),
            mockk<Uri>(relaxed = true)
        )
        // Should not crash
    }
}
