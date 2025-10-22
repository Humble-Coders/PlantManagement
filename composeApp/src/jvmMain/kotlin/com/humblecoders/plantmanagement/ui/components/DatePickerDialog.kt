package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DatePickerDialog(
    title: String,
    initialDate: LocalDate? = null,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var showCalendar by remember { mutableStateOf(true) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(
                            Color(0xFF374151),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
                
                // Date display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected Date",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF9FAFB)
                            )
                        }
                        IconButton(
                            onClick = { showCalendar = !showCalendar },
                            modifier = Modifier.background(
                                Color(0xFF374151),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Icon(
                                if (showCalendar) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Calendar",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
                
                // Calendar
                if (showCalendar) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF111827),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Month/Year navigation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedDate = selectedDate.minusMonths(1)
                                    },
                                    modifier = Modifier.background(
                                        Color(0xFF374151),
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = Color(0xFF9CA3AF)
                                    )
                                }
                                
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF9FAFB)
                                )
                                
                                IconButton(
                                    onClick = {
                                        selectedDate = selectedDate.plusMonths(1)
                                    },
                                    modifier = Modifier.background(
                                        Color(0xFF374151),
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                            
                            // Days of week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                                    Text(
                                        text = day,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF6B7280),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            // Calendar days
                            val firstDayOfMonth = selectedDate.withDayOfMonth(1)
                            val lastDayOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
                            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
                            val daysInMonth = selectedDate.lengthOfMonth()
                            
                            // Generate calendar grid
                            val calendarDays = mutableListOf<LocalDate?>()
                            
                            // Add empty cells for days before the first day of the month
                            repeat(firstDayOfWeek) {
                                calendarDays.add(null)
                            }
                            
                            // Add days of the month
                            for (day in 1..daysInMonth) {
                                calendarDays.add(selectedDate.withDayOfMonth(day))
                            }
                            
                            // Create rows of 7 days each
                            calendarDays.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    week.forEach { date ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                        ) {
                                            if (date != null) {
                                                val isToday = date == LocalDate.now()
                                                val isSelected = date == selectedDate
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            when {
                                                                isSelected -> Color(0xFF10B981)
                                                                isToday -> Color(0xFF374151)
                                                                else -> Color.Transparent
                                                            },
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { selectedDate = date },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = date.dayOfMonth.toString(),
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> Color(0xFFF9FAFB)
                                                            else -> Color(0xFF9CA3AF)
                                                        }
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
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            onDateSelected(selectedDate)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DateRangePickerDialog(
    title: String,
    initialFromDate: LocalDate? = null,
    initialToDate: LocalDate? = null,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    var fromDate by remember { mutableStateOf(initialFromDate) }
    var toDate by remember { mutableStateOf(initialToDate) }
    var showFromCalendar by remember { mutableStateOf(false) }
    var showToCalendar by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(
                            Color(0xFF374151),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
                
                // From Date
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "From Date",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9CA3AF)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFromCalendar = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fromDate?.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) ?: "Select date",
                                fontSize = 16.sp,
                                color = if (fromDate != null) Color(0xFFF9FAFB) else Color(0xFF6B7280)
                            )
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select From Date",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
                
                // To Date
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "To Date",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9CA3AF)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showToCalendar = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = toDate?.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) ?: "Select date",
                                fontSize = 16.sp,
                                color = if (toDate != null) Color(0xFFF9FAFB) else Color(0xFF6B7280)
                            )
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select To Date",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            fromDate = null
                            toDate = null
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            onDateRangeSelected(fromDate, toDate)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Show date pickers
    if (showFromCalendar) {
        DatePickerDialog(
            title = "Select From Date",
            initialDate = fromDate,
            onDateSelected = { date ->
                fromDate = date
                showFromCalendar = false
            },
            onDismiss = { showFromCalendar = false }
        )
    }
    
    if (showToCalendar) {
        DatePickerDialog(
            title = "Select To Date",
            initialDate = toDate,
            onDateSelected = { date ->
                toDate = date
                showToCalendar = false
            },
            onDismiss = { showToCalendar = false }
        )
    }
}
