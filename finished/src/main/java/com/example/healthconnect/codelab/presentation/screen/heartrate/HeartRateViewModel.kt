package com.example.healthconnect.codelab.presentation.screen.heartrate

import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthconnect.codelab.data.HealthConnectManager
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.launch

class HeartRateViewModel(private val healthConnectManager: HealthConnectManager) : ViewModel() {
    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
    )
    var permissionsGranted = mutableStateOf(false)
        private set

    var heartRateReadings: MutableState<List<HeartRateRecord>> = mutableStateOf(listOf())
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    fun initialLoad() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                readHeartRateData()
            }
        }
    }

    fun insertHeartRate(heartRate: Int, timestamp: LocalDateTime) {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
                val record = HeartRateRecord(
                    startTime = timestamp.toInstant(ZoneOffset.UTC),
                    endTime = timestamp.toInstant(ZoneOffset.UTC),
                    startZoneOffset = zoneOffset,
                    endZoneOffset = zoneOffset,
                    samples = listOf(
                        HeartRateRecord.Sample(
                            beatsPerMinute = heartRate.toLong(),
                            time = timestamp.toInstant(ZoneOffset.UTC)
                        )
                    )
                )
                healthConnectManager.writeHeartRateRecord(record)
//                readHeartRateData()
            }
        }
    }

    private suspend fun readHeartRateData() {
        val endTime = Instant.now()
        val startTime = endTime.minus(30, ChronoUnit.DAYS)
        heartRateReadings.value = healthConnectManager.readHeartRateData(startTime, endTime)
    }

    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
        uiState = try {
            if (permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}

class HeartRateViewModelFactory(
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeartRateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HeartRateViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}