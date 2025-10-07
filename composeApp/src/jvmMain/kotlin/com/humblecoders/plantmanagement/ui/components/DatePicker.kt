package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun DatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Date",
    enabled: Boolean = true,
    colors: DatePickerColors = DatePickerDefaults.colors()
) {
    var showCalendar by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Date Display Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { 
                    if (enabled) {
                        showCalendar = true 
                    }
                }
        ) {
            OutlinedTextField(
                value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                onValueChange = { },
                label = { Text(label, color = colors.labelColor) },
                readOnly = true,
                enabled = false, // Disable the text field itself to avoid focus issues
                trailingIcon = {
                    Box(
                        modifier = Modifier.clickable { showCalendar = true }
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = colors.iconColor
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = colors.textColor,
                    backgroundColor = colors.backgroundColor,
                    focusedBorderColor = colors.focusedBorderColor,
                    unfocusedBorderColor = colors.unfocusedBorderColor,
                    disabledTextColor = colors.textColor,
                    disabledBorderColor = colors.unfocusedBorderColor,
                    disabledLabelColor = colors.labelColor,
                    disabledTrailingIconColor = colors.iconColor
                ),
                singleLine = true
            )
        }
        
        // Calendar Dropdown - Use Popup for proper overlay
        if (showCalendar) {
            val density = LocalDensity.current
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = with(density) { IntOffset(0, (-300).dp.roundToPx()) }, // Position above the field
                onDismissRequest = { showCalendar = false }
            ) {
                CalendarDropdown(
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        onDateSelected(date)
                        showCalendar = false
                    },
                    onDismiss = { showCalendar = false },
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun CalendarDropdown(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    colors: DatePickerColors
) {
    var currentMonth by remember { mutableStateOf(selectedDate) }
    
    Card(
        modifier = Modifier
            .width(280.dp) // Fixed width instead of fillMaxWidth
            .padding(top = 4.dp),
        backgroundColor = colors.calendarBackgroundColor,
        shape = RoundedCornerShape(8.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp), // Reduced padding
            verticalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
        ) {
            // Month/Year Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        currentMonth = currentMonth.minusMonths(1)
                    }
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous Month",
                        tint = colors.iconColor
                    )
                }
                
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${currentMonth.year}",
                    color = colors.headerTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                IconButton(
                    onClick = { 
                        currentMonth = currentMonth.plusMonths(1)
                    }
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next Month",
                        tint = colors.iconColor
                    )
                }
            }
            
            // Day Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        color = colors.dayHeaderColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Calendar Days
            val firstDayOfMonth = currentMonth.withDayOfMonth(1)
            val lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
            val startDate = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value.toLong() % 7)
            
            for (week in 0..5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (day in 0..6) {
                        val date = startDate.plusDays((week * 7 + day).toLong())
                        val isCurrentMonth = date.month == currentMonth.month
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(32.dp) // Fixed size instead of aspectRatio
                                .padding(1.dp)
                                .background(
                                    color = when {
                                        isSelected -> colors.selectedDayBackgroundColor
                                        isToday -> colors.todayBackgroundColor
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable(enabled = isCurrentMonth) {
                                    if (isCurrentMonth) {
                                        onDateSelected(date)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = when {
                                    !isCurrentMonth -> colors.otherMonthTextColor
                                    isSelected -> colors.selectedDayTextColor
                                    isToday -> colors.todayTextColor
                                    else -> colors.dayTextColor
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            
            // Today Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onDateSelected(LocalDate.now()) }
                ) {
                    Text(
                        "Today",
                        color = colors.todayButtonColor,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

data class DatePickerColors(
    val textColor: Color = Color(0xFFF9FAFB),
    val backgroundColor: Color = Color(0xFF111827),
    val focusedBorderColor: Color = Color(0xFF06B6D4),
    val unfocusedBorderColor: Color = Color(0xFF374151),
    val disabledTextColor: Color = Color(0xFF9CA3AF),
    val disabledBorderColor: Color = Color(0xFF374151),
    val labelColor: Color = Color(0xFF9CA3AF),
    val iconColor: Color = Color(0xFF9CA3AF),
    val calendarBackgroundColor: Color = Color(0xFF1F2937),
    val headerTextColor: Color = Color(0xFFF9FAFB),
    val dayHeaderColor: Color = Color(0xFF9CA3AF),
    val dayTextColor: Color = Color(0xFFF9FAFB),
    val selectedDayBackgroundColor: Color = Color(0xFF06B6D4),
    val selectedDayTextColor: Color = Color(0xFF111827),
    val todayBackgroundColor: Color = Color(0xFF10B981),
    val todayTextColor: Color = Color(0xFF111827),
    val otherMonthTextColor: Color = Color(0xFF6B7280),
    val todayButtonColor: Color = Color(0xFF06B6D4)
)

object DatePickerDefaults {
    fun colors(): DatePickerColors = DatePickerColors()
}
