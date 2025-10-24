package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.Note
import com.humblecoders.plantmanagement.data.UserRole
import com.humblecoders.plantmanagement.viewmodels.NoteViewModel
import java.time.format.DateTimeFormatter

@Composable
fun NotesScreen(
    noteViewModel: NoteViewModel,
    userRole: UserRole? = null
) {
    val noteState = noteViewModel.noteState
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    
    val isAdmin = userRole == UserRole.ADMIN

    // Clear messages after showing
    LaunchedEffect(noteState.successMessage, noteState.error) {
        if (noteState.successMessage != null || noteState.error != null) {
            kotlinx.coroutines.delay(3000)
            noteViewModel.clearSuccessMessage()
            noteViewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
                
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Note", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = noteState.searchQuery,
                onValueChange = { query ->
                    noteViewModel.searchNotes(query)
                },
                label = { Text("Search notes...", color = Color(0xFF9CA3AF)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                },
                trailingIcon = {
                    if (noteState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { noteViewModel.searchNotes("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF9CA3AF))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF374151),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color(0xFF9CA3AF),
                    cursorColor = Color(0xFF10B981),
                    textColor = Color(0xFFF9FAFB)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Messages
            noteState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFDC2626),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            noteState.successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF10B981),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading indicator
            if (noteState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else {
                // Notes List
                if (noteState.notes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF374151),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Note,
                                contentDescription = "No Notes",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (noteState.searchQuery.isNotEmpty()) "No notes found matching your search" else "No notes yet",
                                color = Color(0xFF9CA3AF),
                                fontSize = 16.sp
                            )
                            if (noteState.searchQuery.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Click 'Add Note' to create your first note",
                                    color = Color(0xFF6B7280),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(noteState.notes) { note ->
                            NoteCard(
                                note = note,
                                isAdmin = isAdmin,
                                onEdit = { noteToEdit = note; showEditDialog = true },
                                onDelete = { noteToDelete = note; showDeleteConfirmDialog = true },
                                onToggleCompletion = { isCompleted ->
                                    noteViewModel.toggleNoteCompletion(note.id, isCompleted)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Note Dialog
    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onAddNote = { title, description, date ->
                noteViewModel.addNote(title, description, date)
                showAddDialog = false
            },
            isLoading = noteState.isAdding
        )
    }

    // Edit Note Dialog
    if (showEditDialog && noteToEdit != null) {
        EditNoteDialog(
            note = noteToEdit!!,
            onDismiss = { showEditDialog = false; noteToEdit = null },
            onUpdateNote = { note ->
                noteViewModel.updateNote(note)
                showEditDialog = false
                noteToEdit = null
            },
            isLoading = noteState.isUpdating
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false; noteToDelete = null },
            title = { Text("Delete Note", color = Color(0xFFF9FAFB)) },
            text = { Text("Are you sure you want to delete this note?", color = Color(0xFF9CA3AF)) },
            confirmButton = {
                Button(
                    onClick = {
                        noteViewModel.deleteNote(noteToDelete!!.id)
                        showDeleteConfirmDialog = false
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false; noteToDelete = null }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            },
            backgroundColor = Color(0xFF1F2937)
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleCompletion: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.title,
                            color = if (note.isCompleted) Color(0xFF9CA3AF) else Color(0xFFF9FAFB),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (note.isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✓ Completed",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
                
                Row {
                    // Completion toggle is available for all authenticated users
                    IconButton(
                        onClick = { onToggleCompletion(!note.isCompleted) }
                    ) {
                        Icon(
                            if (note.isCompleted) Icons.Default.Undo else Icons.Default.CheckCircle,
                            contentDescription = if (note.isCompleted) "Mark Incomplete" else "Mark Complete",
                            tint = if (note.isCompleted) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )
                    }
                    
                    // Edit and Delete are only available for admin users
                    if (isAdmin) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF10B981))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }
            
            if (note.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.description,
                    color = if (note.isCompleted) Color(0xFF9CA3AF) else Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }
    }
}
