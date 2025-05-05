package com.assignment.taskandmedicinereminderapp.repository

import com.assignment.taskandmedicinereminderapp.model.ActivityLog
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.assignment.taskandmedicinereminderapp.model.SleepLog
import com.assignment.taskandmedicinereminderapp.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Base path for all user data
    private val baseUserPath get() = "users/${getCurrentUserId()}"
    
    // Collection paths
    private val tasksCollection get() = "$baseUserPath/tasks"
    private val medicinesCollection get() = "$baseUserPath/medicines"
    private val sleepLogsCollection get() = "$baseUserPath/wellness/sleepLogs"
    private val activityLogsCollection get() = "$baseUserPath/wellness/activityLogs"
    
    // Get current user ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }
    
    // ----- Task CRUD Operations -----
    
    suspend fun addTask(task: Task): Result<Task> {
        return try {
            val taskWithId = if (task.id.isBlank()) {
                task.copy(id = UUID.randomUUID().toString())
            } else {
                task
            }
            
            firestore.collection(tasksCollection)
                .document(taskWithId.id)
                .set(taskWithId)
                .await()
                
            Result.success(taskWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTasks(): Result<List<Task>> {
        return try {
            val snapshot = firestore.collection(tasksCollection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val tasks = snapshot.documents.mapNotNull { it.toObject(Task::class.java) }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTask(taskId: String): Result<Task> {
        return try {
            val document = firestore.collection(tasksCollection)
                .document(taskId)
                .get()
                .await()
                
            val task = document.toObject(Task::class.java)
            if (task != null) {
                Result.success(task)
            } else {
                Result.failure(NoSuchElementException("Task not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTask(task: Task): Result<Task> {
        return try {
            firestore.collection(tasksCollection)
                .document(task.id)
                .set(task)
                .await()
                
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            firestore.collection(tasksCollection)
                .document(taskId)
                .delete()
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ----- Medicine CRUD Operations -----
    
    suspend fun addMedicine(medicine: Medicine): Result<Medicine> {
        return try {
            val medicineWithId = if (medicine.id.isBlank()) {
                medicine.copy(id = UUID.randomUUID().toString())
            } else {
                medicine
            }
            
            firestore.collection(medicinesCollection)
                .document(medicineWithId.id)
                .set(medicineWithId)
                .await()
                
            Result.success(medicineWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMedicines(): Result<List<Medicine>> {
        return try {
            val snapshot = firestore.collection(medicinesCollection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val medicines = snapshot.documents.mapNotNull { 
                val medicine = it.toObject(Medicine::class.java)
                // Make sure to preserve the document ID
                medicine?.copy(id = it.id) 
            }
            Result.success(medicines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMedicine(medicineId: String): Result<Medicine> {
        return try {
            val document = firestore.collection(medicinesCollection)
                .document(medicineId)
                .get()
                .await()
                
            val medicine = document.toObject(Medicine::class.java)
            if (medicine != null) {
                Result.success(medicine)
            } else {
                Result.failure(NoSuchElementException("Medicine not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMedicine(medicine: Medicine): Result<Medicine> {
        return try {
            firestore.collection(medicinesCollection)
                .document(medicine.id)
                .set(medicine)
                .await()
                
            Result.success(medicine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMedicine(medicineId: String): Result<Unit> {
        return try {
            firestore.collection(medicinesCollection)
                .document(medicineId)
                .delete()
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ----- SleepLog CRUD Operations -----
    
    suspend fun addSleepLog(sleepLog: SleepLog): Result<SleepLog> {
        return try {
            // Create the wellness document if it doesn't exist
            firestore.document("$baseUserPath/wellness")
                .set(mapOf("createdAt" to com.google.firebase.Timestamp.now()))
                .await()
                
            val sleepLogWithId = if (sleepLog.id.isBlank()) {
                sleepLog.copy(id = UUID.randomUUID().toString())
            } else {
                sleepLog
            }
            
            firestore.collection(sleepLogsCollection)
                .document(sleepLogWithId.id)
                .set(sleepLogWithId)
                .await()
                
            Result.success(sleepLogWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSleepLogs(): Result<List<SleepLog>> {
        return try {
            val snapshot = firestore.collection(sleepLogsCollection)
                .orderBy("sleepStart", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val sleepLogs = snapshot.documents.mapNotNull { it.toObject(SleepLog::class.java) }
            Result.success(sleepLogs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSleepLog(sleepLogId: String): Result<SleepLog> {
        return try {
            val document = firestore.collection(sleepLogsCollection)
                .document(sleepLogId)
                .get()
                .await()
                
            val sleepLog = document.toObject(SleepLog::class.java)
            if (sleepLog != null) {
                Result.success(sleepLog)
            } else {
                Result.failure(NoSuchElementException("Sleep log not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSleepLog(sleepLog: SleepLog): Result<SleepLog> {
        return try {
            firestore.collection(sleepLogsCollection)
                .document(sleepLog.id)
                .set(sleepLog)
                .await()
                
            Result.success(sleepLog)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteSleepLog(sleepLogId: String): Result<Unit> {
        return try {
            firestore.collection(sleepLogsCollection)
                .document(sleepLogId)
                .delete()
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ----- ActivityLog CRUD Operations -----
    
    suspend fun addActivityLog(activityLog: ActivityLog): Result<ActivityLog> {
        return try {
            // Create the wellness document if it doesn't exist
            firestore.document("$baseUserPath/wellness")
                .set(mapOf("createdAt" to com.google.firebase.Timestamp.now()))
                .await()
                
            val activityLogWithId = if (activityLog.id.isBlank()) {
                activityLog.copy(id = UUID.randomUUID().toString())
            } else {
                activityLog
            }
            
            firestore.collection(activityLogsCollection)
                .document(activityLogWithId.id)
                .set(activityLogWithId)
                .await()
                
            Result.success(activityLogWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActivityLogs(): Result<List<ActivityLog>> {
        return try {
            val snapshot = firestore.collection(activityLogsCollection)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val activityLogs = snapshot.documents.mapNotNull { it.toObject(ActivityLog::class.java) }
            Result.success(activityLogs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActivityLog(activityLogId: String): Result<ActivityLog> {
        return try {
            val document = firestore.collection(activityLogsCollection)
                .document(activityLogId)
                .get()
                .await()
                
            val activityLog = document.toObject(ActivityLog::class.java)
            if (activityLog != null) {
                Result.success(activityLog)
            } else {
                Result.failure(NoSuchElementException("Activity log not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateActivityLog(activityLog: ActivityLog): Result<ActivityLog> {
        return try {
            firestore.collection(activityLogsCollection)
                .document(activityLog.id)
                .set(activityLog)
                .await()
                
            Result.success(activityLog)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteActivityLog(activityLogId: String): Result<Unit> {
        return try {
            firestore.collection(activityLogsCollection)
                .document(activityLogId)
                .delete()
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 