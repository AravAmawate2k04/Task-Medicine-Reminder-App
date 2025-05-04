package com.assignment.taskandmedicinereminderapp.repository

import com.assignment.taskandmedicinereminderapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun login(email: String, password: String): FirebaseUser {
        return suspendCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { user ->
                        continuation.resume(user)
                    } ?: continuation.resumeWithException(Exception("Authentication failed"))
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        return suspendCoroutine { continuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { user ->
                        continuation.resume(user)
                    } ?: continuation.resumeWithException(Exception("User creation failed"))
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    suspend fun createOrUpdateUserInFirestore(user: FirebaseUser) {
        val userModel = User(
            uid = user.uid,
            email = user.email ?: ""
        )

        val userDocRef = usersCollection.document(user.uid)
        // First check if user exists
        val userDoc = userDocRef.get().await()
        
        if (!userDoc.exists()) {
            // Create new user if doesn't exist
            userDocRef.set(userModel).await()
        } else {
            // Update user email if exists (in case it changed)
            userDocRef.update("email", user.email ?: "").await()
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
} 