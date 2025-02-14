package com.example.healthconnect.codelab.presentation.screen.heartrate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.HeartRateRecord
import com.example.healthconnect.codelab.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Composable
fun HeartRateScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    heartRateReadings: List<HeartRateRecord>,
    uiState: HeartRateViewModel.UiState,
    onInsertClick: (Int, LocalDateTime) -> Unit = { _, _ -> },
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {}
) {
    val errorId = remember { mutableStateOf(UUID.randomUUID()) }
    var heartRateInput by remember { mutableStateOf("") }
    var dateTimeInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is HeartRateViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        if (uiState is HeartRateViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    fun isValidHeartRate(heartRate: String): Boolean {
        val value = heartRate.toIntOrNull()
        return value != null && value in 30..250
    }

    fun isValidDateTime(dateTime: String): Boolean {
        return try {
            LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            true
        } catch (e: Exception) {
            false
        }
    }

    if (uiState != HeartRateViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionsGranted) {
                item {
                    Button(onClick = { onPermissionsLaunch(permissions) }) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Heart Rate Input
                        OutlinedTextField(
                            value = heartRateInput,
                            onValueChange = { heartRateInput = it },
                            label = { Text("Heart Rate (bpm)") },
                            isError = heartRateInput.isNotEmpty() && !isValidHeartRate(heartRateInput),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (heartRateInput.isNotEmpty() && !isValidHeartRate(heartRateInput)) {
                            Text(
                                text = "Please enter a valid heart rate (30-250 bpm)",
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // DateTime Input
                        OutlinedTextField(
                            value = dateTimeInput,
                            onValueChange = { dateTimeInput = it },
                            label = { Text("Date/Time (yyyy-MM-dd HH:mm:ss)") },
                            isError = dateTimeInput.isNotEmpty() && !isValidDateTime(dateTimeInput),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (dateTimeInput.isNotEmpty() && !isValidDateTime(dateTimeInput)) {
                            Text(
                                text = "Please enter a valid date/time (yyyy-MM-dd HH:mm:ss)",
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                enabled = isValidHeartRate(heartRateInput) && isValidDateTime(dateTimeInput),
                                onClick = {
                                    val dateTime = LocalDateTime.parse(
                                        dateTimeInput,
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                    )
                                    onInsertClick(heartRateInput.toInt(), dateTime)
                                    heartRateInput = ""
                                    dateTimeInput = ""
                                }
                            ) {
                                Text("Save")
                            }

                            Button(onClick = { onPermissionsResult() }) {
                                Text("Load")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Heart Rate History",
                            fontSize = 24.sp,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                items(heartRateReadings) { record ->
                    record.samples.forEach { sample ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${sample.beatsPerMinute} bpm")
                            Text(
                                text = DateTimeFormatter
                                    .ofLocalizedDateTime(FormatStyle.MEDIUM)
                                    .format(sample.time.atZone(java.time.ZoneOffset.UTC))
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Student Name: Yu-Ling Wu")
                    Text("Student ID: 301434538")
                }
            }
        }
    }
}