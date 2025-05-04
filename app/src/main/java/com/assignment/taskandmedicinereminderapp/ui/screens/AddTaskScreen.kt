package com.assignment.taskandmedicinereminderapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assignment.taskandmedicinereminderapp.model.Task
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val currentUser = (authState as? com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Authenticated)?.user
    val repository = FirestoreRepository()
    val scope = rememberCoroutineScope()
    
    // Form state
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var importance by remember { mutableStateOf(0.0f) } // 0.0f to 1.0f, 4 steps (0, 0.33, 0.66, 1.0)
    var selectedCategory by remember { mutableStateOf("Work") }
    var enableReminders by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var reminderMinutesBefore by remember { mutableStateOf(30) } // Default 30 minutes before
    var reminderOptions = listOf(15, 30, 60, 120) // 15 min, 30 min, 1 hour, 2 hours
    
    // Apple Watch-inspired dark theme colors
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF1C1C1E)
    val accentColor = Color(0xFF0A84FF) // Blue in Apple's dark palette
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAEAEB2)
    val errorColor = Color(0xFFFF453A) // Red in Apple's dark palette
    
    val categories = listOf("Work", "Personal", "Health", "Finance", "Education", "Home", "Other")
    
    val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Task", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = accentColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (validateForm(taskTitle, taskDescription)) {
                                isSubmitting = true
                                scope.launch {
                                    // Determine priority based on importance slider
                                    val priority = when {
                                        importance >= 0.75f -> "Urgent & Important"
                                        importance >= 0.5f -> "Urgent, Not Important"
                                        importance >= 0.25f -> "Not Urgent, Important"
                                        else -> "Not Urgent, Not Important"
                                    }
                                    
                                    val task = Task(
                                        title = taskTitle,
                                        description = taskDescription,
                                        deadline = Timestamp(deadlineDate),
                                        priority = priority,
                                        isRecurring = false,
                                        recurrencePattern = "",
                                        createdAt = Timestamp.now(),
                                        completed = false,
                                        category = selectedCategory,
                                        reminderTime = if (enableReminders) {
                                            val calendar = Calendar.getInstance().apply {
                                                time = deadlineDate
                                                add(Calendar.MINUTE, -reminderMinutesBefore)
                                            }
                                            Timestamp(calendar.time)
                                        } else null
                                    )
                                    
                                    val result = repository.addTask(task)
                                    isSubmitting = false
                                    
                                    if (result.isSuccess) {
                                        onNavigateBack()
                                    } else {
                                        snackbarMessage = "Failed to add task: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                        showSnackbar = true
                                    }
                                }
                            } else {
                                snackbarMessage = "Please fill all required fields"
                                showSnackbar = true
                            }
                        },
                        enabled = !isSubmitting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Task",
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
        containerColor = darkBackground,
        snackbarHost = {
            SnackbarHost(hostState = remember { SnackbarHostState() }) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = cardBackground,
                    contentColor = textColor,
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("Dismiss", color = accentColor)
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Title", color = if (taskTitle.isBlank() && showSnackbar) errorColor else secondaryTextColor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                isError = taskTitle.isBlank() && showSnackbar,
                supportingText = {
                    if (taskTitle.isBlank() && showSnackbar) {
                        Text("Title is required", color = errorColor)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = accentColor,
                    errorBorderColor = errorColor,
                    errorCursorColor = errorColor,
                    errorTextColor = errorColor,
                    errorContainerColor = darkBackground
                )
            )
            
            // Task Description
            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Description", color = if (taskDescription.isBlank() && showSnackbar) errorColor else secondaryTextColor) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                isError = taskDescription.isBlank() && showSnackbar,
                supportingText = {
                    if (taskDescription.isBlank() && showSnackbar) {
                        Text("Description is required", color = errorColor)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = accentColor,
                    errorBorderColor = errorColor,
                    errorCursorColor = errorColor,
                    errorTextColor = errorColor,
                    errorContainerColor = darkBackground
                )
            )
            
            // Deadline
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Deadline",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Check if deadline is in the past
                    val now = Calendar.getInstance().time
                    val isDeadlineInPast = deadlineDate.before(now)
                    
                    // Date row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = if (isDeadlineInPast) errorColor else accentColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        Text(
                            text = dateFormatter.format(deadlineDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDeadlineInPast) errorColor else textColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Change", color = accentColor)
                        }
                    }
                    
                    // Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Time",
                            tint = if (isDeadlineInPast) errorColor else accentColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Text(
                            text = timeFormatter.format(deadlineDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDeadlineInPast) errorColor else textColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("Change", color = accentColor)
                        }
                    }
                    
                    // Show warning if deadline is in the past
                    if (isDeadlineInPast) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Warning: Deadline cannot be in the past",
                            style = MaterialTheme.typography.bodySmall,
                            color = errorColor
                        )
                    }
                }
            }
            
            // Importance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Task Importance",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val importanceColor = when {
                        importance >= 0.75f -> Color(0xFFFF453A) // Red for "Urgent & Important" (highest)
                        importance >= 0.5f -> Color(0xFFFF9F0A) // Orange for "Not Urgent, Important"
                        importance >= 0.25f -> Color(0xFFFFCC00) // Yellow for "Urgent, Not Important"
                        else -> Color(0xFF0A84FF) // Blue for "Not Urgent, Not Important" (lowest)
                    }
                    
                    // Display current priority text based on slider position
                    Text(
                        text = when {
                            importance >= 0.75f -> "Urgent & Important"
                            importance >= 0.5f -> "Urgent, Not Important"
                            importance >= 0.25f -> "Not Urgent, Important"
                            else -> "Not Urgent, Not Important"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = importanceColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Slider(
                        value = importance,
                        onValueChange = { importance = it },
                        steps = 2, // 4 positions (0, 0.33, 0.66, 1.0) means 3 steps between, but we need to subtract 1 for API
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = importanceColor,
                            activeTrackColor = importanceColor,
                            inactiveTrackColor = importanceColor.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            
            // Category
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = { selectedCategory = it },
                label = { Text("Category", color = secondaryTextColor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = accentColor,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = secondaryTextColor
                )
            )
            
            // Reminders
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enable Reminders",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                        Switch(
                            checked = enableReminders,
                            onCheckedChange = { enableReminders = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor,
                                checkedBorderColor = accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.DarkGray,
                                uncheckedBorderColor = Color.DarkGray
                            )
                        )
                    }
                    
                    if (enableReminders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Remind me before:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Reminder time options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderOptions.forEach { minutes ->
                                val text = when {
                                    minutes >= 60 -> "${minutes / 60} hour${if (minutes > 60) "s" else ""}"
                                    else -> "$minutes min"
                                }
                                
                                FilterChip(
                                    selected = reminderMinutesBefore == minutes,
                                    onClick = { reminderMinutesBefore = minutes },
                                    label = { Text(text) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = cardBackground,
                                        labelColor = textColor,
                                        selectedContainerColor = accentColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        
                        // Add a message to validate that reminder time is before deadline
                        val reminderTime = Calendar.getInstance().apply { 
                            time = deadlineDate
                            add(Calendar.MINUTE, -reminderMinutesBefore)
                        }.time
                        
                        val now = Calendar.getInstance().time
                        
                        if (reminderTime.before(now)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Warning: Reminder time is in the past",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF453A)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // Check if deadline is in the past
                    val now = Calendar.getInstance().time
                    val isDeadlineInPast = deadlineDate.before(now)
                    
                    if (isDeadlineInPast) {
                        snackbarMessage = "Cannot create task with deadline in the past"
                        showSnackbar = true
                        return@Button
                    }
                    
                    if (validateForm(taskTitle, taskDescription)) {
                        isSubmitting = true
                        scope.launch {
                            // Determine priority based on importance slider
                            val priority = when {
                                importance >= 0.75f -> "Urgent & Important"
                                importance >= 0.5f -> "Urgent, Not Important"
                                importance >= 0.25f -> "Not Urgent, Important"
                                else -> "Not Urgent, Not Important"
                            }
                            
                            val task = Task(
                                title = taskTitle,
                                description = taskDescription,
                                deadline = Timestamp(deadlineDate),
                                priority = priority,
                                isRecurring = false,
                                recurrencePattern = "",
                                createdAt = Timestamp.now(),
                                completed = false,
                                category = selectedCategory,
                                reminderTime = if (enableReminders) {
                                    val calendar = Calendar.getInstance().apply {
                                        time = deadlineDate
                                        add(Calendar.MINUTE, -reminderMinutesBefore)
                                    }
                                    Timestamp(calendar.time)
                                } else null
                            )
                            
                            val result = repository.addTask(task)
                            isSubmitting = false
                            
                            if (result.isSuccess) {
                                onNavigateBack()
                            } else {
                                snackbarMessage = "Failed to add task: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                showSnackbar = true
                            }
                        }
                    } else {
                        snackbarMessage = "Please fill all required fields"
                        showSnackbar = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White,
                    disabledContainerColor = accentColor.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Task")
                }
            }
        }
        
        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = deadlineDate.time
            )
            
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = millis
                                deadlineDate = calendar.time
                            }
                            showDatePicker = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = accentColor
                        )
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = secondaryTextColor
                        )
                    ) {
                        Text("Cancel")
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = cardBackground,
                    titleContentColor = textColor,
                    headlineContentColor = textColor,
                    weekdayContentColor = secondaryTextColor,
                    subheadContentColor = secondaryTextColor,
                    yearContentColor = textColor,
                    currentYearContentColor = accentColor,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = accentColor,
                    dayContentColor = textColor,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = accentColor,
                    todayContentColor = accentColor,
                    todayDateBorderColor = accentColor
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = cardBackground,
                        titleContentColor = textColor,
                        headlineContentColor = textColor,
                        weekdayContentColor = secondaryTextColor,
                        subheadContentColor = secondaryTextColor,
                        yearContentColor = textColor,
                        currentYearContentColor = accentColor,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = accentColor,
                        dayContentColor = textColor,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = accentColor,
                        todayContentColor = accentColor,
                        todayDateBorderColor = accentColor
                    )
                )
            }
        }
        
        // Time Picker Dialog
        if (showTimePicker) {
            val calendar = Calendar.getInstance().apply { time = deadlineDate }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            
            val timePickerState = rememberTimePickerState(
                initialHour = hour,
                initialMinute = minute
            )
            
            Dialog(
                onDismissRequest = { showTimePicker = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBackground,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Select Time",
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColor
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        TimePicker(state = timePickerState)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showTimePicker = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = secondaryTextColor
                                )
                            ) {
                                Text("Cancel")
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            TextButton(
                                onClick = {
                                    val newDate = Calendar.getInstance().apply {
                                        time = deadlineDate
                                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        set(Calendar.MINUTE, timePickerState.minute)
                                    }.time
                                    deadlineDate = newDate
                                    showTimePicker = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = accentColor
                                )
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
        
        // Show Snackbar if needed
        LaunchedEffect(showSnackbar) {
            if (showSnackbar) {
                // Auto-hide snackbar after 3 seconds
                launch {
                    kotlinx.coroutines.delay(3000)
                    showSnackbar = false
                }
            }
        }
    }
}

private fun validateForm(title: String, description: String): Boolean {
    return title.isNotBlank() && description.isNotBlank()
} 