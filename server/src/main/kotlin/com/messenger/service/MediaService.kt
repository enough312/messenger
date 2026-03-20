package com.messenger.service

import com.messenger.config.AppConfig
import com.messenger.shared.dto.MediaUploadResponse
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

class MediaService(
    private val config: AppConfig,
    private val s3Client: S3Client,
) {
    fun ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(config.minioBucket).build())
        } catch (_: NoSuchBucketException) {
            if (config.s3AutoCreateBucket) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(config.minioBucket).build())
            } else {
                throw ServiceException("Bucket ${config.minioBucket} does not exist", statusCode = 500)
            }
        }
    }

    fun upload(contentType: String, bytes: ByteArray, originalName: String?): MediaUploadResponse {
        val extension = originalName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
        val objectKey = "${UUID.randomUUID()}$extension"
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(config.minioBucket)
                .key(objectKey)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes),
        )
        return MediaUploadResponse(
            url = "${config.publicBaseUrl.trimEnd('/')}/media/$objectKey",
            contentType = contentType,
            size = bytes.size.toLong(),
        )
    }

    fun download(objectKey: String): ResponseBytes<GetObjectResponse> = s3Client.getObjectAsBytes(
        GetObjectRequest.builder()
            .bucket(config.minioBucket)
            .key(objectKey)
            .build(),
    )
}
