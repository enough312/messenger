package com.messenger.config

data class AppConfig(
    val appEnv: String,
    val host: String,
    val port: Int,
    val publicBaseUrl: String,
    val dbUrl: String,
    val dbUser: String?,
    val dbPassword: String?,
    val dbPoolSize: Int,
    val dbSslMode: String?,
    val redisUrl: String?,
    val redisHost: String,
    val redisPort: Int,
    val redisPassword: String?,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtRealm: String,
    val jwtSecret: String,
    val jwtAccessTtlMinutes: Long,
    val jwtRefreshTtlDays: Long,
    val mediaStorageMode: String,
    val mediaLocalDir: String,
    val minioEndpoint: String,
    val minioRegion: String,
    val minioAccessKey: String,
    val minioSecretKey: String,
    val minioBucket: String,
    val s3PathStyle: Boolean,
    val s3AutoCreateBucket: Boolean,
    val smtpHost: String?,
    val smtpPort: Int,
    val smtpUser: String?,
    val smtpPassword: String?,
    val smtpFrom: String?,
    val firebaseProjectId: String?,
    val firebaseCredentialsPath: String?,
    val totpIssuer: String,
    val turnUrls: String?,
    val turnUsername: String?,
    val turnCredential: String?,
    val stunUrl: String?,
) {
    companion object {
        fun fromEnv(): AppConfig {
            val appEnv = env("APP_ENV", "development")
            val rawDatabaseUrl = envOptional("DB_URL")
                ?: envOptional("JDBC_DATABASE_URL")
                ?: envOptional("DATABASE_URL")
                ?: "jdbc:postgresql://localhost:5432/messenger"
            val (dbUserFromUrl, dbPasswordFromUrl) = parseUrlCredentials(rawDatabaseUrl.removePrefix("jdbc:"))
            val dbUrl = rawDatabaseUrl.toJdbcPostgresUrl()
            return AppConfig(
                appEnv = appEnv,
                host = env("SERVER_HOST", "0.0.0.0"),
                port = envOptional("PORT")?.toInt()
                    ?: env("SERVER_PORT", "8080").toInt(),
                publicBaseUrl = envOptional("PUBLIC_BASE_URL")
                    ?: envOptional("RENDER_EXTERNAL_URL")
                    ?: "http://localhost:8080",
                dbUrl = dbUrl,
                dbUser = envOptional("DB_USER")
                    ?: envOptional("PGUSER")
                    ?: dbUserFromUrl,
                dbPassword = envOptional("DB_PASSWORD")
                    ?: envOptional("PGPASSWORD")
                    ?: dbPasswordFromUrl,
                dbPoolSize = env("DB_POOL_SIZE", "10").toInt(),
                dbSslMode = envOptional("DB_SSL_MODE"),
                redisUrl = envOptional("REDIS_URL")
                    ?: envOptional("KV_URL")
                    ?: envOptional("UPSTASH_REDIS_URL"),
                redisHost = env("REDIS_HOST", "localhost"),
                redisPort = env("REDIS_PORT", "6379").toInt(),
                redisPassword = envOptional("REDIS_PASSWORD"),
                jwtIssuer = env("JWT_ISSUER", "messenger"),
                jwtAudience = env("JWT_AUDIENCE", "messenger-clients"),
                jwtRealm = env("JWT_REALM", "messenger"),
                jwtSecret = env("JWT_SECRET", "change-me"),
                jwtAccessTtlMinutes = env("JWT_ACCESS_TTL_MINUTES", "15").toLong(),
                jwtRefreshTtlDays = env("JWT_REFRESH_TTL_DAYS", "30").toLong(),
                mediaStorageMode = env("MEDIA_STORAGE_MODE", if (appEnv == "production") "local" else "s3"),
                mediaLocalDir = env("MEDIA_LOCAL_DIR", "./data/media"),
                minioEndpoint = env("MINIO_ENDPOINT", "http://localhost:9000"),
                minioRegion = env("MINIO_REGION", "us-east-1"),
                minioAccessKey = env("MINIO_ACCESS_KEY", "minioadmin"),
                minioSecretKey = env("MINIO_SECRET_KEY", "minioadmin"),
                minioBucket = env("MINIO_BUCKET", "messenger-media"),
                s3PathStyle = envBoolean("S3_PATH_STYLE", true),
                s3AutoCreateBucket = envBoolean("S3_AUTO_CREATE_BUCKET", appEnv == "development"),
                smtpHost = envOptional("SMTP_HOST"),
                smtpPort = env("SMTP_PORT", "587").toInt(),
                smtpUser = envOptional("SMTP_USER"),
                smtpPassword = envOptional("SMTP_PASSWORD"),
                smtpFrom = envOptional("SMTP_FROM"),
                firebaseProjectId = envOptional("FIREBASE_PROJECT_ID"),
                firebaseCredentialsPath = envOptional("FIREBASE_CREDENTIALS_PATH"),
                totpIssuer = env("TOTP_ISSUER", "Messenger"),
                turnUrls = envOptional("TURN_URLS"),
                turnUsername = envOptional("TURN_USERNAME"),
                turnCredential = envOptional("TURN_CREDENTIAL"),
                stunUrl = envOptional("STUN_URL"),
            )
        }

        private fun env(name: String, default: String): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

        private fun envOptional(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

        private fun envBoolean(name: String, default: Boolean): Boolean {
            val value = envOptional(name) ?: return default
            return value.equals("true", ignoreCase = true) || value == "1" || value.equals("yes", ignoreCase = true)
        }

        private fun String.toJdbcPostgresUrl(): String {
            if (startsWith("jdbc:postgresql://")) return this
            if (startsWith("postgres://") || startsWith("postgresql://")) {
                val uri = java.net.URI(this)
                val host = uri.host ?: return "jdbc:$this"
                val port = if (uri.port != -1) ":${uri.port}" else ""
                val path = uri.rawPath ?: ""
                val query = uri.rawQuery?.let { "?$it" } ?: ""
                return "jdbc:postgresql://$host$port$path$query"
            }
            return this
        }

        private fun parseUrlCredentials(url: String): Pair<String?, String?> {
            val authority = runCatching { java.net.URI(url).userInfo }.getOrNull() ?: return null to null
            val username = authority.substringBefore(':').takeIf { it.isNotBlank() }
            val password = authority.substringAfter(':', "").takeIf { it.isNotBlank() }
            return username to password
        }
    }
}
