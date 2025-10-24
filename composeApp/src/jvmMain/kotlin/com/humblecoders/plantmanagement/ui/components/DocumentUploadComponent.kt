package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.io.File

@Composable
fun DocumentUploadComponent(
    selectedDocuments: List<File>,
    onDocumentsSelected: (List<File>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Add Document",
    allowedExtensions: List<String> = listOf("jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "txt"),
    maxFiles: Int = 5
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFFF9FAFB),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        
        // Document selection button
        OutlinedButton(
            onClick = {
                val fileChooser = javax.swing.JFileChooser()
                fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Documents (Images, PDFs, Word docs)", 
                    *allowedExtensions.toTypedArray()
                )
                fileChooser.isMultiSelectionEnabled = true
                
                val result = fileChooser.showOpenDialog(null)
                if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                    val selectedFiles = fileChooser.selectedFiles.toList()
                    val validFiles = selectedFiles.filter { file ->
                        val extension = file.extension.lowercase()
                        extension in allowedExtensions
                    }
                    
                    // Limit to maxFiles
                    val filesToAdd = validFiles.take(maxFiles - selectedDocuments.size)
                    onDocumentsSelected(selectedDocuments + filesToAdd)
                }
            },
            enabled = selectedDocuments.size < maxFiles,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF06B6D4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Document", tint = Color(0xFF06B6D4))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedDocuments.size < maxFiles) "Select Documents" else "Maximum files reached",
                color = Color(0xFF06B6D4)
            )
        }
        
        // Selected documents list
        if (selectedDocuments.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedDocuments.forEachIndexed { index, file ->
                    DocumentItem(
                        file = file,
                        onRemove = {
                            val updatedList = selectedDocuments.toMutableList()
                            updatedList.removeAt(index)
                            onDocumentsSelected(updatedList)
                        }
                    )
                }
            }
        }
        
        // File type info
        Text(
            text = "Supported formats: ${allowedExtensions.joinToString(", ").uppercase()}",
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DocumentItem(
    file: File,
    onRemove: () -> Unit
) {
    val isImage = file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    val isPdf = file.extension.lowercase() == "pdf"
    
    Card(
        backgroundColor = Color(0xFF111827),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when {
                        isImage -> Icons.Default.Image
                        isPdf -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = when {
                        isImage -> Color(0xFF10B981)
                        isPdf -> Color(0xFFEF4444)
                        else -> Color(0xFF06B6D4)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = file.name,
                        color = Color(0xFFF9FAFB),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(file.length() / 1024.0).toInt()} KB",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
