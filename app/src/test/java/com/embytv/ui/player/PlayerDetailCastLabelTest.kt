package com.embytv.ui.player

import com.embytv.domain.model.EmbyPersonSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerDetailCastLabelTest {
    @Test
    fun castLabelUsesActorsOnlyAndKeepsRoles() {
        val label = PlayerDetailCastLabelResolver.resolve(
            listOf(
                EmbyPersonSummary(id = "director-1", name = "导演甲", role = null, type = "Director"),
                EmbyPersonSummary(id = "actor-1", name = "演员甲", role = "主角", type = "Actor"),
                EmbyPersonSummary(id = "writer-1", name = "编剧甲", role = null, type = "Writer"),
                EmbyPersonSummary(id = "actor-2", name = "演员乙", role = null, type = "Actor"),
            ),
            roleLabel = { name, role -> "$name 饰 $role" },
        )

        assertEquals("演员甲 饰 主角 / 演员乙", label)
    }

    @Test
    fun castLabelUsesInjectedEnglishRoleFormat() {
        val label = PlayerDetailCastLabelResolver.resolve(
            listOf(
                EmbyPersonSummary(id = "actor-1", name = "Actor A", role = "Lead", type = "Actor"),
            ),
            roleLabel = { name, role -> "$name as $role" },
        )

        assertEquals("Actor A as Lead", label)
    }

    @Test
    fun castLabelLimitsOverlaySummaryToFourActors() {
        val label = PlayerDetailCastLabelResolver.resolve(
            (1..5).map { index ->
                EmbyPersonSummary(id = "actor-$index", name = "演员$index", role = null, type = "Actor")
            },
        )

        assertEquals("演员1 / 演员2 / 演员3 / 演员4", label)
    }

    @Test
    fun castLabelReturnsNullWhenNoActorExists() {
        val label = PlayerDetailCastLabelResolver.resolve(
            listOf(
                EmbyPersonSummary(id = "director-1", name = "导演甲", role = null, type = "Director"),
            ),
        )

        assertNull(label)
    }
}
