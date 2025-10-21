package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.Note
import com.humblecoders.plantmanagement.repositories.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NoteState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = ""
)

class NoteViewModel(
    private val noteRepository: NoteRepository
) {
    var noteState by mutableStateOf(NoteState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            noteState = noteState.copy(isLoading = true, error = null)
            try {
                val result = noteRepository.getAllNotes()
                result.fold(
                    onSuccess = { notes ->
                        noteState = noteState.copy(
                            notes = notes,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        noteState = noteState.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load notes"
                        )
                    }
                )
            } catch (e: Exception) {
                noteState = noteState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notes"
                )
            }
        }
    }

    fun addNote(title: String, description: String, date: LocalDate) {
        if (title.isBlank()) {
            noteState = noteState.copy(error = "Title cannot be empty")
            return
        }

        viewModelScope.launch {
            noteState = noteState.copy(isAdding = true, error = null)
            try {
                val note = Note(
                    title = title.trim(),
                    description = description.trim(),
                    date = date
                )
                
                val result = noteRepository.addNote(note)
                result.fold(
                    onSuccess = { noteId ->
                        noteState = noteState.copy(
                            isAdding = false,
                            successMessage = "Note added successfully"
                        )
                        loadNotes() // Refresh the list
                    },
                    onFailure = { error ->
                        noteState = noteState.copy(
                            isAdding = false,
                            error = error.message ?: "Failed to add note"
                        )
                    }
                )
            } catch (e: Exception) {
                noteState = noteState.copy(
                    isAdding = false,
                    error = e.message ?: "Failed to add note"
                )
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteState = noteState.copy(isUpdating = true, error = null)
            try {
                val result = noteRepository.updateNote(note)
                result.fold(
                    onSuccess = {
                        noteState = noteState.copy(
                            isUpdating = false,
                            successMessage = "Note updated successfully"
                        )
                        loadNotes() // Refresh the list
                    },
                    onFailure = { error ->
                        noteState = noteState.copy(
                            isUpdating = false,
                            error = error.message ?: "Failed to update note"
                        )
                    }
                )
            } catch (e: Exception) {
                noteState = noteState.copy(
                    isUpdating = false,
                    error = e.message ?: "Failed to update note"
                )
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteState = noteState.copy(isDeleting = true, error = null)
            try {
                val result = noteRepository.deleteNote(noteId)
                result.fold(
                    onSuccess = {
                        noteState = noteState.copy(
                            isDeleting = false,
                            successMessage = "Note deleted successfully"
                        )
                        loadNotes() // Refresh the list
                    },
                    onFailure = { error ->
                        noteState = noteState.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete note"
                        )
                    }
                )
            } catch (e: Exception) {
                noteState = noteState.copy(
                    isDeleting = false,
                    error = e.message ?: "Failed to delete note"
                )
            }
        }
    }

    fun toggleNoteCompletion(noteId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            noteState = noteState.copy(isUpdating = true, error = null)
            try {
                val result = noteRepository.toggleNoteCompletion(noteId, isCompleted)
                result.fold(
                    onSuccess = {
                        noteState = noteState.copy(
                            isUpdating = false,
                            successMessage = if (isCompleted) "Note marked as completed" else "Note marked as incomplete"
                        )
                        loadNotes() // Refresh the list
                    },
                    onFailure = { error ->
                        noteState = noteState.copy(
                            isUpdating = false,
                            error = error.message ?: "Failed to update note status"
                        )
                    }
                )
            } catch (e: Exception) {
                noteState = noteState.copy(
                    isUpdating = false,
                    error = e.message ?: "Failed to update note status"
                )
            }
        }
    }

    fun searchNotes(query: String) {
        noteState = noteState.copy(searchQuery = query)
        
        if (query.isBlank()) {
            loadNotes()
            return
        }

        viewModelScope.launch {
            noteState = noteState.copy(isLoading = true, error = null)
            try {
                val result = noteRepository.searchNotes(query)
                result.fold(
                    onSuccess = { notes ->
                        noteState = noteState.copy(
                            notes = notes,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        noteState = noteState.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to search notes"
                        )
                    }
                )
            } catch (e: Exception) {
                noteState = noteState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to search notes"
                )
            }
        }
    }

    fun clearError() {
        noteState = noteState.copy(error = null)
    }

    fun clearSuccessMessage() {
        noteState = noteState.copy(successMessage = null)
    }
}