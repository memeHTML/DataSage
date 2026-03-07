package com.retailiq.datasage.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavModelsTest {
    @Test
    fun owner_hasFiveTabs() {
        val tabs = tabsForRole(UserRole.OWNER)
        assertEquals(5, tabs.size)
        assertEquals(listOf("home", "sales", "inventory", "analytics", "more"), tabs.map { it.route })
    }

    @Test
    fun staff_hasFourTabs() {
        val tabs = tabsForRole(UserRole.STAFF)
        assertEquals(4, tabs.size)
        assertEquals(listOf("home", "sales", "inventory", "more"), tabs.map { it.route })
    }
}
