package com.assignment.taskandmedicinereminderapp.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.assignment.taskandmedicinereminderapp.model.Medicine
import com.assignment.taskandmedicinereminderapp.model.Task
import com.assignment.taskandmedicinereminderapp.navigation.Screen
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import com.assignment.taskandmedicinereminderapp.viewmodel.MedicineViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    medicineViewModel: MedicineViewModel = viewModel(),
    onNavigateToTasks: () -> Unit,
    onNavigateToAddTask: () -> Unit,
    onNavigateToMedicines: () -> Unit,
    onNavigateToAddMedicine: () -> Unit,
    onSignOut: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Authenticated)?.user
    val repository = FirestoreRepository()
    val scope = rememberCoroutineScope()
    
    // State for task statistics
    var totalTasks by remember { mutableStateOf(0) }
    var urgentImportantTasks by remember { mutableStateOf(0) }
    var urgentNotImportantTasks by remember { mutableStateOf(0) }
    var notUrgentImportantTasks by remember { mutableStateOf(0) }
    var notUrgentNotImportantTasks by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // State for medicine schedule
    var medicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var todayMedicineSchedules by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }
    var hasMedicines by remember { mutableStateOf(false) }
    var isMedicineLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Function to refresh all data
    val refreshAllData: () -> Unit = {
        scope.launch {
            Log.d("DashboardScreen", "Manually refreshing all data")
            refreshTrigger += 1
            medicineViewModel.fetchMedicines()
        }
    }
    
    // Ensure medicines are refreshed when dashboard is shown
    LaunchedEffect(key1 = currentUser, key2 = refreshTrigger) {
        if (currentUser != null) {
            Log.d("DashboardScreen", "Refreshing medicines from main dashboard")
            medicineViewModel.fetchMedicines()
        }
    }

    // Load task statistics
    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                val result = repository.getTasks()
                if (result.isSuccess) {
                    val tasks = result.getOrNull() ?: emptyList()
                    totalTasks = tasks.size
                    
                    // Count tasks based on Eisenhower quadrants
                    urgentImportantTasks = tasks.count { task ->
                        task.priority.equals("Urgent & Important", ignoreCase = true)
                    }
                    
                    urgentNotImportantTasks = tasks.count { task ->
                        task.priority.equals("Urgent, Not Important", ignoreCase = true)
                    }
                    
                    notUrgentImportantTasks = tasks.count { task ->
                        task.priority.equals("Not Urgent, Important", ignoreCase = true)
                    }
                    
                    notUrgentNotImportantTasks = tasks.count { task ->
                        task.priority.equals("Not Urgent, Not Important", ignoreCase = true)
                    }
                }
                isLoading = false
            }
        }
    }

    // Load medicine schedules for today
    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            scope.launch {
                isMedicineLoading = true
                val result = repository.getMedicines()
                if (result.isSuccess) {
                    val allMedicines = result.getOrNull() ?: emptyList()
                    // Filter out expired medicines
                    medicines = allMedicines.filter { it.isActive() }
                    hasMedicines = medicines.isNotEmpty()
                    
                    if (hasMedicines) {
                        // Extract all time slots for today and sort them
                        val allTimeSlots = mutableListOf<Pair<String, Boolean>>()
                        val currentTime = Calendar.getInstance()
                        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val currentTimeStr = dateFormat.format(currentTime.time)
                        
                        medicines.forEach { medicine ->
                            medicine.times.forEach { timeSlot ->
                                // Check if the time slot has passed for today
                                val hasTimePassed = timeSlot.compareTo(currentTimeStr) <= 0
                                allTimeSlots.add(Pair(timeSlot, hasTimePassed))
                            }
                        }
                        
                        // Sort time slots by time
                        todayMedicineSchedules = allTimeSlots.distinctBy { it.first }.sortedBy { it.first }
                    }
                }
                isMedicineLoading = false
            }
        }
    }

    // Apple Watch-inspired dark theme colors
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF1C1C1E)
    val accentColor = Color(0xFF0A84FF)
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAEAEB2)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Dashboard",
                        color = textColor
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground,
                    titleContentColor = textColor
                ),
                actions = {
                    // Add refresh button
                    IconButton(onClick = { refreshAllData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = accentColor
                        )
                    }
                    
                    IconButton(onClick = {
                        authViewModel.logout()
                        onSignOut()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = accentColor
                        )
                    }
                }
            )
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
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Apple Watch style circular progress for tasks
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(top = 16.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Only show the task chart if there are tasks
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(80.dp)
                        )
                    } else if (totalTasks > 0) {
                        // Calculate angles for pie chart
                        val urgentImportantAngle = if (totalTasks > 0) 360f * urgentImportantTasks / totalTasks else 0f
                        val urgentNotImportantAngle = if (totalTasks > 0) 360f * urgentNotImportantTasks / totalTasks else 0f
                        val notUrgentImportantAngle = if (totalTasks > 0) 360f * notUrgentImportantTasks / totalTasks else 0f
                        val notUrgentNotImportantAngle = if (totalTasks > 0) 360f * notUrgentNotImportantTasks / totalTasks else 0f
                        
                        Canvas(modifier = Modifier.size(200.dp)) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val radius = minOf(canvasWidth, canvasHeight) / 2
                            val center = Offset(canvasWidth / 2, canvasHeight / 2)
                            
                            // Draw outer circular track
                            drawCircle(
                                color = Color(0xFF2C2C2E),
                                radius = radius * 0.9f,
                                center = center
                            )
                            
                            val oval = Size(radius * 1.8f, radius * 1.8f)
                            var startAngle = -90f
                            
                            // Draw task segments
                            if (urgentImportantAngle > 0) {
                                drawArc(
                                    color = Color(0xFFFF453A), // Red for "Urgent & Important"
                                    startAngle = startAngle,
                                    sweepAngle = urgentImportantAngle,
                                    useCenter = true,
                                    topLeft = Offset(
                                        center.x - oval.width / 2,
                                        center.y - oval.height / 2
                                    ),
                                    size = oval
                                )
                                startAngle += urgentImportantAngle
                            }
                            
                            if (urgentNotImportantAngle > 0) {
                                drawArc(
                                    color = Color(0xFFFFCC00), // Yellow for "Urgent, Not Important"
                                    startAngle = startAngle,
                                    sweepAngle = urgentNotImportantAngle,
                                    useCenter = true,
                                    topLeft = Offset(
                                        center.x - oval.width / 2,
                                        center.y - oval.height / 2
                                    ),
                                    size = oval
                                )
                                startAngle += urgentNotImportantAngle
                            }
                            
                            if (notUrgentImportantAngle > 0) {
                                drawArc(
                                    color = Color(0xFFFF9F0A), // Orange for "Not Urgent, Important"
                                    startAngle = startAngle,
                                    sweepAngle = notUrgentImportantAngle,
                                    useCenter = true,
                                    topLeft = Offset(
                                        center.x - oval.width / 2,
                                        center.y - oval.height / 2
                                    ),
                                    size = oval
                                )
                                startAngle += notUrgentImportantAngle
                            }
                            
                            if (notUrgentNotImportantAngle > 0) {
                                drawArc(
                                    color = Color(0xFF0A84FF), // Blue for "Not Urgent, Not Important"
                                    startAngle = startAngle,
                                    sweepAngle = notUrgentNotImportantAngle,
                                    useCenter = true,
                                    topLeft = Offset(
                                        center.x - oval.width / 2,
                                        center.y - oval.height / 2
                                    ),
                                    size = oval
                                )
                            }
                            
                            // Draw center circle
                            drawCircle(
                                color = darkBackground,
                                radius = radius * 0.7f,
                                center = center
                            )
                        }
                        
                        // Center text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalTasks",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "Tasks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryTextColor
                            )
                        }
                    } else {
                        // No tasks view
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Tasks",
                                style = MaterialTheme.typography.headlineMedium,
                                color = secondaryTextColor,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateToTasks() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Add Task")
                            }
                        }
                    }
                }
                
                // Quick stats cards in Apple Watch style
                if (totalTasks > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { onNavigateToTasks() },
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Urgent & Important
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$urgentImportantTasks",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFFF453A), // Red for Urgent & Important
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "U & I",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor
                                )
                            }
                            
                            // Urgent, Not Important
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$urgentNotImportantTasks",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFFFCC00), // Yellow for Urgent, Not Important
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "U, NI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor
                                )
                            }
                            
                            // Not Urgent, Important
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$notUrgentImportantTasks",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFFF9F0A), // Orange for Not Urgent, Important
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NU, I",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor
                                )
                            }
                            
                            // Not Urgent, Not Important
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$notUrgentNotImportantTasks",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF0A84FF), // Blue for Not Urgent, Not Important
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NU, NI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                }
                
                // Navigation buttons in Apple Watch style
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { onNavigateToTasks() },
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tasks",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Navigate to Tasks",
                            tint = secondaryTextColor
                        )
                    }
                }
                
                // Medicine Schedule Card
                MedicineScheduleWidget(
                    navController = navController,
                    viewModel = medicineViewModel
                )
                
                // Health metrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sleep card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Sleep",
                                tint = Color(0xFFBF5AF2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sleep",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                            ElevatedAssistChip(
                                onClick = { },
                                label = { Text("Soon") },
                                colors = AssistChipDefaults.elevatedAssistChipColors(
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = secondaryTextColor
                                )
                            )
                        }
                    }
                    
                    // Activity card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Activity",
                                tint = Color(0xFFFF2D55), // Pink in Apple's dark palette
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Activity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                            ElevatedAssistChip(
                                onClick = { },
                                label = { Text("Soon") },
                                colors = AssistChipDefaults.elevatedAssistChipColors(
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = secondaryTextColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = color, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
fun PlaceholderCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun MedicineScheduleWidget(
    navController: NavController,
    viewModel: MedicineViewModel = viewModel()
) {
    var errorState by remember { mutableStateOf<String?>(null) }
    
    val medicines by viewModel.medicines.collectAsState()
    val todaySchedule = viewModel.getTodaySchedule()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Force refresh medicines when widget is displayed
    LaunchedEffect(key1 = Unit) {
        Log.d("MedicineScheduleWidget", "Refreshing medicines from widget")
        try {
            viewModel.fetchMedicines()
        } catch (e: Exception) {
            Log.e("MedicineScheduleWidget", "Error refreshing medicines: ${e.message}", e)
            errorState = "Error refreshing data"
        }
    }

    // Apple Watch-inspired dark theme colors
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF1C1C1E)
    val accentColor = Color(0xFF0A84FF)
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAEAEB2)
    val errorColor = Color(0xFFFF453A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable {
                try {
                    navController.navigate(Screen.Medicines.route)
                } catch (e: Exception) {
                    Log.e("MedicineScheduleWidget", "Navigation error: ${e.message}", e)
                    errorState = "Navigation error"
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Medicine",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Medicine Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Schedule",
                    tint = secondaryTextColor
                )
            }
            
            if (errorState != null || error != null) {
                Text(
                    text = errorState ?: error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = errorColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { 
                        errorState = null
                        viewModel.fetchMedicines() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Retry")
                }
            } else if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading medicines...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (medicines.isEmpty()) {
                // No medicines at all
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No medicines added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            try {
                                navController.navigate(Screen.AddMedicine.route)
                            } catch (e: Exception) {
                                Log.e("MedicineScheduleWidget", "Navigation error: ${e.message}", e)
                                errorState = "Navigation error"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Add Medicine")
                    }
                }
            } else if (todaySchedule.isEmpty()) {
                // Medicines exist but no time slots for today
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No medicine schedules active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show some medicine details
                    Text(
                        text = "${medicines.size} medicine(s) in your collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                    
                    // Show first medicine
                    if (medicines.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = cardBackground.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = medicines[0].name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (medicines[0].times.isNotEmpty()) {
                                    Text(
                                        text = "Times: ${medicines[0].times.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Show the medicine schedule
                val upcomingSlots = todaySchedule.filter { !it.second }
                val pastSlots = todaySchedule.filter { it.second }
                
                // Sort all slots by time
                val allSortedSlots = todaySchedule.sortedBy { it.first }
                
                // Debug - Display today's date according to the app
                val todayString = Medicine.formatDate(Calendar.getInstance().time)
                val currentTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
                Log.d("DashboardScreen", "Today's date according to app: $todayString, current time: $currentTimeFormatted")
                
                if (allSortedSlots.isNotEmpty()) {
                    // Use a horizontally scrollable row to show all time slots
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Today's Schedule ($todayString, Now: $currentTimeFormatted)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Horizontal scrollable row for all time slots
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            allSortedSlots.forEach { (time, isPast) ->
                                MedicineTimeIndicator(
                                    time = formatTimeSlot(time),
                                    isPast = isPast,
                                    textColor = secondaryTextColor
                                )
                            }
                        }
                    }
                    
                    // Show upcoming medicine reminders
                    if (upcomingSlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Next dose: ${formatTimeSlot(upcomingSlots.first().first)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // This should never happen since we check todaySchedule.isEmpty() above
                    Text(
                        text = "No medicine times available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MedicineTimeIndicator(
    time: String, 
    isPast: Boolean,
    textColor: Color
) {
    // Add more debug information
    Log.d("MedicineTimeIndicator", "Rendering time: $time, isPast: $isPast")
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp) // Fixed width to ensure consistent spacing
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isPast) Color(0xFF0A84FF) // Blue for passed times
                           else Color(0xFF30D158), // Green for upcoming times
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Add a small marker inside for better visibility
            if (isPast) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Time label with different colors for past vs future
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                fontWeight = if (!isPast) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isPast) textColor else Color(0xFF30D158), // Green text for future times
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        
        // Add a label to indicate past/future
        Text(
            text = if (isPast) "Past" else "Soon",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 8.sp
            ),
            color = if (isPast) Color(0xFF0A84FF) else Color(0xFF30D158),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Helper function to format time from "HH:mm" to "h:mm a" format
private fun formatTimeSlot(timeSlot: String): String {
    try {
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
    } catch (e: Exception) {
        Log.e("MedicineTimeFormat", "Error formatting time: $timeSlot", e)
        return timeSlot
    }
} 