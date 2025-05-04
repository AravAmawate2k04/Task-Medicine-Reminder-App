package com.assignment.taskandmedicinereminderapp

import android.app.Application
import com.google.firebase.FirebaseApp

class ReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
} 