package com.assignment.taskandmedicinereminderapp.util

import android.util.Log
import com.assignment.taskandmedicinereminderapp.model.ActivityLog
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.assignment.taskandmedicinereminderapp.model.SleepLog
import com.assignment.taskandmedicinereminderapp.model.Task
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Utility class to test Firestore operations
 * This can be used to quickly add sample data or test data retrieval
 */
object FirestoreTestUtil {
    private const val TAG = "FirestoreTestUtil"
    private val repository = FirestoreRepository()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Add sample tasks
    fun addSampleTasks() {
        scope.launch {
            try {
                // High priority task
                val highPriorityTask = Task(
                    title = "Complete project presentation",
                    description = "Finish slides and practice delivery",
                    deadline = getTimestampInFuture(3), // 3 days from now
                    priority = "Urgent & Important",
                    isRecurring = false,
                    recurrencePattern = "",
                    createdAt = Timestamp.now(),
                    completed = false
                )
                
                // Medium priority recurring task
                val mediumPriorityTask = Task(
                    title = "Weekly team meeting",
                    description = "Discuss project progress and roadblocks",
                    deadline = getTimestampInFuture(7), // 7 days from now
                    priority = "Important, Not Urgent",
                    isRecurring = true,
                    recurrencePattern = "Weekly",
                    createdAt = Timestamp.now(),
                    completed = false
                )
                
                // Low priority task
                val lowPriorityTask = Task(
                    title = "Organize desk",
                    description = "Clean up workspace",
                    deadline = getTimestampInFuture(10), // 10 days from now
                    priority = "Not Urgent, Not Important",
                    isRecurring = false,
                    recurrencePattern = "",
                    createdAt = Timestamp.now(),
                    completed = false
                )
                
                repository.addTask(highPriorityTask)
                repository.addTask(mediumPriorityTask)
                repository.addTask(lowPriorityTask)
                
                Log.d(TAG, "Sample tasks added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sample tasks", e)
            }
        }
    }
    
    // Add sample medicines
    fun addSampleMedicines() {
        scope.launch {
            try {
                // Daily medication
                val dailyMedicine = Medicine(
                    name = "Vitamin D",
                    times = listOf("08:00"),
                    withFood = true,
                    durationDays = 30,
                    startDate = Timestamp.now()
                )
                
                // Multiple times per day medication
                val multipleDailyMedicine = Medicine(
                    name = "Blood Pressure Medication",
                    times = listOf("08:00", "20:00"),
                    withFood = false,
                    durationDays = 60,
                    startDate = Timestamp.now()
                )
                
                // Three times a day medication
                val threeTimesMedicine = Medicine(
                    name = "Pain Reliever",
                    times = listOf("08:00", "14:00", "20:00"),
                    withFood = true,
                    durationDays = 7,
                    startDate = Timestamp.now()
                )
                
                repository.addMedicine(dailyMedicine)
                repository.addMedicine(multipleDailyMedicine)
                repository.addMedicine(threeTimesMedicine)
                
                Log.d(TAG, "Sample medicines added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sample medicines", e)
            }
        }
    }
    
    // Add sample sleep logs
    fun addSampleSleepLogs() {
        scope.launch {
            try {
                // Good night's sleep
                val goodSleep = SleepLog(
                    sleepStart = getTimestampAtTime(previousDay(), 22, 30), // 10:30 PM yesterday
                    wakeTime = getTimestampAtTime(today(), 7, 0), // 7:00 AM today
                    isInterrupted = false,
                    quality = "Good",
                    interruptions = emptyList()
                )
                
                // Interrupted sleep
                val interruptedSleep = SleepLog(
                    sleepStart = getTimestampAtTime(twoDaysAgo(), 23, 0), // 11:00 PM two days ago
                    wakeTime = getTimestampAtTime(yesterday(), 6, 30), // 6:30 AM yesterday
                    isInterrupted = true,
                    quality = "Average",
                    interruptions = listOf(
                        getTimestampAtTime(yesterday(), 2, 15), // 2:15 AM
                        getTimestampAtTime(yesterday(), 4, 45)  // 4:45 AM
                    )
                )
                
                // Poor sleep
                val poorSleep = SleepLog(
                    sleepStart = getTimestampAtTime(yesterday(), 1, 0), // 1:00 AM yesterday
                    wakeTime = getTimestampAtTime(yesterday(), 6, 0), // 6:00 AM yesterday
                    isInterrupted = true,
                    quality = "Poor",
                    interruptions = listOf(
                        getTimestampAtTime(yesterday(), 3, 30), // 3:30 AM
                        getTimestampAtTime(yesterday(), 5, 0)   // 5:00 AM
                    )
                )
                
                repository.addSleepLog(goodSleep)
                repository.addSleepLog(interruptedSleep)
                repository.addSleepLog(poorSleep)
                
                Log.d(TAG, "Sample sleep logs added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sample sleep logs", e)
            }
        }
    }
    
    // Add sample activity logs
    fun addSampleActivityLogs() {
        scope.launch {
            try {
                // Today's activity
                val todayActivity = ActivityLog(
                    date = formatDate(today()),
                    steps = 8500,
                    goal = 10000,
                    updatedAt = Timestamp.now()
                )
                
                // Yesterday's activity
                val yesterdayActivity = ActivityLog(
                    date = formatDate(yesterday()),
                    steps = 12000,
                    goal = 10000,
                    updatedAt = Timestamp.now()
                )
                
                // Two days ago activity
                val twoDaysAgoActivity = ActivityLog(
                    date = formatDate(twoDaysAgo()),
                    steps = 5000,
                    goal = 10000,
                    updatedAt = Timestamp.now()
                )
                
                repository.addActivityLog(todayActivity)
                repository.addActivityLog(yesterdayActivity)
                repository.addActivityLog(twoDaysAgoActivity)
                
                Log.d(TAG, "Sample activity logs added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sample activity logs", e)
            }
        }
    }
    
    // Test retrieving data
    fun testDataRetrieval() {
        scope.launch {
            try {
                Log.d(TAG, "======= FIRESTORE DATA RETRIEVAL TEST STARTED =======")
                
                // Get tasks
                val tasksResult = repository.getTasks()
                if (tasksResult.isSuccess) {
                    val tasks = tasksResult.getOrNull()
                    Log.d(TAG, "📋 Retrieved ${tasks?.size ?: 0} tasks")
                    tasks?.forEach { Log.d(TAG, "  ✓ Task: ${it.title} (Priority: ${it.priority})") }
                } else {
                    Log.e(TAG, "❌ Failed to retrieve tasks: ${tasksResult.exceptionOrNull()?.message}")
                }
                
                // Get medicines
                val medicinesResult = repository.getMedicines()
                if (medicinesResult.isSuccess) {
                    val medicines = medicinesResult.getOrNull()
                    Log.d(TAG, "💊 Retrieved ${medicines?.size ?: 0} medicines")
                    medicines?.forEach { 
                        Log.d(TAG, "  ✓ Medicine: ${it.name} (Times: ${it.times.joinToString()}, With Food: ${it.withFood})")
                    }
                } else {
                    Log.e(TAG, "❌ Failed to retrieve medicines: ${medicinesResult.exceptionOrNull()?.message}")
                }
                
                // Get sleep logs
                val sleepLogsResult = repository.getSleepLogs()
                if (sleepLogsResult.isSuccess) {
                    val sleepLogs = sleepLogsResult.getOrNull()
                    Log.d(TAG, "😴 Retrieved ${sleepLogs?.size ?: 0} sleep logs")
                    sleepLogs?.forEach { Log.d(TAG, "  ✓ Sleep Quality: ${it.quality} (Duration: ${(it.wakeTime.seconds - it.sleepStart.seconds) / 3600.0} hours)") }
                } else {
                    Log.e(TAG, "❌ Failed to retrieve sleep logs: ${sleepLogsResult.exceptionOrNull()?.message}")
                }
                
                // Get activity logs
                val activityLogsResult = repository.getActivityLogs()
                if (activityLogsResult.isSuccess) {
                    val activityLogs = activityLogsResult.getOrNull()
                    Log.d(TAG, "🏃 Retrieved ${activityLogs?.size ?: 0} activity logs")
                    activityLogs?.forEach { Log.d(TAG, "  ✓ Activity Date: ${it.date}, Steps: ${it.steps}/${it.goal}") }
                } else {
                    Log.e(TAG, "❌ Failed to retrieve activity logs: ${activityLogsResult.exceptionOrNull()?.message}")
                }
                
                Log.d(TAG, "======= FIRESTORE DATA RETRIEVAL TEST COMPLETED =======")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in data retrieval test", e)
            }
        }
    }
    
    // Add all sample data at once
    fun addAllSampleData() {
        addSampleTasks()
        addSampleMedicines()
        addSampleSleepLogs()
        addSampleActivityLogs()
    }
    
    // Helper functions
    private fun getTimestampInFuture(days: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return Timestamp(calendar.time)
    }
    
    private fun today(): Date {
        return Calendar.getInstance().time
    }
    
    private fun yesterday(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return calendar.time
    }
    
    private fun previousDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return calendar.time
    }
    
    private fun twoDaysAgo(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        return calendar.time
    }
    
    private fun getTimestampAtTime(date: Date, hour: Int, minute: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        return Timestamp(calendar.time)
    }
    
    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
} 