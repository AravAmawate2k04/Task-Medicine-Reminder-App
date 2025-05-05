package com.assignment.taskandmedicinereminderapp.model

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class Medicine(
    val id: String = "",
    val name: String = "",
    val times: List<String> = listOf(), // Format: "HH:mm"
    val withFood: Boolean = true,
    val durationDays: Int = 7,
    val startDate: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(), // Adding createdAt field for sorting
    val active: Boolean = true, // Whether the medicine is active or archived
    val takenHistory: Map<String, Map<String, Boolean>> = mapOf() // Map of date -> (time -> taken status)
) {
    // Check if the medicine is still active (not expired)
    fun isActive(): Boolean {
        // If no time slots, medicine is considered inactive
        if (times.isEmpty()) {
            return false
        }
        
        // Check if medicine is manually marked inactive
        if (!active) {
            return false
        }
        
        // Check if medicine has expired based on duration
        val currentTime = Calendar.getInstance().time.time
        val startTime = startDate.toDate().time
        val endTime = startTime + TimeUnit.DAYS.toMillis(durationDays.toLong())
        return currentTime <= endTime
    }
    
    // Get days remaining for the medicine course
    fun daysRemaining(): Int {
        val currentTime = Calendar.getInstance().time.time
        val startTime = startDate.toDate().time
        val endTime = startTime + TimeUnit.DAYS.toMillis(durationDays.toLong())
        
        // If already expired, return 0
        if (currentTime > endTime) return 0
        
        val remainingMillis = endTime - currentTime
        return TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt()
    }
    
    // Check if medicine was taken today at a specific time
    fun isTakenToday(timeSlot: String): Boolean? {
        val today = formatDate(Calendar.getInstance().time)
        
        // Debug logging
        val hasDateEntry = takenHistory.containsKey(today)
        val hasTimeEntry = hasDateEntry && takenHistory[today]?.containsKey(timeSlot) == true
        val value = if (hasTimeEntry) takenHistory[today]?.get(timeSlot) else null
        
        // If there's no entry for today or the time slot, return null instead of false
        // This allows us to distinguish between "not taken yet" (null) and "explicitly skipped" (false)
        if (!hasDateEntry || !hasTimeEntry) {
            return null
        }
        return value
    }
    
    // Calculate adherence rate (how many doses were taken out of total)
    fun adherenceRate(): Float {
        var totalDoses = 0
        var takenDoses = 0
        
        takenHistory.forEach { (_, timesMap) ->
            timesMap.forEach { (_, taken) ->
                totalDoses++
                if (taken) takenDoses++
            }
        }
        
        return if (totalDoses > 0) takenDoses.toFloat() / totalDoses else 1.0f
    }
    
    // Format date to string for takenHistory map
    companion object {
        fun formatDate(date: java.util.Date): String {
            val cal = Calendar.getInstance()
            cal.time = date
            return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }
} 