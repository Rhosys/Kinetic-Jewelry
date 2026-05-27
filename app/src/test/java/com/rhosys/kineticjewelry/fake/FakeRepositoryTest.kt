package com.rhosys.kineticjewelry.fake

import com.rhosys.kineticjewelry.domain.model.AppFilter
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.ConnectionState
import com.rhosys.kineticjewelry.domain.model.ContactFilter
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRepositoryTest {

    // ── FakeAppFilterRepository ──────────────────────────────────────────────

    @Test
    fun `FakeAppFilterRepository starts empty`() = runBlocking {
        val repo = FakeAppFilterRepository()
        assertNull(repo.getByPackageName("com.example"))
        assertEquals(emptyList<AppFilter>(), repo.observeAll().first())
    }

    @Test
    fun `FakeAppFilterRepository upsert stores and retrieves filter`() = runBlocking {
        val repo = FakeAppFilterRepository()
        val filter = AppFilter("com.example", "Example", true, VibrationMode.SHORT_PULSE, false)
        repo.upsert(filter)
        assertEquals(filter, repo.getByPackageName("com.example"))
    }

    @Test
    fun `FakeAppFilterRepository upsert replaces existing entry`() = runBlocking {
        val repo = FakeAppFilterRepository()
        val v1 = AppFilter("com.example", "Example", true, VibrationMode.SHORT_PULSE, false)
        val v2 = AppFilter("com.example", "Example", false, VibrationMode.LONG_PULSE, true)
        repo.upsert(v1)
        repo.upsert(v2)
        assertEquals(v2, repo.getByPackageName("com.example"))
    }

    @Test
    fun `FakeAppFilterRepository delete removes entry`() = runBlocking {
        val repo = FakeAppFilterRepository()
        repo.upsert(AppFilter("com.example", "Example", true, VibrationMode.SHORT_PULSE, false))
        repo.delete("com.example")
        assertNull(repo.getByPackageName("com.example"))
    }

    @Test
    fun `FakeAppFilterRepository observeAll reflects latest state`() = runBlocking {
        val repo = FakeAppFilterRepository()
        val f1 = AppFilter("com.app1", "App1", true, VibrationMode.SHORT_PULSE, false)
        val f2 = AppFilter("com.app2", "App2", false, VibrationMode.LONG_PULSE, false)
        repo.upsert(f1)
        repo.upsert(f2)
        val all = repo.observeAll().first()
        assertEquals(2, all.size)
        assertTrue(all.contains(f1))
        assertTrue(all.contains(f2))
    }

    // ── FakeContactFilterRepository ──────────────────────────────────────────

    @Test
    fun `FakeContactFilterRepository starts empty`() = runBlocking {
        val repo = FakeContactFilterRepository()
        assertNull(repo.get("com.example", "", "Alice"))
        assertEquals(emptyList<ContactFilter>(), repo.observeByApp("com.example").first())
    }

    @Test
    fun `FakeContactFilterRepository upsert stores and retrieves by composite key`() = runBlocking {
        val repo = FakeContactFilterRepository()
        val contact = ContactFilter("com.example", "", "Alice", true, VibrationMode.SOS)
        repo.upsert(contact)
        assertEquals(contact, repo.get("com.example", "", "Alice"))
    }

    @Test
    fun `FakeContactFilterRepository composite key distinguishes group from direct`() = runBlocking {
        val repo = FakeContactFilterRepository()
        val direct = ContactFilter("com.example", "", "Alice", true, null)
        val grouped = ContactFilter("com.example", "Work", "Alice", false, null)
        repo.upsert(direct)
        repo.upsert(grouped)
        assertEquals(direct, repo.get("com.example", "", "Alice"))
        assertEquals(grouped, repo.get("com.example", "Work", "Alice"))
    }

    @Test
    fun `FakeContactFilterRepository observeByApp only returns contacts for that package`() = runBlocking {
        val repo = FakeContactFilterRepository()
        val c1 = ContactFilter("com.app1", "", "Alice", true, null)
        val c2 = ContactFilter("com.app2", "", "Bob", true, null)
        repo.upsert(c1)
        repo.upsert(c2)
        val forApp1 = repo.observeByApp("com.app1").first()
        assertEquals(1, forApp1.size)
        assertEquals(c1, forApp1[0])
    }

    @Test
    fun `FakeContactFilterRepository delete removes by composite key`() = runBlocking {
        val repo = FakeContactFilterRepository()
        val contact = ContactFilter("com.example", "", "Alice", true, null)
        repo.upsert(contact)
        repo.delete("com.example", "", "Alice")
        assertNull(repo.get("com.example", "", "Alice"))
    }

    // ── FakeBluetoothDeviceRepository ────────────────────────────────────────

    @Test
    fun `FakeBluetoothDeviceRepository starts empty`() = runBlocking {
        val repo = FakeBluetoothDeviceRepository()
        assertEquals(emptyList<BluetoothDeviceInfo>(), repo.getAll())
        assertEquals(emptyList<BluetoothDeviceInfo>(), repo.observeAlertEnabled().first())
    }

    @Test
    fun `FakeBluetoothDeviceRepository upsert stores device`() = runBlocking {
        val repo = FakeBluetoothDeviceRepository()
        val device = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", true, ConnectionState.DISCONNECTED)
        repo.upsert(device)
        assertEquals(listOf(device), repo.getAll())
    }

    @Test
    fun `FakeBluetoothDeviceRepository observeAlertEnabled filters by isAlertEnabled`() = runBlocking {
        val repo = FakeBluetoothDeviceRepository()
        val alertOn  = BluetoothDeviceInfo("AA:BB:CC:DD:EE:01", "Ring", true, ConnectionState.DISCONNECTED)
        val alertOff = BluetoothDeviceInfo("AA:BB:CC:DD:EE:02", "Bracelet", false, ConnectionState.DISCONNECTED)
        repo.upsert(alertOn)
        repo.upsert(alertOff)
        val alertDevices = repo.observeAlertEnabled().first()
        assertEquals(listOf(alertOn), alertDevices)
    }

    @Test
    fun `FakeBluetoothDeviceRepository delete removes device`() = runBlocking {
        val repo = FakeBluetoothDeviceRepository()
        val device = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", true, ConnectionState.DISCONNECTED)
        repo.upsert(device)
        repo.delete("AA:BB:CC:DD:EE:FF")
        assertEquals(emptyList<BluetoothDeviceInfo>(), repo.getAll())
    }
}
