package com.humblecoders.plantmanagement.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class FirebaseStorageService(credentials: GoogleCredentials? = null) {
    
    private val storage: Storage = if (credentials != null) {
        StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service
    } else {
        StorageOptions.getDefaultInstance().service
    }
    
    private val bucketName = "plantmanagement-b1db8.firebasestorage.app"
    
    suspend fun uploadImage(file: File, folder: String = "purchases"): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Generate unique file name with sanitized filename
            val sanitizedFileName = file.name
                .replace(" ", "_")
                .replace(Regex("[^a-zA-Z0-9._-]"), "")
            val fileName = "${folder}/${UUID.randomUUID()}_${sanitizedFileName}"
            
            // Create blob info
            val blobId = BlobId.of(bucketName, fileName)
            val blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(getContentType(file.extension))
                .setCacheControl("public, max-age=31536000")
                .build()
            
            // Upload file
            val blob = storage.create(blobInfo, file.readBytes())
            
            // Make the file publicly accessible
            try {
                blob.createAcl(com.google.cloud.storage.Acl.of(
                    com.google.cloud.storage.Acl.User.ofAllUsers(),
                    com.google.cloud.storage.Acl.Role.READER
                ))
            } catch (e: Exception) {
                println("Warning: Could not set ACL (might already be public): ${e.message}")
            }
            
            // Get proper download URL using Firebase Storage format
            // Use proper URL encoding
            val encodedPath = java.net.URLEncoder.encode(fileName, "UTF-8")
                .replace("+", "%20") // Replace + with proper space encoding
            
            val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/$encodedPath?alt=media"
            
            println("Image uploaded successfully: $downloadUrl")
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            println("Error uploading image: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun deleteImage(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Extract file path from URL
            val filePath = extractFilePathFromUrl(imageUrl)
            if (filePath.isNotBlank()) {
                val blobId = BlobId.of(bucketName, filePath)
                storage.delete(blobId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error deleting image: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun extractFilePathFromUrl(url: String): String {
        return try {
            // Extract path from Firebase Storage URL
            // Format: https://firebasestorage.googleapis.com/v0/b/bucket/o/path%2Ffile?alt=media
            val regex = Regex("""/o/([^?]+)""")
            val match = regex.find(url)
            match?.groupValues?.get(1)?.replace("%2F", "/") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun getContentType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            else -> "application/octet-stream"
        }
    }
}

