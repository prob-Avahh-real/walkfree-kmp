package com.walkfree.data

import com.walkfree.model.ActivityMode
import com.walkfree.model.MonitoringSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InMemorySessionStoreTest {

    @Test
    fun `save and load sessions`() = runTest {
        val store = InMemorySessionStore()
        val session = MonitoringSession(id = "SES001", startTime = 1000L, endTime = 2000L)
        store.save(session)
        val loaded = store.loadAll()
        assertEquals(1, loaded.size)
        assertEquals("SES001", loaded.first().id)
    }

    @Test
    fun `sessions sorted by descending start time`() = runTest {
        val store = InMemorySessionStore()
        store.save(MonitoringSession(id = "SES001", startTime = 1000L))
        store.save(MonitoringSession(id = "SES002", startTime = 3000L))
        store.save(MonitoringSession(id = "SES003", startTime = 2000L))
        val loaded = store.loadAll()
        assertEquals("SES002", loaded[0].id)
        assertEquals("SES003", loaded[1].id)
        assertEquals("SES001", loaded[2].id)
    }

    @Test
    fun `delete removes session`() = runTest {
        val store = InMemorySessionStore()
        store.save(MonitoringSession(id = "SES001", startTime = 1000L))
        store.save(MonitoringSession(id = "SES002", startTime = 2000L))
        store.delete("SES001")
        val loaded = store.loadAll()
        assertEquals(1, loaded.size)
        assertEquals("SES002", loaded.first().id)
    }
}
