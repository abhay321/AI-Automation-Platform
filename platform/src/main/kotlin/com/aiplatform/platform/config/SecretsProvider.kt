package com.aiplatform.platform.config

import java.io.File

interface SecretsProvider {
    val name: String
    fun getSecret(key: String): String?
}

class EnvironmentSecretsProvider : SecretsProvider {
    override val name = "Environment"
    override fun getSecret(key: String): String? {
        // Look up env variables with support for both snake_case and standard properties format
        val cleanKey = key.uppercase().replace(".", "_").replace("-", "_")
        return System.getenv(cleanKey) ?: System.getenv(key)
    }
}

class FileSecretsProvider(private val secretDir: File) : SecretsProvider {
    override val name = "SecretFile"
    override fun getSecret(key: String): String? {
        if (!secretDir.exists() || !secretDir.isDirectory) return null
        val cleanKey = key.lowercase().replace(".", "_").replace("-", "_")
        val secretFile = File(secretDir, cleanKey)
        return if (secretFile.exists() && secretFile.isFile) {
            secretFile.readText().trim()
        } else {
            null
        }
    }
}

// Stubs for future cloud secret managers
class HashiCorpVaultSecretsProvider : SecretsProvider {
    override val name = "HashiCorpVault"
    override fun getSecret(key: String): String? = null // Future implementation
}

class AwsSecretsManagerProvider : SecretsProvider {
    override val name = "AwsSecretsManager"
    override fun getSecret(key: String): String? = null // Future implementation
}

class GcpSecretManagerProvider : SecretsProvider {
    override val name = "GcpSecretManager"
    override fun getSecret(key: String): String? = null // Future implementation
}

class AzureKeyVaultSecretsProvider : SecretsProvider {
    override val name = "AzureKeyVault"
    override fun getSecret(key: String): String? = null // Future implementation
}
