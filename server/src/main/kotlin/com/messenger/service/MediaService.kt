package com.messenger.service

import com.messenger.config.AppConfig
import com.messenger.shared.dto.MediaUploadResponse
import java.io.File
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

data class MediaBinary(
    val bytes: ByteArray,
    val contentType: String,
)

class MediaService(
    private val config: AppConfig,
    private val s3Client: S3Client?,
) {
    fun ensureBucket() {
        if (config.mediaStorageMode.equals("local", ignoreCase = true)) {
            File(config.mediaLocalDir).mkdirs()
            return
        }
        val client = s3Client ?: throw ServiceException("S3 client is not configured", statusCode = 500)
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(config.minioBucket).build())
        } catch (_: NoSuchBucketException) {
            if (config.s3AutoCreateBucket) {
                client.createBucket(CreateBucketRequest.builder().bucket(config.minioBucket).build())
            } else {
                throw ServiceException("Bucket ${config.minioBucket} does not exist", statusCode = 500)
            }
        }
    }

    fun upload(contentType: String, bytes: ByteArray, originalName: String?): MediaUploadResponse {
        val extension = originalName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
        val objectKey = "${UUID.randomUUID()}$extension"
        if (config.mediaStorageMode.equals("local", ignoreCase = true)) {
            val mediaDir = File(config.mediaLocalDir).apply { mkdirs() }
            File(mediaDir, objectKey).writeBytes(bytes)
            File(mediaDir, "$objectKey.contentType").writeText(contentType)
        } else {
            val client = s3Client ?: throw ServiceException("S3 client is not configured", statusCode = 500)
            client.putObject(
                PutObjectRequest.builder()
                    .bucket(config.minioBucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build(),
                RequestBody.fromBytes(bytes),
            )
        }
        return MediaUploadResponse(
            url = "${config.publicBaseUrl.trimEnd('/')}/media/$objectKey",
            contentType = contentType,
            size = bytes.size.toLong(),
        )
    }

    fun download(objectKey: String): MediaBinary {
        if (config.mediaStorageMode.equals("local", ignoreCase = true)) {
            val mediaDir = File(config.mediaLocalDir)
            val file = File(mediaDir, objectKey)
            if (!file.exists()) throw ServiceException("Media not found", statusCode = 404)
            val contentType = File(mediaDir, "$objectKey.contentType")
                .takeIf { it.exists() }
                ?.readText()
                ?.takeIf { it.isNotBlank() }
                ?: "application/octet-stream"
            return MediaBinary(file.readBytes(), contentType)
        }

        val client = s3Client ?: throw ServiceException("S3 client is not configured", statusCode = 500)
        val response = client.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(config.minioBucket)
                .key(objectKey)
                .build(),
        )
        return MediaBinary(
            bytes = response.asByteArray(),
            contentType = response.response().contentType()?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
        )
    }
}
