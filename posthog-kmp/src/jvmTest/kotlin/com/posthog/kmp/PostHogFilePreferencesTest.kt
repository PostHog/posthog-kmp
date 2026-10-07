package com.posthog.kmp

import com.posthog.internal.PostHogPreferences.Companion.ANONYMOUS_ID
import com.posthog.internal.PostHogPreferences.Companion.GROUPS
import java.io.File
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostHogFilePreferencesTest {

    private val config = com.posthog.PostHogConfig("test-key")

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun tempFile(): File = File(tempFolder.newFolder(), "preferences.json")

    @Test
    fun returnsDefaultWhenMissing() {
        val prefs = PostHogFilePreferences(tempFile(), config)
        assertNull(prefs.getValue("missing"))
        assertEquals("fallback", prefs.getValue("missing", "fallback"))
    }

    @Test
    fun roundTripsValueTypesAcrossInstances() {
        val file = tempFile()
        val prefs = PostHogFilePreferences(file, config)
        prefs.setValue(ANONYMOUS_ID, "anon-123")
        prefs.setValue("bool", true)
        prefs.setValue("int", 42)
        prefs.setValue("double", 1.5)
        prefs.setValue(GROUPS, mapOf("company" to "posthog"))
        prefs.setValue("list", listOf("a", "b"))
        prefs.awaitPendingWrites()

        val reloaded = PostHogFilePreferences(file, config)
        assertEquals("anon-123", reloaded.getValue(ANONYMOUS_ID))
        assertEquals(true, reloaded.getValue("bool"))
        assertEquals(42, reloaded.getValue("int"))
        assertEquals(1.5, reloaded.getValue("double"))
        assertEquals(mapOf("company" to "posthog"), reloaded.getValue(GROUPS))
        assertEquals(listOf("a", "b"), reloaded.getValue("list"))
    }

    @Test
    fun removeDeletesKeyPersistently() {
        val file = tempFile()
        val prefs = PostHogFilePreferences(file, config)
        prefs.setValue("key", "value")
        prefs.awaitPendingWrites()
        assertEquals("value", PostHogFilePreferences(file, config).getValue("key"))
        prefs.remove("key")
        prefs.awaitPendingWrites()

        assertNull(prefs.getValue("key"))
        assertNull(PostHogFilePreferences(file, config).getValue("key"))
    }

    @Test
    fun clearKeepsExceptedKeys() {
        val file = tempFile()
        val prefs = PostHogFilePreferences(file, config)
        prefs.setValue(ANONYMOUS_ID, "anon-123")
        prefs.setValue("other", "value")
        prefs.awaitPendingWrites()
        assertEquals("value", PostHogFilePreferences(file, config).getValue("other"))

        prefs.clear(except = listOf(ANONYMOUS_ID))
        prefs.awaitPendingWrites()

        assertEquals("anon-123", prefs.getValue(ANONYMOUS_ID))
        assertNull(prefs.getValue("other"))

        val reloaded = PostHogFilePreferences(file, config)
        assertEquals("anon-123", reloaded.getValue(ANONYMOUS_ID))
        assertNull(reloaded.getValue("other"))
    }

    @Test
    fun getAllFiltersInternalKeys() {
        val prefs = PostHogFilePreferences(tempFile(), config)
        prefs.setValue(ANONYMOUS_ID, "anon-123")
        prefs.setValue("custom", "value")
        prefs.awaitPendingWrites()

        val all = prefs.getAll()
        assertFalse(all.containsKey(ANONYMOUS_ID))
        assertEquals("value", all["custom"])
    }

    @Test
    fun clearWithoutExceptionsRemovesPersistedValues() {
        val file = tempFile()
        val prefs = PostHogFilePreferences(file, config)
        prefs.setValue(ANONYMOUS_ID, "anon")
        prefs.setValue("custom", "value")
        prefs.awaitPendingWrites()
        prefs.clear()
        prefs.awaitPendingWrites()

        val reloaded = PostHogFilePreferences(file, config)
        assertNull(reloaded.getValue(ANONYMOUS_ID))
        assertEquals(emptyMap(), reloaded.getAll())
    }

    @Test
    fun nonObjectJsonFallsBackToEmptyPreferences() {
        for (json in listOf("[]", "null", "42")) {
            val file = tempFile().apply { writeText(json) }
            val prefs = PostHogFilePreferences(file, config)
            assertEquals("fallback", prefs.getValue("key", "fallback"), json)
            prefs.setValue("key", "recovered")
            prefs.awaitPendingWrites()
            assertEquals("recovered", PostHogFilePreferences(file, config).getValue("key"), json)
        }
    }

    @Test
    fun writeFailurePreservesInMemoryStateAndLogsWarning() {
        val parent = tempFolder.newFile()
        val warnings = mutableListOf<String>()
        val failingConfig = com.posthog.PostHogConfig("key").apply {
            logger = object : com.posthog.internal.PostHogLogger {
                override fun log(message: String) {
                    warnings.add(message)
                }
                override fun isEnabled(): Boolean = true
            }
        }
        val prefs = PostHogFilePreferences(File(parent, "preferences.json"), failingConfig)
        prefs.setValue("key", "value")
        prefs.awaitPendingWrites()

        assertEquals("value", prefs.getValue("key"))
        assertTrue(warnings.any { it.contains("Failed to persist preferences") })
        assertTrue(parent.isFile)
    }

    @Test
    fun survivesCorruptFile() {
        val file = tempFile()
        file.parentFile.mkdirs()
        file.writeText("{not json")

        val prefs = PostHogFilePreferences(file, config)
        assertNull(prefs.getValue("key"))
        prefs.setValue("key", "value")
        prefs.awaitPendingWrites()
        assertEquals("value", PostHogFilePreferences(file, config).getValue("key"))
        assertTrue(file.exists())
    }
}
