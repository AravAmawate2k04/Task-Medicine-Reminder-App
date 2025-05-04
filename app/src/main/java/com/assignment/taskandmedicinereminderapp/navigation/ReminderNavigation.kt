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

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Dashboard : Screen("dashboard")
    data object Tasks : Screen("tasks")
    data object AddTask : Screen("add_task")
}

@Composable
fun ReminderNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
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
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.Tasks.route)
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
    }
} 