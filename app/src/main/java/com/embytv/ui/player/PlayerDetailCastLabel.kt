package com.embytv.ui.player

import com.embytv.domain.model.EmbyPersonSummary

object PlayerDetailCastLabelResolver {
    fun resolve(
        people: List<EmbyPersonSummary>,
        limit: Int = 4,
        roleLabel: (name: String, role: String) -> String = { name, role -> "$name ($role)" },
    ): String? =
        people
            .asSequence()
            .filter { it.type.equals("Actor", ignoreCase = true) }
            .map { person ->
                person.role
                    ?.takeIf { it.isNotBlank() }
                    ?.let { role -> roleLabel(person.name, role) }
                    ?: person.name
            }
            .take(limit.coerceAtLeast(0))
            .toList()
            .joinToString(" / ")
            .takeIf { it.isNotBlank() }
}
