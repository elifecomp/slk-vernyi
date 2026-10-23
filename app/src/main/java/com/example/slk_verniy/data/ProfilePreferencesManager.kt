package com.example.slk_verniy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class EmployeeProfile(
    val name: String = "Анастасия Путилина",
    val position: String = "Специалист торговой сети",
    val storeLocation: String = "Магазин № 124, ул. Ленина, 10",
    val employeeId: String = "777888",
    val avatarUri: String? = null
)

class ProfilePreferencesManager(private val context: Context) {
    companion object {
        private val NAME_KEY = stringPreferencesKey("profile_name")
        private val POSITION_KEY = stringPreferencesKey("profile_position")
        private val STORE_LOCATION_KEY = stringPreferencesKey("profile_store_location")
        private val EMPLOYEE_ID_KEY = stringPreferencesKey("profile_employee_id")
        private val AVATAR_URI_KEY = stringPreferencesKey("profile_avatar_uri")
    }

    val employeeProfile: Flow<EmployeeProfile> = context.dataStore.data
        .catch { _ ->
            try {
                emit(emptyPreferences())
            } catch (_: Exception) {}
        }
        .map { preferences ->
            try {
                EmployeeProfile(
                    name = preferences[NAME_KEY] ?: "Анастасия Путилина",
                    position = preferences[POSITION_KEY] ?: "Специалист торговой сети",
                    storeLocation = preferences[STORE_LOCATION_KEY] ?: "Магазин ВЕРНЫЙ, ул. Григория Соколова,34",
                    employeeId = preferences[EMPLOYEE_ID_KEY] ?: "00001",
                    avatarUri = preferences[AVATAR_URI_KEY]
                )
            } catch (_: Exception) {
                EmployeeProfile()
            }
        }

    val avatarUri: Flow<String?> = employeeProfile.map { it.avatarUri }
    val name: Flow<String> = employeeProfile.map { it.name }

    suspend fun saveProfile(profile: EmployeeProfile) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[NAME_KEY] = profile.name
                    preferences[POSITION_KEY] = profile.position
                    preferences[STORE_LOCATION_KEY] = profile.storeLocation
                    preferences[EMPLOYEE_ID_KEY] = profile.employeeId
                    if (profile.avatarUri != null) {
                        preferences[AVATAR_URI_KEY] = profile.avatarUri
                    } else {
                        preferences.remove(AVATAR_URI_KEY)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
