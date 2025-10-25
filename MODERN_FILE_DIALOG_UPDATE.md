# Modern File Dialog Implementation

## Summary
Replaced the old JFileChooser dialogs with modern Windows native file pickers that provide proper preview functionality and a better user experience.

## What Changed

### 1. New Utility Class: `FileDialogUtils.kt`
Created a utility class that provides:
- **Windows**: Uses native `java.awt.FileDialog` for modern Windows file picker with preview
- **Other platforms**: Falls back to `JFileChooser` for compatibility
- Methods for both file open and save dialogs

### 2. Updated Components
Updated the following components to use the new file dialog utility:

- **DocumentUploadComponent.kt**: Image and PDF document selection
- **ImageUploadComponent.kt**: Image file selection
- **SaleScreen.kt**: Document upload functionality
- **ViewSaleDialog.kt**: PDF save functionality
- **PurchaseScreen.kt**: Document upload and PDF save functionality
- **UserBalanceManagementScreen.kt**: PDF export functionality

## Benefits

1. **Modern UI**: Native Windows file picker with preview panel
2. **Better UX**: Improved file preview and selection experience on Windows
3. **Cross-platform**: Automatically uses the best dialog for each platform
4. **Consistent API**: Unified interface for all file selection operations

## How It Works

On Windows, the utility detects the OS and uses `java.awt.FileDialog` which provides:
- Native Windows file picker appearance
- Built-in preview pane for supported file types
- Modern Windows styling

On other platforms (macOS, Linux), it falls back to `JFileChooser` for compatibility.

## Usage Example

```kotlin
// Select multiple files
val selectedFiles = FileDialogUtils.showOpenDialog(
    title = "Select Documents",
    allowedExtensions = listOf("pdf", "jpg", "png"),
    allowMultiple = true
)

// Save a file
val outputFile = FileDialogUtils.showSaveDialog(
    title = "Save File",
    defaultFilename = "report.pdf",
    allowedExtensions = listOf("pdf")
)
```
