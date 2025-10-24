package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class NoteRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getNotesCollection() =
        firestore.collection("notes")

    /**
     * Add a new note using transaction for atomicity
     */
    suspend fun addNote(note: Note): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getNotesCollection().document()
            
            // Use transaction to ensure atomic write
            firestore.runTransaction { transaction ->
                val noteData = mapOf(
                    "userId" to userId,
                    "title" to note.title,
                    "description" to note.description,
                    "date" to note.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    "isCompleted" to note.isCompleted,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(docRef, noteData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing note using transaction for atomicity
     */
    suspend fun updateNote(note: Note): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                val noteData = mapOf(
                    "userId" to userId,
                    "title" to note.title,
                    "description" to note.description,
                    "date" to note.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    "isCompleted" to note.isCompleted,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.update(getNotesCollection().document(note.id), noteData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a note using transaction for atomicity
     */
    suspend fun deleteNote(noteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                transaction.delete(getNotesCollection().document(noteId))
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle completion status of a note
     */
    suspend fun toggleNoteCompletion(noteId: String, isCompleted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                transaction.update(
                    getNotesCollection().document(noteId),
                    "isCompleted", isCompleted
                )
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all notes for the current user
     */
    suspend fun getAllNotes(): Result<List<Note>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getNotesCollection()
                .get()
                .get(10, TimeUnit.SECONDS)

            val notes = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Note(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        date = if (data["date"] is Long) {
                            LocalDate.ofEpochDay((data["date"] as Long) / (24 * 60 * 60 * 1000))
                        } else {
                            LocalDate.now()
                        },
                        isCompleted = data["isCompleted"] as? Boolean ?: false,
                        createdAt = data["createdAt"] as? com.google.cloud.Timestamp
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.date }

            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search notes by title or description
     */
    suspend fun searchNotes(query: String): Result<List<Note>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getNotesCollection()
                .get()
                .get(10, TimeUnit.SECONDS)

            val notes = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val title = data["title"] as? String ?: ""
                    val description = data["description"] as? String ?: ""
                    
                    // Filter by search query
                    if (title.contains(query, ignoreCase = true) || 
                        description.contains(query, ignoreCase = true)) {
                        Note(
                            id = doc.id,
                            title = title,
                            description = description,
                            date = if (data["date"] is Long) {
                                LocalDate.ofEpochDay((data["date"] as Long) / (24 * 60 * 60 * 1000))
                            } else {
                                LocalDate.now()
                            },
                            isCompleted = data["isCompleted"] as? Boolean ?: false,
                            createdAt = data["createdAt"] as? com.google.cloud.Timestamp
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.date }

            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}