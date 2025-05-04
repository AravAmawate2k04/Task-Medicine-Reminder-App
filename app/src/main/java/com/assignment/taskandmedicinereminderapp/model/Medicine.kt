package com.assignment.taskandmedicinereminderapp.model

import com.google.firebase.Timestamp

data class Medicine(
    val id: String = "",
    val name: String = "",
    val dosage: String = "", // e.g., "1 tablet"
    val frequency: Int = 3, // number of times per day
    val times: List<String> = listOf("08:00", "14:00", "20:00"),
    val takeWith: String = "", // "before food", "after food"
    val scheduleType: String = "", // e.g., "Daily", "Specific Days"
    val days: List<String>? = null,
    val refillCount: Int = 0,
    val takenLog: List<Timestamp> = emptyList(),
    val missedCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
) 