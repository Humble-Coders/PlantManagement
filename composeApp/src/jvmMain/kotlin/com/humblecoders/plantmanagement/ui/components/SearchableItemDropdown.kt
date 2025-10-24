package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.InventoryItem

@Composable
fun SearchableItemDropdown(
    items: List<InventoryItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select Item",
    label: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Filter items based on search query
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.categoryType.name.contains(searchQuery, ignoreCase = true) ||
                item.unit.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val selectedItem = items.find { it.id == selectedItemId }
    
    // Determine what to display in the text field
    val displayValue = remember(selectedItem, searchQuery, isSearching) {
        when {
            isSearching -> searchQuery
            selectedItem != null -> "${selectedItem.name} (${selectedItem.unit})"
            else -> ""
        }
    }
    
    Column(modifier = modifier) {
        // Label
        if (label != null) {
            Text(
                text = label,
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        // Searchable Text Field
        OutlinedTextField(
            value = displayValue,
            onValueChange = { newValue ->
                searchQuery = newValue
                isSearching = true
                expanded = true
            },
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color(0xFFF9FAFB),
                backgroundColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFF06B6D4),
                unfocusedBorderColor = Color(0xFF374151),
                cursorColor = Color(0xFF06B6D4),
                disabledTextColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isSearching && searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                searchQuery = ""
                                isSearching = false
                                expanded = false
                                keyboardController?.hide()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { 
                            if (isSearching) {
                                isSearching = false
                                expanded = false
                                keyboardController?.hide()
                            } else {
                                expanded = !expanded
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    isSearching = false
                    expanded = false
                    keyboardController?.hide()
                }
            ),
            singleLine = true
        )
        
        // Custom Dropdown (positioned below the text field)
        if (expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 200.dp, max = 300.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(top = 4.dp),
                backgroundColor = Color.White,
                shape = RoundedCornerShape(8.dp),
                elevation = 8.dp
            ) {
                // Item List
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No items available" else "No items found",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        filteredItems.forEach { item ->
                            ItemDropdownItem(
                                item = item,
                                isSelected = item.id == selectedItemId,
                                onClick = {
                                    onItemSelected(item.id)
                                    expanded = false
                                    isSearching = false
                                    searchQuery = ""
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDropdownItem(
    item: InventoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.name,
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unit: ${item.unit}",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Text(
                    text = "•",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
                
                Text(
                    text = "Qty: ${item.quantity}",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            if (item.categoryType != com.humblecoders.plantmanagement.data.CategoryType.RAW_MATERIAL) {
                Text(
                    text = item.categoryType.name.replace("_", " "),
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
