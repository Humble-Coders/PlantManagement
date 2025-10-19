package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import java.io.File

@Composable
fun ImageUploadComponent(
    selectedImages: List<File>,
    onImagesSelected: (List<File>) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "Sale Images (Optional)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981)
        )

        // Add Image Button
        OutlinedButton(
            onClick = {
                val fileChooser = javax.swing.JFileChooser()
                fileChooser.dialogTitle = "Select Images"
                fileChooser.currentDirectory = java.io.File(System.getProperty("user.home"))
                fileChooser.isMultiSelectionEnabled = true
                fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Image files", "jpg", "jpeg", "png", "gif", "bmp", "webp"
                )
                val result = fileChooser.showOpenDialog(null)
                if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                    val newImages = selectedImages.toMutableList()
                    newImages.addAll(fileChooser.selectedFiles.toList())
                    onImagesSelected(newImages)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Images")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Images")
        }

        // Selected Images Preview
        if (selectedImages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(selectedImages.size) { index ->
                    val imageFile = selectedImages[index]
                    ImagePreviewItem(
                        imageFile = imageFile,
                        onRemove = {
                            val newImages = selectedImages.toMutableList()
                            newImages.removeAt(index)
                            onImagesSelected(newImages)
                        }
                    )
                }
            }
        }

        // Help Text
        Text(
            "You can add multiple images related to this sale (invoices, receipts, etc.)",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
private fun ImagePreviewItem(
    imageFile: File,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp)),
            backgroundColor = Color(0xFF111827)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // For now, show file name since we don't have image loading in Compose Desktop
                Text(
                    text = imageFile.name.take(10) + if (imageFile.name.length > 10) "..." else "",
                    color = Color(0xFFF9FAFB),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .background(
                    Color(0xFFEF4444),
                    RoundedCornerShape(10.dp)
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove Image",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
