package com.assignment.taskandmedicinereminderapp.model

import com.google.firebase.Timestamp

data class SleepLog(
    val id: String = "",
    val sleepStart: Timestamp = Timestamp.now(),
    val wakeTime: Timestamp = Timestamp.now(),
    val isInterrupted: Boolean = false,
    val quality: String = "", // "Good", "Average", "Poor"
    val interruptions: List<Timestamp> = emptyList()
) 