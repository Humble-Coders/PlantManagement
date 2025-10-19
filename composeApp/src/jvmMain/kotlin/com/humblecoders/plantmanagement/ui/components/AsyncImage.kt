package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URL
import javax.imageio.ImageIO

@Composable
fun AsyncImage(
    imageUrl: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF374151)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF10B981),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Loading...",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }
        }
    },
    error: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF374151)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Failed to load",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp
                )
            }
        }
    }
) {
    var imageState by remember { mutableStateOf<ImageState>(ImageState.Loading) }
    var bufferedImage by remember { mutableStateOf<BufferedImage?>(null) }

    LaunchedEffect(imageUrl) {
        imageState = ImageState.Loading
        try {
            bufferedImage = withContext(Dispatchers.IO) {
                loadImageFromUrl(imageUrl)
            }
            imageState = if (bufferedImage != null) ImageState.Success else ImageState.Error
        } catch (e: Exception) {
            println("Error loading image: ${e.message}")
            imageState = ImageState.Error
        }
    }

    Box(modifier = modifier) {
        when (imageState) {
            is ImageState.Loading -> placeholder()
            is ImageState.Success -> {
                bufferedImage?.let { image ->
                    val bitmap = image.toComposeImageBitmap()
                    Image(
                        painter = BitmapPainter(bitmap),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
            }
            is ImageState.Error -> error()
        }
    }
}

private suspend fun loadImageFromUrl(url: String): BufferedImage? {
    return try {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val inputStream = connection.getInputStream()
        val bufferedImage = ImageIO.read(inputStream)
        inputStream.close()
        bufferedImage
    } catch (e: Exception) {
        println("Error loading image from URL: ${e.message}")
        null
    }
}

private sealed class ImageState {
    object Loading : ImageState()
    object Success : ImageState()
    object Error : ImageState()
}
