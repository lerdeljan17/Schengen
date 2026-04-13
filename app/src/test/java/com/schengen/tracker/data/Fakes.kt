package com.schengen.tracker.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeStayDao : StayDao {
    private val stays = MutableStateFlow<List<StayEntity>>(emptyList())
    private val profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    private val plannedTrips = MutableStateFlow<List<PlannedTripEntity>>(emptyList())

    private var nextStayId = 1L
    private var nextProfileId = 1L
    private var nextPlannedTripId = 1L

    override fun observeAll(profileId: Long): Flow<List<StayEntity>> =
        stays.map { items ->
            items.filter { it.profileId == profileId }.sortedByDescending { it.entryDate }
        }

    override fun observeProfiles(): Flow<List<ProfileEntity>> =
        profiles.map { items -> items.sortedBy { it.name } }

    override fun observePlannedTrips(profileId: Long): Flow<List<PlannedTripEntity>> =
        plannedTrips.map { items ->
            items.filter { it.profileId == profileId }.sortedBy { it.entryDate }
        }

    override suspend fun insert(stay: StayEntity): Long {
        val entity = stay.withId { nextStayId++ }
        stays.value = stays.value + entity
        return entity.id
    }

    override suspend fun insertProfile(profile: ProfileEntity): Long {
        val entity = profile.withId { nextProfileId++ }
        profiles.value = profiles.value + entity
        return entity.id
    }

    override suspend fun insertPlannedTrip(trip: PlannedTripEntity): Long {
        val entity = trip.withId { nextPlannedTripId++ }
        plannedTrips.value = plannedTrips.value + entity
        return entity.id
    }

    override suspend fun updateStay(stay: StayEntity) {
        stays.value = stays.value.map { if (it.id == stay.id) stay else it }
    }

    override suspend fun updatePlannedTrip(trip: PlannedTripEntity) {
        plannedTrips.value = plannedTrips.value.map { if (it.id == trip.id) trip else it }
    }

    override suspend fun updateProfile(profile: ProfileEntity) {
        profiles.value = profiles.value.map { if (it.id == profile.id) profile else it }
    }

    override suspend fun delete(stay: StayEntity) {
        deleteById(stay.id)
    }

    override suspend fun deleteById(id: Long) {
        stays.value = stays.value.filterNot { it.id == id }
    }

    override suspend fun deletePlannedTripById(id: Long) {
        plannedTrips.value = plannedTrips.value.filterNot { it.id == id }
    }

    override suspend fun deleteStaysByProfileId(profileId: Long) {
        stays.value = stays.value.filterNot { it.profileId == profileId }
    }

    override suspend fun deletePlannedTripsByProfileId(profileId: Long) {
        plannedTrips.value = plannedTrips.value.filterNot { it.profileId == profileId }
    }

    override suspend fun deleteProfileById(id: Long) {
        profiles.value = profiles.value.filterNot { it.id == id }
    }

    override suspend fun getLatestOpenStay(profileId: Long): StayEntity? =
        stays.value
            .filter { it.profileId == profileId && it.exitDate == null }
            .maxByOrNull { it.entryDate }

    override suspend fun getStayById(id: Long): StayEntity? =
        stays.value.firstOrNull { it.id == id }

    override suspend fun getPlannedTripById(id: Long): PlannedTripEntity? =
        plannedTrips.value.firstOrNull { it.id == id }

    override suspend fun getAllStays(profileId: Long): List<StayEntity> =
        stays.value.filter { it.profileId == profileId }.sortedByDescending { it.entryDate }

    override suspend fun getAllPlannedTrips(profileId: Long): List<PlannedTripEntity> =
        plannedTrips.value.filter { it.profileId == profileId }.sortedBy { it.entryDate }

    override suspend fun getAllProfiles(): List<ProfileEntity> =
        profiles.value.sortedBy { it.name }

    override suspend fun getProfileById(id: Long): ProfileEntity? =
        profiles.value.firstOrNull { it.id == id }

    private fun StayEntity.withId(nextId: () -> Long): StayEntity =
        if (id == 0L) copy(id = nextId()) else also { nextStayId = maxOf(nextStayId, id + 1) }

    private fun ProfileEntity.withId(nextId: () -> Long): ProfileEntity =
        if (id == 0L) copy(id = nextId()) else also { nextProfileId = maxOf(nextProfileId, id + 1) }

    private fun PlannedTripEntity.withId(nextId: () -> Long): PlannedTripEntity =
        if (id == 0L) copy(id = nextId()) else also { nextPlannedTripId = maxOf(nextPlannedTripId, id + 1) }
}

internal class FakeSharedPreferences(
    initialValues: Map<String, Any?> = emptyMap()
) : SharedPreferences {
    private val values = initialValues.toMutableMap()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean =
        values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners -= listener
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor =
            put(key, value)

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
            put(key, values?.toSet())

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
            put(key, value)

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
            put(key, value)

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            put(key, value)

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
            put(key, value)

        override fun remove(key: String?): SharedPreferences.Editor =
            put(key, null)

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        private fun applyChanges() {
            val changedKeys = linkedSetOf<String>()
            if (clear) {
                changedKeys += values.keys
                values.clear()
            }
            pending.forEach { (key, value) ->
                changedKeys += key
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
            changedKeys.forEach { key ->
                listeners.forEach { listener ->
                    listener.onSharedPreferenceChanged(this@FakeSharedPreferences, key)
                }
            }
        }
    }
}
