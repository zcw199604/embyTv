package com.embytv.ui.player

object PlayerDetailProviderIdsLabelResolver {
    fun resolve(providerIds: Map<String, String>): String? =
        listOfNotNull(
            providerIds.providerValue("Imdb")?.let { "IMDb $it" },
            providerIds.providerValue("Douban")?.let { "Douban $it" },
        ).joinToString(" · ").takeIf { it.isNotBlank() }
}

private fun Map<String, String>.providerValue(provider: String): String? =
    entries.firstOrNull { it.key.equals(provider, ignoreCase = true) }
        ?.value
        ?.trim()
        ?.takeIf { it.isNotBlank() }
