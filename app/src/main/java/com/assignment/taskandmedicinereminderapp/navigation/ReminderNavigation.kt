package com.assignment.taskandmedicinereminderapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assignment.taskandmedicinereminderapp.ui.screens.*
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthState
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import com.assignment.taskandmedicinereminderapp.viewmodel.MedicineViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Dashboard : Screen("dashboard")
    data object Tasks : Screen("tasks")
    data object AddTask : Screen("add_task")
    data object Medicines : Screen("medicines")
    data object AddMedicine : Screen("add_medicine")
}

@Composable
fun ReminderNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    medicineViewModel: MedicineViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    // Determine the start destination based on auth state
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Dashboard.route
        else -> Screen.Login.route
    }
    
    NavHost(navController = navController, startDestination = startDestination) {
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Signup.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                authViewModel = authViewModel,
                medicineViewModel = medicineViewModel,
                onNavigateToTasks = {
                    navController.navigate(Screen.Tasks.route)
                },
                onNavigateToAddTask = {
                    navController.navigate(Screen.AddTask.route)
                },
                onNavigateToMedicines = {
                    navController.navigate(Screen.Medicines.route)
                },
                onNavigateToAddMedicine = {
                    navController.navigate(Screen.AddMedicine.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Tasks.route) {
            TasksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddTask = {
                    navController.navigate(Screen.AddTask.route)
                }
            )
        }
        
        composable(Screen.AddTask.route) {
            AddTaskScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Medicines.route) {
            MedicinesScreen(
                navController = navController,
                viewModel = medicineViewModel
            )
        }
        
        composable(Screen.AddMedicine.route) {
            AddMedicineScreen(
                navController = navController,
                viewModel = medicineViewModel
            )
        }
    }
} 