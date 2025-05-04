package com.assignment.taskandmedicinereminderapp.model

import com.google.firebase.Timestamp

data class ActivityLog(
    val id: String = "",
    val date: String = "", // e.g., "2025-05-03"
    val steps: Int = 0,
    val goal: Int = 10000,
    val updatedAt: Timestamp = Timestamp.now()
) 