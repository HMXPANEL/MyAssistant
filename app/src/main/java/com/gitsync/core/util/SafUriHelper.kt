package com.gitsync.core.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

object SafUriHelper {

    fun resolveLocalPath(context: Context, uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val parts = docId.split(":")
            val volumeId = parts[0]
            val relative = parts.getOrElse(1) { "" }

            when {
                volumeId == "primary" -> {
                    "${Environment.getExternalStorageDirectory()}/$relative"
                }
                else -> {
                    resolveSecondaryStoragePath(context, volumeId, relative)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isValidDirectory(context: Context, uri: Uri): Boolean {
        return try {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile != null && docFile.exists() && docFile.isDirectory
        } catch (_: Exception) {
            false
        }
    }

    fun getDirectoryName(context: Context, uri: Uri): String {
        return try {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile?.name ?: "Untitled"
        } catch (_: Exception) {
            "Untitled"
        }
    }

    fun takePersistablePermissions(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Permission not available
        }
    }

    fun releasePersistablePermissions(context: Context, uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Ignore
        }
    }

    private fun resolveSecondaryStoragePath(
        context: Context,
        volumeId: String,
        relative: String
    ): String? {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val volume = storageManager?.storageVolumes?.firstOrNull {
            it.uuid == volumeId || it.getMediaStoreVolumeName() == volumeId
        }
        if (volume != null) {
            try {
                val pathMethod = StorageVolume::class.java.getMethod("getPath")
                val mountPoint = pathMethod.invoke(volume) as? String
                if (mountPoint != null) {
                    return "$mountPoint/$relative"
                }
            } catch (_: Exception) {
                // Fall through
            }
        }
        return null
    }

    private fun StorageVolume.getMediaStoreVolumeName(): String? {
        return try {
            val method = StorageVolume::class.java.getMethod("getMediaStoreVolumeName")
            method.invoke(this) as? String
        } catch (_: Exception) {
            null
        }
    }
}
