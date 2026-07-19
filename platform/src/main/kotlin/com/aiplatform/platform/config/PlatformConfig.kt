package com.aiplatform.platform.config

data class PlatformConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val rabbitmq: RabbitMqConfig,
    val minio: MinioConfig,
    val qdrant: QdrantConfig,
    val security: SecurityConfig,
    val metrics: MetricsConfig
)

data class ServerConfig(
    @ConfigProperty(
        description = "Port on which the HTTP server binds and listens",
        defaultValue = "3000",
        isRequired = true,
        validationRules = "Must be between 1 and 65535",
        example = "3000",
        displayName = "Server Port"
    )
    val port: Int = 3000,

    @ConfigProperty(
        description = "Network host interface address to bind the server to",
        defaultValue = "0.0.0.0",
        isRequired = true,
        validationRules = "Valid IP address or hostname",
        example = "127.0.0.1",
        displayName = "Server Bind Host"
    )
    val host: String = "0.0.0.0",

    @ConfigProperty(
        description = "Prefix path for all application web routes",
        defaultValue = "",
        isRequired = false,
        validationRules = "Must start with a slash if not empty",
        example = "/api/v1",
        displayName = "Context Path"
    )
    val contextPath: String = "",

    @ConfigProperty(
        description = "Maximum time limit in milliseconds for handling incoming HTTP requests",
        defaultValue = "30000",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "15000",
        displayName = "Request Timeout"
    )
    val requestTimeoutMs: Long = 30000
)

data class DatabaseConfig(
    @ConfigProperty(
        description = "JDBC database connection URL for PostgreSQL backend storage",
        defaultValue = "",
        isRequired = true,
        validationRules = "Must start with jdbc:postgresql://",
        example = "jdbc:postgresql://localhost:5432/platform_db",
        displayName = "Database URL"
    )
    val url: String,

    @ConfigProperty(
        description = "Fully qualified class name of the database driver",
        defaultValue = "org.postgresql.Driver",
        isRequired = true,
        validationRules = "Must be a valid class path",
        example = "org.postgresql.Driver",
        displayName = "Database Driver"
    )
    val driver: String = "org.postgresql.Driver",

    @ConfigProperty(
        description = "Username used for authentication with the database",
        defaultValue = "",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "db_admin",
        displayName = "Database Username"
    )
    val username: String,

    @ConfigProperty(
        description = "Password used for authentication with the database",
        defaultValue = "",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "supersecret_db_pass",
        displayName = "Database Password"
    )
    val password: String,

    @ConfigProperty(
        description = "Maximum size of the HikariCP database connection pool",
        defaultValue = "10",
        isRequired = true,
        validationRules = "Must be between 1 and 200",
        example = "20",
        displayName = "Max Pool Size"
    )
    val maximumPoolSize: Int = 10,

    @ConfigProperty(
        description = "Minimum number of idle connections that HikariCP tries to maintain in the pool",
        defaultValue = "2",
        isRequired = true,
        validationRules = "Must be less than or equal to maximumPoolSize",
        example = "5",
        displayName = "Minimum Idle Connections"
    )
    val minimumIdle: Int = 2,

    @ConfigProperty(
        description = "Idle timeout for database connections in milliseconds",
        defaultValue = "600000",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "300000",
        displayName = "Connection Idle Timeout"
    )
    val idleTimeoutMs: Long = 600000,

    @ConfigProperty(
        description = "Maximum connection timeout in milliseconds for database client requests",
        defaultValue = "30000",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "10000",
        displayName = "Connection Timeout"
    )
    val connectionTimeoutMs: Long = 30000
)

data class RedisConfig(
    @ConfigProperty(
        description = "Redis host address for caching and distributed locks",
        defaultValue = "localhost",
        isRequired = true,
        validationRules = "Valid hostname or IP",
        example = "redis-cluster.internal",
        displayName = "Redis Host"
    )
    val host: String = "localhost",

    @ConfigProperty(
        description = "Redis server listener port",
        defaultValue = "6379",
        isRequired = true,
        validationRules = "Must be between 1 and 65535",
        example = "6379",
        displayName = "Redis Port"
    )
    val port: Int = 6379,

    @ConfigProperty(
        description = "Optional authentication password for Redis",
        defaultValue = "",
        isRequired = false,
        validationRules = "None",
        example = "redis_pass_123",
        displayName = "Redis Password"
    )
    val password: String? = null,

    @ConfigProperty(
        description = "Timeout limit in milliseconds for Redis command executions",
        defaultValue = "3000",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "5000",
        displayName = "Redis Timeout"
    )
    val timeoutMs: Int = 3000,

    @ConfigProperty(
        description = "Target database index inside the Redis instance",
        defaultValue = "0",
        isRequired = true,
        validationRules = "Must be greater than or equal to 0",
        example = "1",
        displayName = "Redis Database Index"
    )
    val database: Int = 0
)

data class RabbitMqConfig(
    @ConfigProperty(
        description = "RabbitMQ broker host address for event messaging integration",
        defaultValue = "localhost",
        isRequired = true,
        validationRules = "Valid hostname or IP",
        example = "rabbitmq-server.internal",
        displayName = "RabbitMQ Host"
    )
    val host: String = "localhost",

    @ConfigProperty(
        description = "RabbitMQ broker server listener port",
        defaultValue = "5672",
        isRequired = true,
        validationRules = "Must be between 1 and 65535",
        example = "5672",
        displayName = "RabbitMQ Port"
    )
    val port: Int = 5672,

    @ConfigProperty(
        description = "Username for RabbitMQ broker authentication",
        defaultValue = "guest",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "user_auth",
        displayName = "RabbitMQ Username"
    )
    val username: String = "guest",

    @ConfigProperty(
        description = "Password for RabbitMQ broker authentication",
        defaultValue = "guest",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "user_pass",
        displayName = "RabbitMQ Password"
    )
    val password: String = "guest",

    @ConfigProperty(
        description = "Target virtual host scope inside RabbitMQ",
        defaultValue = "/",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "/dev-vhost",
        displayName = "RabbitMQ Virtual Host"
    )
    val virtualHost: String = "/",

    @ConfigProperty(
        description = "Connection timeout in milliseconds for RabbitMQ socket triggers",
        defaultValue = "30000",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "15000",
        displayName = "RabbitMQ Connection Timeout"
    )
    val connectionTimeoutMs: Int = 30000
)

data class MinioConfig(
    @ConfigProperty(
        description = "S3 endpoint URL for MinIO object storage services",
        defaultValue = "http://localhost:9000",
        isRequired = true,
        validationRules = "Must be a valid HTTP/HTTPS URL",
        example = "http://minio.platform:9000",
        displayName = "MinIO Endpoint"
    )
    val endpoint: String = "http://localhost:9000",

    @ConfigProperty(
        description = "Access key credential for S3 / MinIO storage authentication",
        defaultValue = "",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "minio_root_access",
        displayName = "MinIO Access Key"
    )
    val accessKey: String,

    @ConfigProperty(
        description = "Secret key credential for S3 / MinIO storage authentication",
        defaultValue = "",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "minio_secret_pass",
        displayName = "MinIO Secret Key"
    )
    val secretKey: String,

    @ConfigProperty(
        description = "Default bucket name to organize assets and attachments inside",
        defaultValue = "platform-assets",
        isRequired = true,
        validationRules = "Must follow S3 naming standards",
        example = "production-assets",
        displayName = "MinIO Bucket Name"
    )
    val bucketName: String = "platform-assets",

    @ConfigProperty(
        description = "Instructs the client to construct the bucket if it does not exist at startup",
        defaultValue = "true",
        isRequired = true,
        validationRules = "Boolean flag",
        example = "false",
        displayName = "Auto-Create Bucket"
    )
    val createBucketIfNotExist: Boolean = true
)

data class QdrantConfig(
    @ConfigProperty(
        description = "Host address for Qdrant vector database used in RAG contexts",
        defaultValue = "localhost",
        isRequired = true,
        validationRules = "Valid hostname or IP",
        example = "qdrant-node.internal",
        displayName = "Qdrant Host"
    )
    val host: String = "localhost",

    @ConfigProperty(
        description = "REST api port for Qdrant connections",
        defaultValue = "6333",
        isRequired = true,
        validationRules = "Must be between 1 and 65535",
        example = "6333",
        displayName = "Qdrant REST Port"
    )
    val port: Int = 6333,

    @ConfigProperty(
        description = "High speed gRPC connector port for Qdrant vector storage",
        defaultValue = "6334",
        isRequired = true,
        validationRules = "Must be between 1 and 65535",
        example = "6334",
        displayName = "Qdrant gRPC Port"
    )
    val grpcPort: Int = 6334,

    @ConfigProperty(
        description = "Optional API key for secure Qdrant access",
        defaultValue = "",
        isRequired = false,
        validationRules = "None",
        example = "qdrant_api_secret_key",
        displayName = "Qdrant API Key"
    )
    val apiKey: String? = null,

    @ConfigProperty(
        description = "Enable encrypted HTTPS/gRPCS secure connection with the database",
        defaultValue = "false",
        isRequired = true,
        validationRules = "Boolean flag",
        example = "true",
        displayName = "Use Secure Connection"
    )
    val useSecureConnection: Boolean = false
)

data class SecurityConfig(
    @ConfigProperty(
        description = "Master symmetric key for local field-level credential encryption at-rest (AES-256)",
        defaultValue = "",
        isRequired = true,
        validationRules = "Must be at least 16 characters in length",
        example = "encryption_pass_must_be_long_32",
        displayName = "Encryption Key"
    )
    val encryptionKey: String,

    @ConfigProperty(
        description = "Signature secret key for JWT session tokens and user authorization contexts",
        defaultValue = "",
        isRequired = true,
        validationRules = "Cannot be blank",
        example = "jwt_signing_token_key",
        displayName = "JWT Signature Secret"
    )
    val tokenSecret: String,

    @ConfigProperty(
        description = "Expiration threshold in minutes for newly constructed JWT tokens",
        defaultValue = "1440",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "60",
        displayName = "Token Expiration (Minutes)"
    )
    val tokenExpirationMinutes: Long = 1440
)

data class MetricsConfig(
    @ConfigProperty(
        description = "Global toggle flag to enable or disable platform performance monitoring",
        defaultValue = "true",
        isRequired = true,
        validationRules = "Boolean flag",
        example = "false",
        displayName = "Metrics Enabled"
    )
    val enabled: Boolean = true,

    @ConfigProperty(
        description = "HTTP endpoint path where Prometheus can scrape performance metrics",
        defaultValue = "/metrics",
        isRequired = true,
        validationRules = "Must start with a slash",
        example = "/prometheus/metrics",
        displayName = "Prometheus Endpoint"
    )
    val prometheusEndpoint: String = "/metrics",

    @ConfigProperty(
        description = "Granular interval in seconds for Micrometer registry metrics processing",
        defaultValue = "60",
        isRequired = true,
        validationRules = "Must be greater than 0",
        example = "15",
        displayName = "Metrics Interval Step"
    )
    val stepSeconds: Long = 60
)
