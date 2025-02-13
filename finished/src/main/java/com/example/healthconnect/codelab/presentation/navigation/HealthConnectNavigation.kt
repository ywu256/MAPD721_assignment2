/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthconnect.codelab.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.healthconnect.codelab.data.HealthConnectManager
import com.example.healthconnect.codelab.presentation.screen.WelcomeScreen
import com.example.healthconnect.codelab.presentation.screen.changes.DifferentialChangesScreen
import com.example.healthconnect.codelab.presentation.screen.changes.DifferentialChangesViewModel
import com.example.healthconnect.codelab.presentation.screen.changes.DifferentialChangesViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.exercisesession.ExerciseSessionScreen
import com.example.healthconnect.codelab.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.healthconnect.codelab.presentation.screen.exercisesession.ExerciseSessionViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.exercisesessiondetail.ExerciseSessionDetailScreen
import com.example.healthconnect.codelab.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModel
import com.example.healthconnect.codelab.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsScreen
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsViewModel
import com.example.healthconnect.codelab.presentation.screen.inputreadings.InputReadingsViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.privacypolicy.PrivacyPolicyScreen
import com.example.healthconnect.codelab.showExceptionSnackbar

/**
 * Provides the navigation in the app.
 */
@Composable
fun HealthConnectNavigation(
  navController: NavHostController,
  healthConnectManager: HealthConnectManager,
  scaffoldState: ScaffoldState,
) {
  val scope = rememberCoroutineScope()
  NavHost(navController = navController, startDestination = Screen.WelcomeScreen.route) {
    val availability by healthConnectManager.availability
    composable(Screen.WelcomeScreen.route) {
      WelcomeScreen(
        healthConnectAvailability = availability,
        onResumeAvailabilityCheck = {
          healthConnectManager.checkAvailability()
        }
      )
    }
    composable(
      route = Screen.PrivacyPolicy.route,
      deepLinks = listOf(
        navDeepLink {
          action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
        }
      )
    ) {
      PrivacyPolicyScreen()
    }
    composable(Screen.ExerciseSessions.route) {
      val viewModel: ExerciseSessionViewModel = viewModel(
        factory = ExerciseSessionViewModelFactory(
          healthConnectManager = healthConnectManager
        )
      )
      val permissionsGranted by viewModel.permissionsGranted
      val sessionsList by viewModel.sessionsList
      val permissions = viewModel.permissions
      val backgroundReadPermissions = viewModel.backgroundReadPermissions
      val backgroundReadAvailable by viewModel.backgroundReadAvailable
      val backgroundReadGranted by viewModel.backgroundReadGranted
      val onPermissionsResult = { viewModel.initialLoad() }
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      ExerciseSessionScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        backgroundReadAvailable = backgroundReadAvailable,
        backgroundReadGranted = backgroundReadGranted,
        backgroundReadPermissions = backgroundReadPermissions,
        onReadClick = {
          viewModel.enqueueReadStepWorker()
        },
        sessionsList = sessionsList,
        uiState = viewModel.uiState,
        onInsertClick = {
          viewModel.insertExerciseSession()
        },
        onDetailsClick = { uid ->
          navController.navigate(Screen.ExerciseSessionDetail.route + "/" + uid)
        },
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
          permissionsLauncher.launch(values)
        }
      )
    }
    composable(Screen.ExerciseSessionDetail.route + "/{$UID_NAV_ARGUMENT}") {
      val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
      val viewModel: ExerciseSessionDetailViewModel = viewModel(
        factory = ExerciseSessionDetailViewModelFactory(
          uid = uid,
          healthConnectManager = healthConnectManager
        )
      )
      val permissionsGranted by viewModel.permissionsGranted
      val sessionMetrics by viewModel.sessionMetrics
      val permissions = viewModel.permissions
      val onPermissionsResult = { viewModel.initialLoad() }
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      ExerciseSessionDetailScreen(
        permissions = permissions,
        permissionsGranted = permissionsGranted,
        sessionMetrics = sessionMetrics,
        uiState = viewModel.uiState,
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
          permissionsLauncher.launch(values)
        }
      )
    }
    composable(Screen.InputReadings.route) {
      val viewModel: InputReadingsViewModel = viewModel(
        factory = InputReadingsViewModelFactory(
          healthConnectManager = healthConnectManager
        )
      )
      val permissionsGranted by viewModel.permissionsGranted
      val readingsList by viewModel.readingsList
      val permissions = viewModel.permissions
      val weeklyAvg by viewModel.weeklyAvg
      val onPermissionsResult = { viewModel.initialLoad() }
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      InputReadingsScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,

        uiState = viewModel.uiState,
        onInsertClick = { weightInput ->
          viewModel.inputReadings(weightInput)
        },
        weeklyAvg = weeklyAvg,
        readingsList = readingsList,
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
          permissionsLauncher.launch(values)
        }
      )
    }
    composable(Screen.DifferentialChanges.route) {
      val viewModel: DifferentialChangesViewModel = viewModel(
        factory = DifferentialChangesViewModelFactory(
          healthConnectManager = healthConnectManager
        )
      )
      val changesToken by viewModel.changesToken
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = {viewModel.initialLoad()}
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()}
      DifferentialChangesScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        changesEnabled = changesToken != null,
        onChangesEnable = { enabled ->
          viewModel.enableOrDisableChanges(enabled)
        },
        changes = viewModel.changes,
        changesToken = changesToken,
        onGetChanges = {
          viewModel.getChanges()
        },
        uiState = viewModel.uiState,
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
          permissionsLauncher.launch(values)}
      )
    }
  }
}
