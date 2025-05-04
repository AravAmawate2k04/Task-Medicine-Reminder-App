package com.assignment.taskandmedicinereminderapp.viewmodel

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
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        repository.currentUser?.let {
            _authState.value = AuthState.Authenticated(it)
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repository.login(email, password)
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    fun signUp(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repository.signUp(email, password)
                repository.createOrUpdateUserInFirestore(user)
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }
    
    fun logout() {
        repository.logout()
        _authState.value = AuthState.Unauthenticated
    }
} 