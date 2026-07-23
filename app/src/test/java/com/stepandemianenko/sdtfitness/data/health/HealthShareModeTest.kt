package com.stepandemianenko.sdtfitness.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthShareModeTest {

    @Test
    fun `fromStorage parses stored names and falls back to OFF`() {
        assertEquals(HealthShareMode.AUTO, HealthShareMode.fromStorage("AUTO"))
        assertEquals(HealthShareMode.MANUAL, HealthShareMode.fromStorage("MANUAL"))
        assertEquals(HealthShareMode.OFF, HealthShareMode.fromStorage("OFF"))
        assertEquals(HealthShareMode.OFF, HealthShareMode.fromStorage(null))
        assertEquals(HealthShareMode.OFF, HealthShareMode.fromStorage(""))
        assertEquals(HealthShareMode.OFF, HealthShareMode.fromStorage("garbage"))
    }

    @Test
    fun `push requires a non-OFF mode plus connection plus write permissions`() {
        assertTrue(canPushToHealthConnect(HealthShareMode.AUTO, connected = true, hasWritePermissions = true))
        assertTrue(canPushToHealthConnect(HealthShareMode.MANUAL, connected = true, hasWritePermissions = true))

        assertFalse(canPushToHealthConnect(HealthShareMode.OFF, connected = true, hasWritePermissions = true))
        assertFalse(canPushToHealthConnect(HealthShareMode.AUTO, connected = false, hasWritePermissions = true))
        assertFalse(canPushToHealthConnect(HealthShareMode.AUTO, connected = true, hasWritePermissions = false))
        assertFalse(canPushToHealthConnect(HealthShareMode.MANUAL, connected = false, hasWritePermissions = false))
    }
}
