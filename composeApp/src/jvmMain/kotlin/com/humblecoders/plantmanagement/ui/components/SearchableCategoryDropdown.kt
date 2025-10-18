package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.data.CashReportType

@Composable
fun SearchableCategoryDropdown(
    selectedCategory: CashReportCategory?,
    categories: List<CashReportCategory>,
    onCategorySelected: (CashReportCategory) -> Unit,
    onAddNewCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddNewDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
                // Main dropdown button
                OutlinedTextField(
                    value = if (isSearching) searchQuery else (selectedCategory?.name ?: ""),
                    onValueChange = { newValue ->
                        isSearching = true
                        searchQuery = newValue
                        showDropdown = true
                        
                        // If user cleared the field, clear selection
                        if (newValue.isEmpty()) {
                            onCategorySelected(CashReportCategory())
                            isSearching = false
                        }
                    },
            label = { 
                Text(
                    "Category", 
                    color = Color(0xFF9CA3AF)
                ) 
            },
            trailingIcon = {
                IconButton(onClick = { 
                    showDropdown = !showDropdown
                    if (!showDropdown) {
                        isSearching = false
                        searchQuery = ""
                    }
                }) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { 
                    showDropdown = false
                    isSearching = false
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color(0xFFF9FAFB),
                cursorColor = Color(0xFF10B981),
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF4B5563),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedLabelColor = Color(0xFF9CA3AF)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Dropdown menu
        if (showDropdown) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(8.dp),
                elevation = 8.dp
            ) {
                Column {
                    // Search field in dropdown
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search categories", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            cursorColor = Color(0xFF10B981),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color(0xFF9CA3AF)
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    )

                    // Categories list
                    val filteredCategories = categories.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) 
                    }

                    if (filteredCategories.isEmpty() && searchQuery.isNotBlank()) {
                        // Show "Add new category" option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    newCategoryName = searchQuery
                                    showAddNewDialog = true
                                    showDropdown = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add new",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Add '$searchQuery' as new category",
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(filteredCategories) { category ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onCategorySelected(category)
                                                    showDropdown = false
                                                    isSearching = false
                                                    searchQuery = ""
                                                }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = "Category",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = category.name,
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add new category dialog
    if (showAddNewDialog) {
        Dialog(onDismissRequest = { showAddNewDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Add New Category",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Create a new category that can be used for both Cash In and Cash Out transactions",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            cursorColor = Color(0xFF10B981),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color(0xFF9CA3AF)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddNewDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF9CA3AF)
                            )
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    onAddNewCategory(newCategoryName)
                                    // Create a temporary category object for the new category
                                    onCategorySelected(CashReportCategory(name = newCategoryName))
                                    showAddNewDialog = false
                                }
                            },
                            enabled = newCategoryName.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Add Category")
                        }
                    }
                }
            }
        }
    }
}
