package com.assignment.taskandmedicinereminderapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assignment.taskandmedicinereminderapp.model.Task
import com.assignment.taskandmedicinereminderapp.repository.FirestoreRepository
import com.assignment.taskandmedicinereminderapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTask: () -> Unit,
    onNavigateToTaskDetail: (Task) -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val currentUser = (authState as? com.assignment.taskandmedicinereminderapp.viewmodel.AuthState.Authenticated)?.user
    val repository = FirestoreRepository()
    val scope = rememberCoroutineScope()
    
    // Task lists by quadrant
    var urgentImportantTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var notUrgentImportantTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var urgentNotImportantTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var notUrgentNotImportantTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedQuadrant by remember { mutableStateOf("UrgentImportant") }
    
    // Apple Watch-inspired dark theme colors
    val darkBackground = Color(0xFF000000)
    val cardBackground = Color(0xFF1C1C1E)
    val accentColor = Color(0xFF0A84FF)
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAEAEB2)
    
    // Priority colors
    val urgentImportantColor = Color(0xFFFF453A) // Red for "Urgent & Important"
    val urgentNotImportantColor = Color(0xFFFFCC00) // Yellow for "Urgent, Not Important"
    val notUrgentImportantColor = Color(0xFFFF9F0A) // Orange for "Not Urgent, Important"
    val notUrgentNotImportantColor = Color(0xFF0A84FF) // Blue for "Not Urgent, Not Important"
    
    // Load tasks and categorize them
    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                refreshTasks(repository, scope) { allTasks ->
                    val tasksByQuadrant = updateTaskLists(allTasks)
                    urgentImportantTasks = tasksByQuadrant["urgentImportant"] ?: emptyList()
                    notUrgentImportantTasks = tasksByQuadrant["notUrgentImportant"] ?: emptyList()
                    urgentNotImportantTasks = tasksByQuadrant["urgentNotImportant"] ?: emptyList()
                    notUrgentNotImportantTasks = tasksByQuadrant["notUrgentNotImportant"] ?: emptyList()
                    isLoading = false
                }
            }
        }
    }
    
    // Delete task dialog
    if (showDeleteDialog && selectedTask != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                selectedTask = null
            },
            title = { Text("Delete Task", color = textColor) },
            text = { Text("Are you sure you want to delete '${selectedTask?.title}'?", color = textColor) },
            containerColor = cardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val task = selectedTask
                                if (task != null && task.id.isNotBlank()) {
                                    Log.d("TasksScreen", "Attempting to delete task: ${task.id}")
                                    
                                    // Get the current user ID
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {
                                        // Delete the task directly from Firestore
                                        FirebaseFirestore.getInstance()
                                            .collection("users/$userId/tasks")
                                            .document(task.id)
                                            .delete()
                                            .await()
                                        
                                        Log.d("TasksScreen", "Task deleted successfully")
                                        
                                        // Manual update of UI state
                                        urgentImportantTasks = urgentImportantTasks.filter { it.id != task.id }
                                        notUrgentImportantTasks = notUrgentImportantTasks.filter { it.id != task.id }
                                        urgentNotImportantTasks = urgentNotImportantTasks.filter { it.id != task.id }
                                        notUrgentNotImportantTasks = notUrgentNotImportantTasks.filter { it.id != task.id }
                                        
                                        // Also refresh tasks from the repository
                                        refreshTasks(repository, scope) { allTasks ->
                                            val tasksByQuadrant = updateTaskLists(allTasks)
                                            urgentImportantTasks = tasksByQuadrant["urgentImportant"] ?: emptyList()
                                            notUrgentImportantTasks = tasksByQuadrant["notUrgentImportant"] ?: emptyList()
                                            urgentNotImportantTasks = tasksByQuadrant["urgentNotImportant"] ?: emptyList()
                                            notUrgentNotImportantTasks = tasksByQuadrant["notUrgentNotImportant"] ?: emptyList()
                                        }
                                    } else {
                                        Log.e("TasksScreen", "Error: User not logged in")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TasksScreen", "Error deleting task: ${e.message}", e)
                            }
                            
                            showDeleteDialog = false
                            selectedTask = null
                        }
                    }
                ) {
                    Text("Delete", color = urgentImportantColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedTask = null
                    }
                ) {
                    Text("Cancel", color = accentColor)
                }
            }
        )
    }
    
    // Move task dialog
    if (showMoveDialog && selectedTask != null) {
        // Initialize selectedQuadrant based on the current task's priority
        LaunchedEffect(selectedTask) {
            selectedQuadrant = when {
                selectedTask?.priority?.contains("Urgent") == true && 
                    selectedTask?.priority?.contains("Important") == true -> "UrgentImportant"
                selectedTask?.priority?.contains("Urgent") == false && 
                    selectedTask?.priority?.contains("Important") == true -> "NotUrgentImportant"
                selectedTask?.priority?.contains("Urgent") == true && 
                    selectedTask?.priority?.contains("Important") == false -> "UrgentNotImportant"
                else -> "NotUrgentNotImportant"
            }
            Log.d("TasksScreen", "Initial quadrant for task: $selectedQuadrant")
        }
        
        AlertDialog(
            onDismissRequest = { 
                showMoveDialog = false
                selectedTask = null
            },
            title = { Text("Move Task", color = textColor) },
            containerColor = cardBackground,
            text = { 
                Column {
                    Text("Select a quadrant to move '${selectedTask?.title}' to:", color = textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedQuadrant == "UrgentImportant",
                            onClick = { selectedQuadrant = "UrgentImportant" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = urgentImportantColor,
                                unselectedColor = secondaryTextColor
                            )
                        )
                        Text(
                            text = "Urgent & Important", 
                            modifier = Modifier.clickable { selectedQuadrant = "UrgentImportant" },
                            color = if (selectedQuadrant == "UrgentImportant") urgentImportantColor else textColor
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedQuadrant == "NotUrgentImportant",
                            onClick = { selectedQuadrant = "NotUrgentImportant" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = notUrgentImportantColor,
                                unselectedColor = secondaryTextColor
                            )
                        )
                        Text(
                            text = "Not Urgent, Important", 
                            modifier = Modifier.clickable { selectedQuadrant = "NotUrgentImportant" },
                            color = if (selectedQuadrant == "NotUrgentImportant") notUrgentImportantColor else textColor
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedQuadrant == "UrgentNotImportant",
                            onClick = { selectedQuadrant = "UrgentNotImportant" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = urgentNotImportantColor,
                                unselectedColor = secondaryTextColor
                            )
                        )
                        Text(
                            text = "Urgent, Not Important", 
                            modifier = Modifier.clickable { selectedQuadrant = "UrgentNotImportant" },
                            color = if (selectedQuadrant == "UrgentNotImportant") urgentNotImportantColor else textColor
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedQuadrant == "NotUrgentNotImportant",
                            onClick = { selectedQuadrant = "NotUrgentNotImportant" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = notUrgentNotImportantColor,
                                unselectedColor = secondaryTextColor
                            )
                        )
                        Text(
                            text = "Not Urgent, Not Important", 
                            modifier = Modifier.clickable { selectedQuadrant = "NotUrgentNotImportant" },
                            color = if (selectedQuadrant == "NotUrgentNotImportant") notUrgentNotImportantColor else textColor
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val task = selectedTask
                                if (task != null && task.id.isNotBlank()) {
                                    // Map quadrant to a priority string
                                    val newPriority = when (selectedQuadrant) {
                                        "UrgentImportant" -> "Urgent & Important"
                                        "NotUrgentImportant" -> "Not Urgent, Important"
                                        "UrgentNotImportant" -> "Urgent, Not Important"
                                        "NotUrgentNotImportant" -> "Not Urgent, Not Important"
                                        else -> task.priority
                                    }
                                    
                                    Log.d("TasksScreen", "Updating task ${task.id} priority to: $newPriority")
                                    
                                    // Only update if priority changed
                                    if (newPriority != task.priority) {
                                        // Get the current user ID
                                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                                        if (userId != null) {
                                            // Create updated task
                                            val updatedTask = task.copy(priority = newPriority)
                                            
                                            // Update task in Firestore
                                            FirebaseFirestore.getInstance()
                                                .collection("users/$userId/tasks")
                                                .document(task.id)
                                                .set(updatedTask)
                                                .await()
                                            
                                            Log.d("TasksScreen", "Task updated successfully")
                                            
                                            // Manual update of UI state - remove from current list
                                            when {
                                                urgentImportantTasks.any { it.id == task.id } -> 
                                                    urgentImportantTasks = urgentImportantTasks.filter { it.id != task.id }
                                                notUrgentImportantTasks.any { it.id == task.id } -> 
                                                    notUrgentImportantTasks = notUrgentImportantTasks.filter { it.id != task.id }
                                                urgentNotImportantTasks.any { it.id == task.id } -> 
                                                    urgentNotImportantTasks = urgentNotImportantTasks.filter { it.id != task.id }
                                                notUrgentNotImportantTasks.any { it.id == task.id } -> 
                                                    notUrgentNotImportantTasks = notUrgentNotImportantTasks.filter { it.id != task.id }
                                            }
                                            
                                            // Add to new list based on new priority
                                            when (newPriority) {
                                                "Urgent & Important" ->
                                                    urgentImportantTasks = urgentImportantTasks + updatedTask
                                                "Not Urgent, Important" ->
                                                    notUrgentImportantTasks = notUrgentImportantTasks + updatedTask
                                                "Urgent, Not Important" ->
                                                    urgentNotImportantTasks = urgentNotImportantTasks + updatedTask
                                                "Not Urgent, Not Important" ->
                                                    notUrgentNotImportantTasks = notUrgentNotImportantTasks + updatedTask
                                            }
                                            
                                            // Also refresh tasks from the repository
                                            refreshTasks(repository, scope) { allTasks ->
                                                val tasksByQuadrant = updateTaskLists(allTasks)
                                                urgentImportantTasks = tasksByQuadrant["urgentImportant"] ?: emptyList()
                                                notUrgentImportantTasks = tasksByQuadrant["notUrgentImportant"] ?: emptyList()
                                                urgentNotImportantTasks = tasksByQuadrant["urgentNotImportant"] ?: emptyList()
                                                notUrgentNotImportantTasks = tasksByQuadrant["notUrgentNotImportant"] ?: emptyList()
                                            }
                                        } else {
                                            Log.e("TasksScreen", "Error: User not logged in")
                                        }
                                    } else {
                                        Log.d("TasksScreen", "Priority unchanged, skipping update")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TasksScreen", "Error updating task: ${e.message}", e)
                            }
                            
                            showMoveDialog = false
                            selectedTask = null
                        }
                    }
                ) {
                    Text("Move", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMoveDialog = false
                        selectedTask = null
                    }
                ) {
                    Text("Cancel", color = secondaryTextColor)
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Tasks",
                        color = textColor
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                onClick = onNavigateToAddTask,
                containerColor = accentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = darkBackground
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(darkBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp)
                    .background(darkBackground)
            ) {
                Text(
                    text = "Eisenhower Matrix",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(8.dp),
                    color = textColor
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Urgent & Important
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        QuadrantHeader(
                            title = "Urgent & Important",
                            color = urgentImportantColor,
                            count = urgentImportantTasks.size
                        )
                        TaskList(
                            tasks = urgentImportantTasks,
                            onTaskClick = onNavigateToTaskDetail,
                            onTaskLongClick = { task ->
                                selectedTask = task
                                showDeleteDialog = true
                            },
                            onMoveClick = { task -> 
                                selectedTask = task
                                showMoveDialog = true
                            },
                            cardBackground = cardBackground,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor
                        )
                    }
                    
                    // Not Urgent & Important
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        QuadrantHeader(
                            title = "Not Urgent, Important",
                            color = notUrgentImportantColor,
                            count = notUrgentImportantTasks.size
                        )
                        TaskList(
                            tasks = notUrgentImportantTasks,
                            onTaskClick = onNavigateToTaskDetail,
                            onTaskLongClick = { task ->
                                selectedTask = task
                                showDeleteDialog = true
                            },
                            onMoveClick = { task -> 
                                selectedTask = task
                                showMoveDialog = true
                            },
                            cardBackground = cardBackground,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Urgent & Not Important
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        QuadrantHeader(
                            title = "Urgent, Not Important",
                            color = urgentNotImportantColor,
                            count = urgentNotImportantTasks.size
                        )
                        TaskList(
                            tasks = urgentNotImportantTasks,
                            onTaskClick = onNavigateToTaskDetail,
                            onTaskLongClick = { task ->
                                selectedTask = task
                                showDeleteDialog = true
                            },
                            onMoveClick = { task -> 
                                selectedTask = task
                                showMoveDialog = true
                            },
                            cardBackground = cardBackground,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor
                        )
                    }
                    
                    // Not Urgent & Not Important
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        QuadrantHeader(
                            title = "Not Urgent, Not Important",
                            color = notUrgentNotImportantColor,
                            count = notUrgentNotImportantTasks.size
                        )
                        TaskList(
                            tasks = notUrgentNotImportantTasks,
                            onTaskClick = onNavigateToTaskDetail,
                            onTaskLongClick = { task ->
                                selectedTask = task
                                showDeleteDialog = true
                            },
                            onMoveClick = { task -> 
                                selectedTask = task
                                showMoveDialog = true
                            },
                            cardBackground = cardBackground,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuadrantHeader(
    title: String,
    color: Color,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onMoveClick: (Task) -> Unit,
    cardBackground: Color = Color(0xFF1C1C1E),
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color(0xFFAEAEB2)
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 4.dp)
    ) {
        items(tasks) { task ->
            TaskItem(
                task = task,
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) },
                onMoveClick = { onMoveClick(task) },
                cardBackground = cardBackground,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveClick: () -> Unit,
    cardBackground: Color = Color(0xFF1C1C1E),
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color(0xFFAEAEB2)
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = task.deadline.toDate().let { dateFormatter.format(it) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = textColor
                )
                
                IconButton(
                    onClick = onMoveClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Move Task",
                        modifier = Modifier.size(16.dp),
                        tint = secondaryTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = secondaryTextColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Due: $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryTextColor
            )
        }
    }
}

private suspend fun refreshTasks(
    repository: FirestoreRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onTasksLoaded: (List<Task>) -> Unit
) {
    try {
        Log.d("TasksScreen", "Refreshing tasks...")
        val result = repository.getTasks()
        if (result.isSuccess) {
            val tasks = result.getOrNull() ?: emptyList()
            Log.d("TasksScreen", "Tasks refreshed successfully: ${tasks.size} tasks found")
            onTasksLoaded(tasks)
        } else {
            Log.e("TasksScreen", "Failed to refresh tasks: ${result.exceptionOrNull()?.message}")
        }
    } catch (e: Exception) {
        Log.e("TasksScreen", "Error refreshing tasks: ${e.message}", e)
    }
}

private fun updateTaskLists(
    allTasks: List<Task>
): Map<String, List<Task>> {
    // First, sort tasks by deadline and category
    val sortedTasks = allTasks.sortedWith(compareBy({ it.category }, { it.deadline.seconds }))
    
    // Categorize tasks based ONLY on the priority field
    val urgentImportant = sortedTasks.filter { task ->
        task.priority.equals("Urgent & Important", ignoreCase = true)
    }
    
    val notUrgentImportant = sortedTasks.filter { task ->
        task.priority.equals("Not Urgent, Important", ignoreCase = true)
    }
    
    val urgentNotImportant = sortedTasks.filter { task ->
        task.priority.equals("Urgent, Not Important", ignoreCase = true)
    }
    
    val notUrgentNotImportant = sortedTasks.filter { task ->
        task.priority.equals("Not Urgent, Not Important", ignoreCase = true)
    }
    
    // Log task distribution for debugging
    Log.d("TasksScreen", "Task distribution by priority:")
    Log.d("TasksScreen", "- Urgent & Important: ${urgentImportant.size} tasks")
    Log.d("TasksScreen", "- Not Urgent, Important: ${notUrgentImportant.size} tasks")
    Log.d("TasksScreen", "- Urgent, Not Important: ${urgentNotImportant.size} tasks")
    Log.d("TasksScreen", "- Not Urgent, Not Important: ${notUrgentNotImportant.size} tasks")
    
    return mapOf(
        "urgentImportant" to urgentImportant,
        "notUrgentImportant" to notUrgentImportant,
        "urgentNotImportant" to urgentNotImportant,
        "notUrgentNotImportant" to notUrgentNotImportant
    )
} 