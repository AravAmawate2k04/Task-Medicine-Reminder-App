package com.assignment.taskandmedicinereminderapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MedicineViewModel : ViewModel() {
    private val TAG = "MedicineViewModel"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        Log.d(TAG, "ViewModel initialized")
        fetchMedicines()
    }
    
    fun fetchMedicines() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "User not logged in - no user ID available")
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                Log.d(TAG, "Fetching medicines for user: $userId")
                
                val medicinesCollection = firestore.collection("users")
                    .document(userId)
                    .collection("medicines")
                
                Log.d(TAG, "Attempting to get medicines from path: users/$userId/medicines")
                
                val snapshot = medicinesCollection.get().await()
                Log.d(TAG, "Retrieved ${snapshot.documents.size} medicine documents")
                
                if (snapshot.isEmpty) {
                    Log.w(TAG, "No medicines found for user $userId")
                }
                
                for (doc in snapshot.documents) {
                    Log.d(TAG, "Document ID: ${doc.id}")
                    Log.d(TAG, "Document data: ${doc.data}")
                }
                
                val medicinesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Convert document to map
                        val data = doc.data
                        if (data == null) {
                            Log.e(TAG, "Document ${doc.id} has no data")
                            return@mapNotNull null
                        }
                        
                        // Manually create Medicine object from the map to ensure all fields are properly set
                        val id = doc.id
                        val name = data["name"] as? String ?: ""
                        val times = (data["times"] as? List<*>)?.filterIsInstance<String>() ?: listOf()
                        val withFood = data["withFood"] as? Boolean ?: true
                        val durationDays = (data["durationDays"] as? Number)?.toInt() ?: 7
                        val startDate = data["startDate"] as? Timestamp ?: Timestamp.now()
                        val createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
                        // Default active to true if it's not present in the data
                        val active = data["active"] as? Boolean ?: true
                        
                        // Handle taken history with defaults
                        val takenHistory = try {
                            @Suppress("UNCHECKED_CAST")
                            data["takenHistory"] as? Map<String, Map<String, Boolean>> ?: mapOf()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing takenHistory: ${e.message}")
                            mapOf<String, Map<String, Boolean>>()
                        }
                        
                        val medicine = Medicine(
                            id = id,
                            name = name,
                            times = times,
                            withFood = withFood,
                            durationDays = durationDays,
                            startDate = startDate,
                            createdAt = createdAt,
                            active = active,
                            takenHistory = takenHistory
                        )
                        
                        Log.d(TAG, "Successfully parsed medicine: ${medicine.name}, times: ${medicine.times}")
                        medicine
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing medicine document ${doc.id}: ${e.message}", e)
                        Log.e(TAG, "Document data that failed: ${doc.data}")
                        null
                    }
                }
                
                // Include all medicines for debugging
                Log.d(TAG, "Total parsed medicines (including inactive): ${medicinesList.size}")
                medicinesList.forEach { medicine ->
                    Log.d(TAG, "Parsed: ${medicine.name}, active: ${medicine.active}, times: ${medicine.times}")
                }
                
                // Keep all medicines but sort active ones first
                val sortedMedicines = medicinesList.sortedByDescending { it.active }
                
                _medicines.value = sortedMedicines
                Log.d(TAG, "Set medicines state with ${sortedMedicines.size} items")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching medicines: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addMedicine(
        name: String, 
        times: List<String>, 
        withFood: Boolean, 
        durationDays: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "User not logged in - cannot add medicine")
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                Log.d(TAG, "Adding medicine: $name with times: $times")
                
                // Validate time slots
                if (times.isEmpty()) {
                    Log.e(TAG, "Cannot add medicine with empty time slots")
                    _error.value = "Please add at least one time slot"
                    _isLoading.value = false
                    return@launch
                }
                
                val now = Timestamp.now()
                val medicine = Medicine(
                    id = "", // Will be set by Firestore
                    name = name,
                    times = times,
                    withFood = withFood,
                    durationDays = durationDays,
                    startDate = now,
                    createdAt = now,
                    active = true,
                    takenHistory = mapOf()
                )
                
                Log.d(TAG, "Medicine object created: $medicine")
                
                // Create a map that exactly matches the field names in Firestore
                val medicineMap = mapOf(
                    "name" to medicine.name,
                    "times" to medicine.times,
                    "withFood" to medicine.withFood,
                    "durationDays" to medicine.durationDays,
                    "startDate" to medicine.startDate,
                    "createdAt" to medicine.createdAt,
                    "active" to medicine.active,
                    "takenHistory" to medicine.takenHistory
                )
                
                Log.d(TAG, "Medicine map for Firestore: $medicineMap")
                Log.d(TAG, "Adding to collection: users/$userId/medicines")
                
                try {
                    // First make sure the user exists in Firestore
                    val userDoc = firestore.collection("users").document(userId)
                    val userSnapshot = userDoc.get().await()
                    
                    if (!userSnapshot.exists()) {
                        Log.d(TAG, "User document doesn't exist. Creating it first.")
                        val userData = mapOf(
                            "email" to auth.currentUser?.email,
                            "createdAt" to Timestamp.now()
                        )
                        userDoc.set(userData).await()
                    }
                    
                    // Then add the medicine to the user's medicines collection
                    val docRef = firestore.collection("users")
                        .document(userId)
                        .collection("medicines")
                        .add(medicineMap)
                        .await()
                    
                    Log.d(TAG, "Medicine added successfully with ID: ${docRef.id}")
                    Log.d(TAG, "Now refreshing medicine list")
                    
                    // Refresh the medicine list
                    fetchMedicines()
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing to Firestore: ${e.message}", e)
                    _error.value = "Database error: ${e.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding medicine: ${e.message}", e)
                _error.value = "Error adding medicine: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun getTodaySchedule(): List<Pair<String, Boolean>> {
        try {
            // Get current date and time
            val currentCalendar = Calendar.getInstance()
            val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentCalendar.get(Calendar.MINUTE)
            val currentTimeValue = currentHour * 60 + currentMinute
            val today = Medicine.formatDate(currentCalendar.time)
            
            Log.d(TAG, "Getting today's schedule. Date: $today, Time: $currentHour:$currentMinute (value: $currentTimeValue)")
            Log.d(TAG, "Current medicines in state: ${_medicines.value.size}")
            
            if (_medicines.value.isEmpty()) {
                Log.d(TAG, "No medicines available for schedule")
                return emptyList()
            }
            
            // Get all unique time slots from all medicines, regardless of whether they've passed
            val allTimes = mutableListOf<Pair<String, Boolean>>()
            
            // Consider all active medicines only
            _medicines.value.filter { it.isActive() }.forEach { medicine ->
                Log.d(TAG, "Processing medicine for schedule: ${medicine.name}, active: ${medicine.active}")
                
                if (medicine.times.isEmpty()) {
                    Log.w(TAG, "Medicine ${medicine.name} has no scheduled times")
                    return@forEach
                }
                
                medicine.times.forEach { timeString ->
                    if (timeString.isBlank() || !timeString.contains(":")) {
                        Log.w(TAG, "Invalid time format for medicine ${medicine.name}: $timeString")
                        return@forEach
                    }
                    
                    try {
                        val parts = timeString.split(":")
                        if (parts.size < 2) {
                            Log.w(TAG, "Invalid time format (not enough parts) for ${medicine.name}: $timeString")
                            return@forEach
                        }
                        
                        val hour = parts[0].toIntOrNull()
                        val minute = parts[1].toIntOrNull()
                        
                        if (hour == null || minute == null) {
                            Log.w(TAG, "Invalid time format (non-numeric) for ${medicine.name}: $timeString")
                            return@forEach
                        }
                        
                        val timeValue = hour * 60 + minute
                        
                        // FORCE debug comparison of times
                        val isPast = when {
                            // If hour is less than current hour, it's definitely past
                            hour < currentHour -> true
                            // If hour is greater than current hour, it's definitely future
                            hour > currentHour -> false
                            // Same hour, check minutes
                            else -> minute < currentMinute
                        }
                        
                        // Debug all time comparisons
                        val comparisonMsg = when {
                            hour < currentHour -> "$hour:$minute is earlier hour than current $currentHour:$currentMinute"
                            hour > currentHour -> "$hour:$minute is later hour than current $currentHour:$currentMinute"
                            minute < currentMinute -> "$hour:$minute is earlier in same hour than current $currentHour:$currentMinute"
                            else -> "$hour:$minute is same or later in same hour than current $currentHour:$currentMinute"
                        }
                        
                        Log.d(TAG, "TIME COMPARISON: $comparisonMsg, isPast=$isPast, timeValue=$timeValue, currentTimeValue=$currentTimeValue")
                        
                        // Get the status from history, if available
                        val takenStatus = medicine.isTakenToday(timeString)
                        
                        Log.d(TAG, "Medicine ${medicine.name} time: $timeString (value: $timeValue), isPast: $isPast, current: $currentTimeValue, taken: $takenStatus")
                        
                        // Add to the list of times if not already added
                        if (!allTimes.any { it.first == timeString }) {
                            allTimes.add(timeString to isPast)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing time $timeString for medicine ${medicine.name}: ${e.message}")
                    }
                }
            }
            
            // Remove duplicates and sort by time
            val uniqueSortedTimes = allTimes.distinctBy { it.first }.sortedBy {
                try {
                    val parts = it.first.split(":")
                    if (parts.size < 2) return@sortedBy 0
                    val hour = parts[0].toIntOrNull() ?: 0
                    val minute = parts[1].toIntOrNull() ?: 0
                    hour * 60 + minute
                } catch (e: Exception) {
                    Log.e(TAG, "Error sorting time slot ${it.first}: ${e.message}")
                    0 // Default value if parsing fails
                }
            }
            
            Log.d(TAG, "Today's ($today) schedule has ${uniqueSortedTimes.size} time slots: $uniqueSortedTimes")
            return uniqueSortedTimes
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in getTodaySchedule: ${e.message}", e)
            return emptyList()
        }
    }
    
    fun getMedicinesByTimeSlot(): Map<String, List<Medicine>> {
        try {
            val result = mutableMapOf<String, MutableList<Medicine>>()
            val medicines = _medicines.value
            
            Log.d(TAG, "Grouping ${medicines.size} medicines by time slot")
            
            if (medicines.isEmpty()) {
                Log.d(TAG, "No medicines to group by time slot")
                return emptyMap()
            }
            
            // Group medicines by time slot - consider all medicines for debugging
            medicines.forEach { medicine ->
                Log.d(TAG, "Processing medicine: ${medicine.name} with times: ${medicine.times}, active: ${medicine.active}")
                
                if (medicine.times.isEmpty()) {
                    Log.w(TAG, "Medicine ${medicine.name} has no scheduled times")
                    return@forEach
                }
                
                medicine.times.forEach { timeSlot ->
                    if (timeSlot.isBlank() || !timeSlot.contains(":")) {
                        Log.w(TAG, "Invalid time format for medicine ${medicine.name}: $timeSlot")
                        return@forEach
                    }
                    
                    try {
                        // Validate time format
                        val parts = timeSlot.split(":")
                        if (parts.size < 2) {
                            Log.w(TAG, "Invalid time parts for medicine ${medicine.name}: $timeSlot")
                            return@forEach
                        }
                        
                        // Add medicine to the appropriate time slot list
                        if (!result.containsKey(timeSlot)) {
                            result[timeSlot] = mutableListOf()
                        }
                        result[timeSlot]?.add(medicine)
                        Log.d(TAG, "Added medicine ${medicine.name} to time slot $timeSlot")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing time $timeSlot for medicine ${medicine.name}: ${e.message}")
                    }
                }
            }
            
            // Sort the result by time slots
            val sortedResult = result.entries.sortedBy {
                try {
                    val parts = it.key.split(":")
                    if (parts.size < 2) return@sortedBy 0
                    
                    val hour = parts[0].toIntOrNull() ?: 0
                    val minute = parts[1].toIntOrNull() ?: 0
                    hour * 60 + minute
                } catch (e: Exception) {
                    Log.e(TAG, "Error sorting time slot ${it.key}: ${e.message}")
                    0 // Default value if parsing fails
                }
            }.associate { it.key to it.value }
            
            Log.d(TAG, "Grouped medicines into ${sortedResult.size} time slots: ${sortedResult.keys}")
            return sortedResult
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in getMedicinesByTimeSlot: ${e.message}", e)
            return emptyMap()
        }
    }
    
    // Mark a medicine as taken for today at a specific time
    fun markMedicineTaken(medicineId: String, timeSlot: String, taken: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "User not logged in - cannot mark medicine as taken")
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                // Log the action for debugging
                val status = if (taken) "taken" else "skipped"
                Log.d(TAG, "Marking medicine $medicineId at time $timeSlot as $status")
                
                // Find the medicine in the local list
                val medicineIndex = _medicines.value.indexOfFirst { it.id == medicineId }
                if (medicineIndex == -1) {
                    Log.e(TAG, "Medicine $medicineId not found in local list")
                    _error.value = "Medicine not found"
                    _isLoading.value = false
                    return@launch
                }
                
                val medicine = _medicines.value[medicineIndex]
                
                // Get today's date as a string
                val today = Medicine.formatDate(Calendar.getInstance().time)
                
                // Create or update the taken history for today
                val updatedTakenHistory = medicine.takenHistory.toMutableMap()
                val todayHistory = updatedTakenHistory[today]?.toMutableMap() ?: mutableMapOf()
                todayHistory[timeSlot] = taken
                updatedTakenHistory[today] = todayHistory
                
                // Create an updated copy of the medicine
                val updatedMedicine = medicine.copy(takenHistory = updatedTakenHistory)
                
                // Update local list immediately
                val updatedList = _medicines.value.toMutableList()
                updatedList[medicineIndex] = updatedMedicine
                _medicines.value = updatedList
                
                // Now update Firestore in the background
                try {
                    val medicineRef = firestore.collection("users")
                        .document(userId)
                        .collection("medicines")
                        .document(medicineId)
                        
                    medicineRef.update("takenHistory", updatedTakenHistory).await()
                    Log.d(TAG, "Successfully updated medicine $medicineId as $status for time $timeSlot")
                    
                    // Refresh from Firebase to ensure we have the latest data
                    fetchMedicines()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating medicine in Firestore: ${e.message}", e)
                    
                    // Try to set the entire document if update fails (might be missing)
                    try {
                        val medicineRef = firestore.collection("users")
                            .document(userId)
                            .collection("medicines")
                            .document(medicineId)
                            
                        val medicineData = mapOf(
                            "name" to medicine.name,
                            "times" to medicine.times,
                            "withFood" to medicine.withFood,
                            "durationDays" to medicine.durationDays,
                            "startDate" to medicine.startDate,
                            "createdAt" to medicine.createdAt,
                            "active" to medicine.active,
                            "takenHistory" to updatedTakenHistory
                        )
                        
                        medicineRef.set(medicineData).await()
                        Log.d(TAG, "Recreated medicine document with updated taken history")
                        fetchMedicines()
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to recreate medicine document: ${e2.message}", e2)
                        _error.value = "Failed to update medicine: ${e2.message}"
                    }
                } finally {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking medicine as taken: ${e.message}", e)
                _error.value = "Error updating medicine: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // Archive a medicine instead of deleting it
    fun archiveMedicine(medicineId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _error.value = "User not logged in"
                    return@launch
                }
                
                Log.d(TAG, "Archiving medicine $medicineId")
                
                // Update the active status to false
                firestore.collection("users")
                    .document(userId)
                    .collection("medicines")
                    .document(medicineId)
                    .update("active", false)
                    .await()
                
                Log.d(TAG, "Successfully archived medicine")
                
                // Refresh the medicine list
                fetchMedicines()
            } catch (e: Exception) {
                Log.e(TAG, "Error archiving medicine: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    // Get adherence statistics for a specific medicine
    fun getMedicineAdherenceStats(medicineId: String): Flow<Pair<Int, Int>> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                throw IllegalStateException("User not logged in")
            }
            
            // Get the medicine
            val medicineDoc = firestore.collection("users")
                .document(userId)
                .collection("medicines")
                .document(medicineId)
                .get()
                .await()
            
            val medicine = medicineDoc.toObject(Medicine::class.java)
            if (medicine == null) {
                throw IllegalStateException("Medicine not found")
            }
            
            var totalDoses = 0
            var takenDoses = 0
            
            medicine.takenHistory.forEach { (_, timesMap) ->
                timesMap.forEach { (_, taken) ->
                    totalDoses++
                    if (taken) takenDoses++
                }
            }
            
            emit(Pair(takenDoses, totalDoses))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting adherence stats: ${e.message}", e)
            emit(Pair(0, 0))
        }
    }
} 