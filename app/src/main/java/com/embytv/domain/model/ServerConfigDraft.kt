package com.embytv.domain.model

data class ServerConfigDraft(
    val protocol: ServerProtocol = ServerProtocol.Https,
    val host: String = "",
    val port: String = ServerProtocol.Https.defaultPort.toString(),
    val path: String = "",
    val username: String = "",
    val password: String = "",
) {
    fun withProtocol(nextProtocol: ServerProtocol): ServerConfigDraft {
        val currentPort = port.trim()
        val shouldUseDefault = currentPort.isBlank() || currentPort == protocol.defaultPort.toString()
        return copy(
            protocol = nextProtocol,
            port = if (shouldUseDefault) nextProtocol.defaultPort.toString() else port,
        )
    }

    fun validate(): Result<Unit> = runCatching {
        require(host.trim().isNotEmpty()) { "请填写服务器地址" }
        require(!host.contains("://")) { "服务器地址不需要包含协议" }
        require(username.trim().isNotEmpty()) { "请填写 Emby 用户名" }
        val numericPort = port.trim().toIntOrNull()
        require(numericPort != null && numericPort in 1..65535) { "端口必须是 1-65535" }
    }

    fun toServerConfig(deviceId: String): ServerConfig {
        validate().getOrThrow()
        val normalizedHost = host.trim().trimEnd('/')
        val normalizedPort = port.trim().toInt()
        val normalizedPath = path.trim().trim('/')
        val baseUrl = buildString {
            append(protocol.scheme)
            append("://")
            append(normalizedHost)
            append(":")
            append(normalizedPort)
            append("/")
            if (normalizedPath.isNotEmpty()) {
                append(normalizedPath)
                append("/")
            }
        }
        return ServerConfig(
            baseUrl = baseUrl,
            username = username.trim(),
            password = password,
            deviceId = deviceId,
        )
    }
}
