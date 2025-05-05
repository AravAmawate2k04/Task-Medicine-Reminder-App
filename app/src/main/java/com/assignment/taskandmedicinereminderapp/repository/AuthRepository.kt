package com.assignment.taskandmedicinereminderapp.repository

import android.util.Log
import com.assignment.taskandmedicinereminderapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthRepository {
    private val TAG = "AuthRepository"
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun login(email: String, password: String): FirebaseUser {
        Log.d(TAG, "Attempting to log in user: $email")
        return suspendCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { user ->
                        Log.d(TAG, "Login successful for user: ${user.uid}")
                        continuation.resume(user)
                    } ?: run {
                        Log.e(TAG, "Authentication result has no user")
                        continuation.resumeWithException(Exception("Authentication failed"))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Login failed: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        Log.d(TAG, "Attempting to sign up user: $email")
        return suspendCoroutine { continuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    authResult.user?.let { user ->
                        Log.d(TAG, "Sign up successful for user: ${user.uid}")
                        continuation.resume(user)
                    } ?: run {
                        Log.e(TAG, "Sign up result has no user")
                        continuation.resumeWithException(Exception("User creation failed"))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Sign up failed: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    suspend fun createOrUpdateUserInFirestore(user: FirebaseUser) {
        Log.d(TAG, "Creating/updating user document in Firestore: ${user.uid}")
        val userModel = User(
            uid = user.uid,
            email = user.email ?: ""
        )

        val userDocRef = usersCollection.document(user.uid)
        // First check if user exists
        val userDoc = userDocRef.get().await()
        
        if (!userDoc.exists()) {
            // Create new user if doesn't exist
            Log.d(TAG, "User document doesn't exist, creating new one")
            userDocRef.set(userModel).await()
        } else {
            // Update user email if exists (in case it changed)
            Log.d(TAG, "User document exists, updating email")
            userDocRef.update("email", user.email ?: "").await()
        }
    }
    
    suspend fun createInitialUserCollections(userId: String) {
        Log.d(TAG, "Creating initial collections for user: $userId")
        try {
            // Create empty collections for tasks, medicines, and wellness
            // These will be created implicitly when documents are added, but
            // we can create empty documents to ensure the structure exists
            
            val userDocRef = usersCollection.document(userId)
            
            // Create initial tasks collection document
            val tasksDocRef = userDocRef.collection("tasks").document("placeholder")
            val tasksPlaceholder = hashMapOf(
                "placeholder" to true,
                "created" to com.google.firebase.Timestamp.now()
            )
            tasksDocRef.set(tasksPlaceholder).await()
            Log.d(TAG, "Created tasks collection placeholder")
            
            // Create initial medicines collection document
            val medicinesDocRef = userDocRef.collection("medicines").document("placeholder") 
            val medicinesPlaceholder = hashMapOf(
                "placeholder" to true,
                "created" to com.google.firebase.Timestamp.now()
            )
            medicinesDocRef.set(medicinesPlaceholder).await()
            Log.d(TAG, "Created medicines collection placeholder")
            
            // Create wellness document with initial structure
            val wellnessDocRef = userDocRef.collection("wellness").document("info")
            val wellnessInfo = hashMapOf(
                "created" to com.google.firebase.Timestamp.now()
            )
            wellnessDocRef.set(wellnessInfo).await()
            Log.d(TAG, "Created wellness document")
            
            // Delete placeholder documents - they've served their purpose in creating the collections
            tasksDocRef.delete().await()
            medicinesDocRef.delete().await()
            
            Log.d(TAG, "Successfully created all initial collections for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating initial collections: ${e.message}", e)
            throw e
        }
    }

    fun logout() {
        Log.d(TAG, "Logging out user: ${currentUser?.uid}")
        firebaseAuth.signOut()
    }
} 