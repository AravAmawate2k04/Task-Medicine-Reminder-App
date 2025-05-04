package com.assignment.taskandmedicinereminderapp.model

import com.google.firebase.Timestamp

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val deadline: Timestamp = Timestamp.now(),
    val priority: String = "", // e.g., "Urgent & Important"
    val isRecurring: Boolean = false,
    val recurrencePattern: String = "", // e.g., "Daily", "Weekly"
    val createdAt: Timestamp = Timestamp.now(),
    val completed: Boolean = false,
    val category: String = "",
    val reminderTime: Timestamp? = null
) 