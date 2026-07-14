package com.posthog.kmp

import com.posthog.internal.PostHogPreferences
import com.posthog.internal.PostHogPreferences.Companion.ALL_INTERNAL_KEYS
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * File-backed [PostHogPreferences] so identity, opt-out state and cached feature flags
 * survive process restarts. The store is a single JSON object; writes go to a temp file
 * and are moved over the previous one so a crash mid-write cannot corrupt the store.
 */
internal class PostHogFilePreferences(
    private val file: File,
    private val config: com.posthog.PostHogConfig,
) : PostHogPreferences {
    private val lock = Any()
    private val preferences: MutableMap<String, Any> by lazy { load() }

    override fun getValue(key: String, defaultValue: Any?): Any? {
        synchronized(lock) {
            return preferences[key] ?: defaultValue
        }
    }

    override fun setValue(key: String, value: Any) {
        synchronized(lock) {
            preferences[key] = value
            persist()
        }
    }

    override fun clear(except: List<String>) {
        synchronized(lock) {
            val it = preferences.iterator()
            while (it.hasNext()) {
                if (!except.contains(it.next().key)) {
                    it.remove()
                }
            }
            persist()
        }
    }

    override fun remove(key: String) {
        synchronized(lock) {
            preferences.remove(key)
            persist()
        }
    }

    override fun getAll(): Map<String, Any> {
        val props: Map<String, Any>
        synchronized(lock) {
            props = preferences.toMap()
        }
        return props.filterKeys { key ->
            !ALL_INTERNAL_KEYS.contains(key)
        }
    }

    private fun load(): MutableMap<String, Any> {
        return try {
            if (!file.exists()) return mutableMapOf()
            @Suppress("UNCHECKED_CAST")
            (config.serializer.deserializeString(file.readText()) as? Map<String, Any>)
                ?.toMutableMap() ?: mutableMapOf()
        } catch (e: Throwable) {
            config.logger.log("Failed to load preferences from ${file.absolutePath}: $e.")
            mutableMapOf()
        }
    }

    private fun persist() {
        try {
            val json = config.serializer.serializeObject(preferences) ?: return
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json)
            try {
                Files.move(
                    tmp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (ignored: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Throwable) {
            config.logger.log("Failed to persist preferences to ${file.absolutePath}: $e.")
        }
    }
}
