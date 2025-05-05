package com.assignment.taskandmedicinereminderapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.assignment.taskandmedicinereminderapp.navigation.ReminderNavigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Firebase connectivity on app start
        checkFirebaseConnectivity()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReminderNavigation()
                }
            }
        }
    }
    
    private fun checkFirebaseConnectivity() {
        // Check authentication status
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User is authenticated: ${currentUser.uid}")
            
            // Test Firestore connection
            val firestore = FirebaseFirestore.getInstance()
            val userDocRef = firestore.collection("users").document(currentUser.uid)
            
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d(TAG, "Firestore connection successful, found user document")
                    } else {
                        Log.w(TAG, "User document does not exist in Firestore!")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to connect to Firestore: ${e.message}", e)
                }
                
            // Log medicine collection path
            val medicinesPath = "users/${currentUser.uid}/medicines"
            Log.d(TAG, "Medicine collection path: $medicinesPath")
            
            // Try to access medicines collection
            firestore.collection(medicinesPath)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "Medicine collection access successful, found ${documents.size()} documents")
                    documents.forEach { doc ->
                        Log.d(TAG, "Medicine document: ${doc.id}, data: ${doc.data}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to access medicines collection: ${e.message}", e)
                }
        } else {
            Log.w(TAG, "User is not authenticated at app start")
        }
    }
}