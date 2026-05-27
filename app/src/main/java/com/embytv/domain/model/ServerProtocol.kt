package com.embytv.domain.model

enum class ServerProtocol(val scheme: String, val defaultPort: Int) {
    Https("https", 443),
    Http("http", 8096);

    companion object {
        fun from(value: String): ServerProtocol? =
            entries.firstOrNull { protocol ->
                protocol.scheme.equals(value.trim(), ignoreCase = true) ||
                    protocol.name.equals(value.trim(), ignoreCase = true)
            }
    }
}
