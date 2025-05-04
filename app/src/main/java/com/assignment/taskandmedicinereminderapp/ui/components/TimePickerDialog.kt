package com.assignment.taskandmedicinereminderapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    containerColor: Color = Color(0xFF1C1C1E),
    contentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var is24Hour by remember { mutableStateOf(true) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = is24Hour
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Time",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        TimePicker(
            state = timePickerState,
            colors = TimePickerDefaults.colors(
                timeSelectorSelectedContainerColor = Color(0xFF0A84FF),
                timeSelectorUnselectedContainerColor = Color(0xFF2C2C2E),
                timeSelectorSelectedContentColor = Color.White,
                timeSelectorUnselectedContentColor = Color.White,
                clockDialColor = Color(0xFF1C1C1E),
                clockDialSelectedContentColor = Color.White,
                clockDialUnselectedContentColor = Color(0xFFAEAEB2),
                periodSelectorBorderColor = Color(0xFF0A84FF),
                periodSelectorSelectedContainerColor = Color(0xFF0A84FF),
                periodSelectorUnselectedContainerColor = Color(0xFF2C2C2E),
                periodSelectorSelectedContentColor = Color.White,
                periodSelectorUnselectedContentColor = Color.White
            )
        )
        
        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
            hour = timePickerState.hour
            minute = timePickerState.minute
            onTimeChange(hour, minute)
        }
    }
} 