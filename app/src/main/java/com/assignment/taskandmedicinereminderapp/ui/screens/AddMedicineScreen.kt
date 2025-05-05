package com.assignment.taskandmedicinereminderapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import com.assignment.taskandmedicinereminderapp.viewmodel.MedicineViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    navController: NavController,
    viewModel: MedicineViewModel = viewModel()
) {
    var medicineName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(1) }
    var withFood by remember { mutableStateOf(true) }
    var durationDays by remember { mutableStateOf(7) }
    
    // Track which time slot is being edited
    var showTimePicker by remember { mutableStateOf(false) }
    var currentEditingTimeIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Check if error state has changed
    LaunchedEffect(error) {
        if (error != null) {
            errorMessage = error
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
    
    // Dynamic time state based on frequency
    val timeSlots = remember(frequency) {
        List(frequency) { index ->
            // Default time values spread throughout the day
            val defaultHour = when (frequency) {
                1 -> 8 // 8 AM for once a day
                2 -> if (index == 0) 8 else 20 // 8 AM and 8 PM for twice a day
                3 -> if (index == 0) 8 else if (index == 1) 14 else 20 // 8 AM, 2 PM, 8 PM
                4 -> 8 + (index * 4) // Every 4 hours starting from 8 AM
                else -> 8 + (index * 2) // Every 2 hours starting from 8 AM for more than 4
            }
            mutableStateOf(String.format("%02d:00", defaultHour))
        }
    }
    
    // Time picker dialog
    if (showTimePicker && currentEditingTimeIndex < timeSlots.size) {
        val currentTimeString = timeSlots[currentEditingTimeIndex].value
        val timeParts = currentTimeString.split(":")
        val initialHour = if (timeParts.size > 1) timeParts[0].toIntOrNull() ?: 0 else 0
        val initialMinute = if (timeParts.size > 1) timeParts[1].toIntOrNull() ?: 0 else 0
        
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                timeSlots[currentEditingTimeIndex].value = String.format("%02d:%02d", hour, minute)
                showTimePicker = false
            },
            initialHour = initialHour,
            initialMinute = initialMinute
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Medicine", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                    // Save the medicine
                    if (medicineName.isNotBlank()) {
                        Log.d("AddMedicineScreen", "Adding medicine: $medicineName with times: ${timeSlots.map { it.value }}")
                        
                        // Log authentication status
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        if (auth.currentUser == null) {
                            Log.e("AddMedicineScreen", "Cannot add medicine - user not authenticated")
                            errorMessage = "User not authenticated. Please log in again."
                            return@FloatingActionButton
                        }
                        
                        // Check time slots format
                        val invalidTimeSlots = timeSlots.map { it.value }.filter { !it.matches(Regex("\\d{2}:\\d{2}")) }
                        if (invalidTimeSlots.isNotEmpty()) {
                            Log.e("AddMedicineScreen", "Invalid time format in slots: $invalidTimeSlots")
                            errorMessage = "Invalid time format. Please set all times correctly."
                            return@FloatingActionButton
                        }
                        
                        // Proceed with adding medicine
                        viewModel.addMedicine(
                            name = medicineName,
                            times = timeSlots.map { it.value },
                            withFood = withFood,
                            durationDays = durationDays
                        )
                        
                        // Only navigate back if no error occurred
                        if (error == null) {
                            navController.popBackStack()
                        }
                    } else {
                        // Show error for empty medicine name
                        Log.e("AddMedicineScreen", "Cannot add medicine: name is blank")
                        errorMessage = "Medicine name cannot be empty"
                    }
                },
                containerColor = successColor,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save"
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
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Error message
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = errorColor.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = errorColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = secondaryTextColor
                                )
                            }
                        }
                    }
                }
                
                // Loading indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Medicine Name
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("Medicine Name", color = secondaryTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = secondaryTextColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = accentColor
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Frequency selection
                Text(
                    text = "Frequency (times per day)",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = frequency.toFloat(),
                    onValueChange = { frequency = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = secondaryTextColor.copy(alpha = 0.3f)
                    )
                )
                Text(
                    text = "$frequency time(s) per day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Time pickers
                Text(
                    text = "Scheduled Times",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Dynamic time selection based on frequency
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        timeSlots.forEachIndexed { index, timeState ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = timeState.value,
                                    onValueChange = { /* Disable direct editing */ },
                                    readOnly = true,
                                    label = { Text("Time ${index + 1}", color = secondaryTextColor) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    placeholder = { Text("HH:MM", color = secondaryTextColor.copy(alpha = 0.7f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.5f),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        cursorColor = accentColor
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    currentEditingTimeIndex = index
                                    showTimePicker = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Select Time",
                                        tint = accentColor
                                    )
                                }
                            }
                            if (index < timeSlots.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // With food or empty stomach
                Text(
                    text = "Take medication",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        RadioButton(
                            selected = withFood,
                            onClick = { withFood = true },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = secondaryTextColor
                            )
                        )
                        Text("With food", color = textColor)
                        Spacer(modifier = Modifier.width(24.dp))
                        RadioButton(
                            selected = !withFood,
                            onClick = { withFood = false },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = secondaryTextColor
                            )
                        )
                        Text("Empty stomach", color = textColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Duration in days
                Text(
                    text = "Duration (days)",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = durationDays.toString(),
                    onValueChange = { 
                        val newValue = it.toIntOrNull() ?: 0
                        if (newValue > 0) {
                            durationDays = newValue
                        }
                    },
                    label = { Text("Number of days", color = secondaryTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = secondaryTextColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = accentColor
                    )
                )
                
                Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1C1E)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time picker with hour and minute sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker (0-23)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hour",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAEAEB2)
                        )
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        IconButton(onClick = {
                            selectedHour = (selectedHour + 1) % 24
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Increase hour",
                                tint = Color(0xFF0A84FF)
                            )
                        }
                        Slider(
                            value = selectedHour.toFloat(),
                            onValueChange = { selectedHour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.height(100.dp).width(120.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0A84FF),
                                activeTrackColor = Color(0xFF0A84FF),
                                inactiveTrackColor = Color(0xFFAEAEB2).copy(alpha = 0.3f)
                            )
                        )
                        IconButton(onClick = {
                            selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Decrease hour",
                                tint = Color(0xFF0A84FF)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Minute picker (0-59)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Minute",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAEAEB2)
                        )
                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        IconButton(onClick = {
                            selectedMinute = (selectedMinute + 1) % 60
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Increase minute",
                                tint = Color(0xFF0A84FF)
                            )
                        }
                        Slider(
                            value = selectedMinute.toFloat(),
                            onValueChange = { selectedMinute = it.toInt() },
                            valueRange = 0f..59f,
                            steps = 58,
                            modifier = Modifier.height(100.dp).width(120.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0A84FF),
                                activeTrackColor = Color(0xFF0A84FF),
                                inactiveTrackColor = Color(0xFFAEAEB2).copy(alpha = 0.3f)
                            )
                        )
                        IconButton(onClick = {
                            selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Decrease minute",
                                tint = Color(0xFF0A84FF)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFFF453A)
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    TextButton(
                        onClick = { onConfirm(selectedHour, selectedMinute) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF0A84FF)
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
} 