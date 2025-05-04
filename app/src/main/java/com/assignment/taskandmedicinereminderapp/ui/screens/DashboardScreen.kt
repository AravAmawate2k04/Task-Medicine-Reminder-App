package com.assignment.taskandmedicinereminderapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assignment.taskandmedicinereminderapp.model.Task
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToTasks: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
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
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
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
                
                // Placeholder for medicines
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Medications",
                            tint = Color(0xFF30D158), // Green in Apple's dark palette
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Medications",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        ElevatedAssistChip(
                            onClick = { },
                            label = { Text("Coming Soon") },
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = Color(0xFF2C2C2E),
                                labelColor = secondaryTextColor
                            )
                        )
                    }
                }
                
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