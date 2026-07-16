package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.loyalstring.rfid.ui.screens.saveImageFromUrlToLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProductImageWithAllFallbacks(
    imageUrl: String?,
    itemCode: String?,
    designName: String? = null,
    baseUrl: String = DEFAULT_PRODUCT_IMAGE_BASE_URL,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    priorityLocalPaths: List<String> = emptyList(),
    cacheRemoteToLocal: Boolean = true,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallbackIcon: ImageVector = Icons.Default.Photo,
    fallbackIconTint: Color = Color.Gray,
    onImageError: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val customApiUrl = remember { UserPreferences.getInstance(context).getCustomApi() }

    val imageCandidates = remember(
        imageUrl,
        itemCode,
        designName,
        customApiUrl,
        baseUrl,
        priorityLocalPaths,
    ) {
        buildProductImageLoadCandidates(
            context = context,
            imageUrl = imageUrl,
            itemCode = itemCode,
            designName = designName,
            customApiUrl = customApiUrl,
            baseUrl = baseUrl,
            priorityLocalPaths = priorityLocalPaths,
        )
    }

    var candidateIndex by remember(imageUrl, itemCode, designName, priorityLocalPaths) {
        mutableIntStateOf(0)
    }
    val currentModel = imageCandidates.getOrNull(candidateIndex)

    val remoteUrl = remember(imageUrl, baseUrl) {
        resolveProductImageUrl(imageUrl, baseUrl)
    }

    LaunchedEffect(remoteUrl, itemCode, designName, cacheRemoteToLocal) {
        if (!cacheRemoteToLocal || remoteUrl.isNullOrBlank() || itemCode.isNullOrBlank()) return@LaunchedEffect

        val hasLocalFile = getLocalProductImageFile(context, itemCode, designName) != null
        if (!hasLocalFile) {
            withContext(Dispatchers.IO) {
                saveImageFromUrlToLocal(context, remoteUrl, itemCode)
            }
        }
    }

    val tryNextCandidate: () -> Unit = {
        if (candidateIndex < imageCandidates.lastIndex) {
            candidateIndex++
        } else {
            onImageError?.invoke()
        }
    }

    when {
        currentModel != null && placeholder != null -> {
            AsyncImage(
                model = currentModel,
                contentDescription = contentDescription,
                modifier = modifier,
                placeholder = placeholder,
                error = error ?: placeholder,
                onError = { tryNextCandidate() },
            )
        }

        currentModel != null -> {
            AsyncImage(
                model = currentModel,
                contentDescription = contentDescription,
                modifier = modifier,
                onError = { tryNextCandidate() },
            )
        }

        else -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                if (error != null) {
                    Icon(
                        painter = error,
                        contentDescription = contentDescription,
                        tint = Color.Unspecified,
                    )
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = contentDescription,
                        tint = fallbackIconTint,
                    )
                }
            }
        }
    }
}

@Composable
fun ProductImagePainterWithAllFallbacks(
    imageUrl: String?,
    itemCode: String?,
    designName: String? = null,
    baseUrl: String = DEFAULT_PRODUCT_IMAGE_BASE_URL,
    priorityLocalPaths: List<String> = emptyList(),
    onImageError: (() -> Unit)? = null,
): Painter {
    val context = LocalContext.current
    val customApiUrl = remember { UserPreferences.getInstance(context).getCustomApi() }

    val imageCandidates = remember(
        imageUrl,
        itemCode,
        designName,
        customApiUrl,
        baseUrl,
        priorityLocalPaths,
    ) {
        buildProductImageLoadCandidates(
            context = context,
            imageUrl = imageUrl,
            itemCode = itemCode,
            designName = designName,
            customApiUrl = customApiUrl,
            baseUrl = baseUrl,
            priorityLocalPaths = priorityLocalPaths,
        )
    }

    var candidateIndex by remember(imageUrl, itemCode, designName, priorityLocalPaths) {
        mutableIntStateOf(0)
    }
    val currentModel = imageCandidates.getOrNull(candidateIndex)

    return rememberAsyncImagePainter(
        model = currentModel,
        onError = {
            if (candidateIndex < imageCandidates.lastIndex) {
                candidateIndex++
            } else {
                onImageError?.invoke()
            }
        },
    )
}
