package com.humblecoders.plantmanagement.utils

import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

object FileDialogUtils {
    
    /**
     * Show a modern file open dialog with preview support
     * On Windows, this will use the native Windows file picker with preview
     * On other platforms, it uses the standard file chooser
     */
    fun showOpenDialog(
        title: String = "Select File",
        defaultDirectory: File? = null,
        allowedExtensions: List<String> = emptyList(),
        allowMultiple: Boolean = false
    ): List<File> {
        return if (System.getProperty("os.name").lowercase().contains("windows")) {
            // Use native Windows FileDialog on Windows
            showWindowsFileDialog(title, defaultDirectory, allowedExtensions, allowMultiple)
        } else {
            // Use JFileChooser on other platforms
            showJFileChooserDialog(title, defaultDirectory, allowedExtensions, allowMultiple)
        }
    }
    
    /**
     * Modern Windows file dialog with native preview support
     */
    private fun showWindowsFileDialog(
        title: String,
        defaultDirectory: File?,
        allowedExtensions: List<String>,
        allowMultiple: Boolean
    ): List<File> {
        val frame = java.awt.Frame()
        val dialog = FileDialog(frame, title, FileDialog.LOAD)
        
        // Set the default directory
        if (defaultDirectory != null && defaultDirectory.exists()) {
            dialog.directory = defaultDirectory.absolutePath
        }
        
        // Configure file filter if extensions are provided
        if (allowedExtensions.isNotEmpty()) {
            // For Windows FileDialog, we need to manually filter
            // but we'll show the filter in the dialog title
            val extensionList = allowedExtensions.joinToString(", ") { "*.$it" }
            dialog.title = "$title ($extensionList)"
        }
        
        // FileDialog doesn't natively support multiple selection
        dialog.isVisible = true
        
        val selectedFiles = mutableListOf<File>()
        
        // Get the selected file (FileDialog supports single file selection)
        val directory = dialog.directory
        val filename = dialog.file
        if (directory != null && filename != null) {
            selectedFiles.add(File(directory, filename))
        }
        
        // Filter by extensions if specified
        if (allowedExtensions.isNotEmpty()) {
            selectedFiles.removeIf { file ->
                val ext = file.extension.lowercase()
                ext !in allowedExtensions.map { it.lowercase() }
            }
        }
        
        dialog.dispose()
        frame.dispose()
        
        return selectedFiles
    }
    
    /**
     * JFileChooser for non-Windows platforms or as fallback
     */
    private fun showJFileChooserDialog(
        title: String,
        defaultDirectory: File?,
        allowedExtensions: List<String>,
        allowMultiple: Boolean
    ): List<File> {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = title
        
        // Set the default directory
        if (defaultDirectory != null && defaultDirectory.exists()) {
            fileChooser.currentDirectory = defaultDirectory
        }
        
        // Enable multi-selection
        if (allowMultiple) {
            fileChooser.isMultiSelectionEnabled = true
        }
        
        // Set file filter if extensions are provided
        if (allowedExtensions.isNotEmpty()) {
            val filter = object : javax.swing.filechooser.FileFilter() {
                override fun accept(f: File): Boolean {
                    if (f.isDirectory) return true
                    val ext = f.extension.lowercase()
                    return ext in allowedExtensions.map { it.lowercase() }
                }
                
                override fun getDescription(): String {
                    val exts = allowedExtensions.joinToString(", ") { ".$it" }
                    return "Supported files ($exts)"
                }
            }
            fileChooser.fileFilter = filter
        }
        
        val result = fileChooser.showOpenDialog(null)
        
        if (result == JFileChooser.APPROVE_OPTION) {
            return if (allowMultiple && fileChooser.isMultiSelectionEnabled) {
                fileChooser.selectedFiles.toList()
            } else {
                listOf(fileChooser.selectedFile)
            }
        }
        
        return emptyList()
    }
    
    /**
     * Show a save dialog with modern UI
     */
    fun showSaveDialog(
        title: String = "Save File",
        defaultDirectory: File? = null,
        defaultFilename: String? = null,
        allowedExtensions: List<String> = emptyList()
    ): File? {
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            val frame = java.awt.Frame()
            val dialog = FileDialog(frame, title, FileDialog.SAVE)
            
            if (defaultDirectory != null && defaultDirectory.exists()) {
                dialog.directory = defaultDirectory.absolutePath
            }
            
            if (defaultFilename != null) {
                dialog.file = defaultFilename
            }
            
            dialog.isVisible = true
            
            val directory = dialog.directory
            val filename = dialog.file
            
            dialog.dispose()
            frame.dispose()
            
            if (directory != null && filename != null) {
                return File(directory, filename)
            }
            
            return null
        } else {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = title
            
            if (defaultDirectory != null && defaultDirectory.exists()) {
                fileChooser.currentDirectory = defaultDirectory
            }
            
            if (defaultFilename != null) {
                fileChooser.selectedFile = File(defaultFilename)
            }
            
            if (allowedExtensions.isNotEmpty()) {
                val filter = object : javax.swing.filechooser.FileFilter() {
                    override fun accept(f: File): Boolean {
                        if (f.isDirectory) return true
                        val ext = f.extension.lowercase()
                        return ext in allowedExtensions.map { it.lowercase() }
                    }
                    
                    override fun getDescription(): String {
                        val exts = allowedExtensions.joinToString(", ") { ".$it" }
                        return "Supported files ($exts)"
                    }
                }
                fileChooser.fileFilter = filter
            }
            
            val result = fileChooser.showSaveDialog(null)
            
            if (result == JFileChooser.APPROVE_OPTION) {
                return fileChooser.selectedFile
            }
            
            return null
        }
    }
}
