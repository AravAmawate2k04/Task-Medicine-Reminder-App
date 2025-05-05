package com.assignment.taskandmedicinereminderapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assignment.taskandmedicinereminderapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data object Unauthenticated : AuthState()
    data class AuthError(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"
    private val repository = AuthRepository()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    init {
        Log.d(TAG, "Initializing AuthViewModel")
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        repository.currentUser?.let {
            Log.d(TAG, "User already authenticated: ${it.uid}, email: ${it.email}")
            _authState.value = AuthState.Authenticated(it)
        } ?: run {
            Log.d(TAG, "No authenticated user found")
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        Log.d(TAG, "Attempting login with email: $email")
        viewModelScope.launch {
            try {
                val user = repository.login(email, password)
                Log.d(TAG, "Login successful for user: ${user.uid}")
                
                // Ensure user document exists in Firestore
                try {
                    repository.createOrUpdateUserInFirestore(user)
                    Log.d(TAG, "User document created/updated in Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating user document in Firestore: ${e.message}", e)
                    // Continue with authentication even if Firestore update fails
                }
                
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                Log.e(TAG, "Login failed: ${e.message}", e)
                _authState.value = AuthState.AuthError(e.message ?: "Authentication failed")
            }
        }
    }
    
    fun signUp(email: String, password: String) {
        _authState.value = AuthState.Loading
        Log.d(TAG, "Attempting signup with email: $email")
        viewModelScope.launch {
            try {
                val user = repository.signUp(email, password)
                Log.d(TAG, "Signup successful for user: ${user.uid}")
                
                try {
                    // Create user document and initial collections
                    repository.createOrUpdateUserInFirestore(user)
                    Log.d(TAG, "User document created in Firestore")
                    
                    // Create initial collections for the user
                    repository.createInitialUserCollections(user.uid)
                    Log.d(TAG, "Initial collections created for user")
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up user data in Firestore: ${e.message}", e)
                    // Continue with authentication even if Firestore setup fails
                }
                
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                Log.e(TAG, "Signup failed: ${e.message}", e)
                _authState.value = AuthState.AuthError(e.message ?: "Registration failed")
            }
        }
    }
    
    fun logout() {
        Log.d(TAG, "Logging out user")
        repository.logout()
        _authState.value = AuthState.Unauthenticated
    }
} 