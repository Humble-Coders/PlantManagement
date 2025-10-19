package com.humblecoders.plantmanagement.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

fun java.awt.image.BufferedImage.toComposeImageBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    return this.toComposeImageBitmap()
}
