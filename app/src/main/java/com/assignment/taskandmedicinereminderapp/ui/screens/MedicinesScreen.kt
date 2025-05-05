package com.assignment.taskandmedicinereminderapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.assignment.taskandmedicinereminderapp.navigation.Screen
import com.assignment.taskandmedicinereminderapp.viewmodel.MedicineViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    navController: NavController,
    viewModel: MedicineViewModel = viewModel()
) {
    var errorState by remember { mutableStateOf<String?>(null) }
    
    val medicines by viewModel.medicines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val medicinesByTimeSlot = viewModel.getMedicinesByTimeSlot()
    
    // Use a separate state for tracking local updates
    val localMedicineUpdates = remember { mutableStateMapOf<String, Map<String, Boolean>>() }
    
    // Ensure medicines are loaded
    LaunchedEffect(key1 = Unit) {
        Log.d("MedicinesScreen", "Loading medicines")
        try {
            viewModel.fetchMedicines()
        } catch (e: Exception) {
            Log.e("MedicinesScreen", "Error loading medicines: ${e.message}", e)
            errorState = "Failed to load medicines"
        }
    }
    
    // Function to handle medicine action (take or skip)
    val handleMedicineAction = { medicineId: String, timeSlot: String, taken: Boolean ->
        try {
            // Update local tracking first
            val medicineKey = "$medicineId:$timeSlot"
            localMedicineUpdates[medicineKey] = mapOf("taken" to taken)
            
            // Then update in Firebase
            viewModel.markMedicineTaken(medicineId, timeSlot, taken)
        } catch (e: Exception) {
            Log.e("MedicinesScreen", "Error taking medicine: ${e.message}", e)
            errorState = "Failed to update medicine status"
        }
    }

    // Apple Watch-inspired dark theme colors
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF1C1C1E)
    val accentColor = Color(0xFF0A84FF)
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAEAEB2)
    val successColor = Color(0xFF30D158)
    val errorColor = Color(0xFFFF453A)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Schedule", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { 
                        try {
                            navController.popBackStack() 
                        } catch (e: Exception) {
                            Log.e("MedicinesScreen", "Navigation error: ${e.message}", e)
                            errorState = "Navigation error"
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground,
                    titleContentColor = textColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    try {
                        navController.navigate(Screen.AddMedicine.route) 
                    } catch (e: Exception) {
                        Log.e("MedicinesScreen", "Navigation error to add screen: ${e.message}", e)
                        errorState = "Navigation error"
                    }
                },
                containerColor = successColor,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medicine"
                )
            }
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(darkBackground)
        ) {
            when {
                errorState != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = errorColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorState ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = errorColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                errorState = null
                                viewModel.fetchMedicines() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accentColor
                    )
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = errorColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (medicinesByTimeSlot.isEmpty() && medicines.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No medicines found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { 
                                            try {
                                                navController.navigate(Screen.AddMedicine.route)
                                            } catch (e: Exception) {
                                                Log.e("MedicinesScreen", "Navigation error: ${e.message}", e)
                                                errorState = "Navigation error"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Text("Add Medicine")
                                    }
                                }
                            }
                        } else if (medicinesByTimeSlot.isEmpty() && medicines.isNotEmpty()) {
                            // We have medicines but no time slots - explain why
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = cardBackground.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Your medicines may be inactive or have invalid time slots",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            // Group medicines by time slot and display them
                            medicinesByTimeSlot.forEach { (timeSlot, medicines) ->
                                item {
                                    val isPastTimeSlot = safeTimeSlotIsPast(timeSlot)
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = formatTimeSlot(timeSlot),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isPastTimeSlot) secondaryTextColor else accentColor,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        
                                        if (isPastTimeSlot) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF0A84FF), CircleShape)
                                            )
                                        }
                                    }
                                }
                                
                                // Medicines for this time slot
                                items(medicines) { medicine ->
                                    val isPast = safeTimeSlotIsPast(timeSlot)
                                    
                                    // Create unique key for this medicine and time slot
                                    val medicineKey = "${medicine.id}:$timeSlot"
                                    val localAction = localMedicineUpdates[medicineKey]
                                    
                                    MedicineItem(
                                        medicine = medicine,
                                        timeSlot = timeSlot,
                                        isPast = isPast,
                                        cardBackground = cardBackground,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        accentColor = accentColor,
                                        locallyTaken = localAction?.get("taken"),
                                        onTakeMedicine = handleMedicineAction
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineItem(
    medicine: Medicine, 
    timeSlot: String,
    isPast: Boolean,
    cardBackground: Color,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    locallyTaken: Boolean? = null,
    onTakeMedicine: (String, String, Boolean) -> Unit
) {
    // This checks if the medicine has a record for this time slot
    // isTakenToday() returns a boolean value indicating whether it was taken (true) or skipped (false)
    // If we have a local override value, use that instead
    val takenStatus = locallyTaken ?: medicine.isTakenToday(timeSlot)
    
    // We need to determine if:
    // 1. The medicine was taken (takenStatus == true)
    // 2. The medicine was skipped (takenStatus == false)
    // 3. The medicine has no record yet (needs buttons)
    
    val wasTaken = takenStatus == true
    val wasSkipped = takenStatus == false
    val needsAction = takenStatus == null && isPast
    
    // Track if this item has been actioned during this view
    val (actioned, setActioned) = remember { mutableStateOf(false) }
    
    val statusColor = when {
        wasTaken -> Color(0xFF30D158) // Green for taken
        wasSkipped -> Color(0xFFFF453A) // Red for explicitly skipped
        isPast -> Color(0xFFFFCC00) // Yellow for past but no action taken
        else -> accentColor // Blue for upcoming
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground.copy(alpha = if (isPast && !wasTaken) 0.9f else 1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        wasTaken -> Icons.Default.CheckCircle
                        wasSkipped -> Icons.Default.Warning
                        isPast -> Icons.Default.Notifications // Clock icon for past but no action
                        else -> Icons.Default.Info
                    },
                    contentDescription = "Medicine",
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = if (medicine.withFood) "Take with food" else "Take on empty stomach",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
                
                // Show status chip only if there's a record of action
                if (wasTaken || wasSkipped) {
                    AssistChip(
                        onClick = { },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2C2C2E),
                            labelColor = if (wasTaken) Color(0xFF30D158) else Color(0xFFFF453A)
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                        label = {
                            Text(if (wasTaken) "Taken" else "Missed")
                        }
                    )
                } else if (!isPast) {
                    // For future times, show days remaining
                    AssistChip(
                        onClick = { },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2C2C2E),
                            labelColor = secondaryTextColor
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                        label = {
                            Text("${medicine.daysRemaining()} days")
                        }
                    )
                }
            }
            
            // Only show action buttons if not yet actioned and needs action
            if (needsAction && !actioned) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { 
                            setActioned(true)
                            onTakeMedicine(medicine.id, timeSlot, false)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF453A)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF453A)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Skip")
                    }
                    
                    Button(
                        onClick = { 
                            setActioned(true)
                            onTakeMedicine(medicine.id, timeSlot, true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF30D158)
                        )
                    ) {
                        Text("Take")
                    }
                }
            }
        }
    }
}

// Helper function to format time from "HH:mm" to "h:mm a" format
private fun formatTimeSlot(timeSlot: String): String {
    val parts = timeSlot.split(":")
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when (hour) {
        0 -> 12
        in 13..23 -> hour - 12
        else -> hour
    }
    
    return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
}

private fun timeSlotIsPast(timeSlot: String): Boolean {
    try {
        val parts = timeSlot.split(":")
        if (parts.size != 2) {
            Log.e("MedicinesScreen", "Invalid time format: $timeSlot")
            return false
        }
        
        val hour = parts[0].toIntOrNull() ?: run {
            Log.e("MedicinesScreen", "Invalid hour in time: $timeSlot")
            return false
        }
        
        val minute = parts[1].toIntOrNull() ?: run {
            Log.e("MedicinesScreen", "Invalid minute in time: $timeSlot")
            return false
        }
        
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        
        return (hour < currentHour) || (hour == currentHour && minute <= currentMinute)
    } catch (e: Exception) {
        Log.e("MedicinesScreen", "Error comparing times: ${e.message}")
        return false
    }
}

// Helper function to safely check if time slot is past
private fun safeTimeSlotIsPast(timeSlot: String): Boolean {
    return try {
        timeSlotIsPast(timeSlot)
    } catch (e: Exception) {
        Log.e("MedicinesScreen", "Error checking if time slot is past: $timeSlot", e)
        false
    }
} 