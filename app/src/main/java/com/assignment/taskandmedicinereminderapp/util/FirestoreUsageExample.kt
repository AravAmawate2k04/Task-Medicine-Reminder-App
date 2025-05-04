package com.assignment.taskandmedicinereminderapp.util

import android.content.Context
import android.widget.Toast
import com.assignment.taskandmedicinereminderapp.model.ActivityLog
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.assignment.taskandmedicinereminderapp.model.SleepLog
import com.assignment.taskandmedicinereminderapp.model.Task
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * This class provides examples of how to use the FirestoreRepository
 * to interact with Firestore in your app.
 */
object FirestoreUsageExample {
    private val repository = FirestoreRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Example: How to create and save a new task
     */
    fun createTask(context: Context, title: String, description: String, deadline: Date, priority: String, isRecurring: Boolean, recurrencePattern: String) {
        scope.launch {
            val task = Task(
                title = title,
                description = description,
                deadline = Timestamp(deadline),
                priority = priority,
                isRecurring = isRecurring,
                recurrencePattern = recurrencePattern,
                createdAt = Timestamp.now(),
                completed = false
            )
            
            val result = repository.addTask(task)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Task created successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to create task: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to fetch and display all tasks
     */
    fun fetchAllTasks(
        context: Context,
        onSuccess: (List<Task>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val result = repository.getTasks()
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val tasks = result.getOrNull() ?: emptyList()
                    onSuccess(tasks)
                } else {
                    onError("Failed to fetch tasks: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
    
    /**
     * Example: How to mark a task as completed
     */
    fun markTaskAsCompleted(context: Context, task: Task, onSuccess: () -> Unit) {
        scope.launch {
            val updatedTask = task.copy(completed = true)
            val result = repository.updateTask(updatedTask)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Task marked as completed", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Failed to update task: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to create and save a new medicine
     */
    fun createMedicine(
        context: Context,
        name: String,
        dosage: String,
        frequency: Int,
        times: List<String>,
        takeWith: String,
        scheduleType: String,
        days: List<String>?
    ) {
        scope.launch {
            val medicine = Medicine(
                name = name,
                dosage = dosage,
                frequency = frequency,
                times = times,
                takeWith = takeWith,
                scheduleType = scheduleType,
                days = days,
                refillCount = 30, // Default refill count
                createdAt = Timestamp.now()
            )
            
            val result = repository.addMedicine(medicine)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Medicine added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to add medicine: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to log a medicine as taken
     */
    fun logMedicineTaken(context: Context, medicine: Medicine, onSuccess: () -> Unit) {
        scope.launch {
            val newTakenLog = medicine.takenLog.toMutableList().apply {
                add(Timestamp.now())
            }
            
            val updatedMedicine = medicine.copy(takenLog = newTakenLog)
            val result = repository.updateMedicine(updatedMedicine)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Medicine logged as taken", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Failed to log medicine: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to create and save a new sleep log
     */
    fun logSleep(
        context: Context,
        sleepStartTime: Date,
        wakeTime: Date,
        isInterrupted: Boolean,
        quality: String,
        interruptions: List<Date> = emptyList()
    ) {
        scope.launch {
            val sleepLog = SleepLog(
                sleepStart = Timestamp(sleepStartTime),
                wakeTime = Timestamp(wakeTime),
                isInterrupted = isInterrupted,
                quality = quality,
                interruptions = interruptions.map { Timestamp(it) }
            )
            
            val result = repository.addSleepLog(sleepLog)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Sleep log added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to add sleep log: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to create and save a new activity log
     */
    fun logActivity(context: Context, steps: Int, goal: Int = 10000) {
        scope.launch {
            val today = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateString = dateFormat.format(today)
            
            val activityLog = ActivityLog(
                date = dateString,
                steps = steps,
                goal = goal,
                updatedAt = Timestamp.now()
            )
            
            val result = repository.addActivityLog(activityLog)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Activity logged successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to log activity: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to update an existing activity log
     */
    fun updateStepCount(context: Context, activityLog: ActivityLog, newStepCount: Int) {
        scope.launch {
            val updatedActivityLog = activityLog.copy(
                steps = newStepCount,
                updatedAt = Timestamp.now()
            )
            
            val result = repository.updateActivityLog(updatedActivityLog)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(context, "Step count updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update step count: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Example: How to fetch recent sleep logs
     */
    fun fetchRecentSleepLogs(
        context: Context,
        onSuccess: (List<SleepLog>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val result = repository.getSleepLogs()
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val sleepLogs = result.getOrNull() ?: emptyList()
                    onSuccess(sleepLogs)
                } else {
                    onError("Failed to fetch sleep logs: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
} 