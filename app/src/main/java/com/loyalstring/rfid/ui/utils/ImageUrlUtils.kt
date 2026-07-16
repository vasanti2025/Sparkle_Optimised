package com.loyalstring.rfid.ui.utils

import java.net.URI

const val DEFAULT_PRODUCT_IMAGE_BASE_URL = "https://rrgold.loyalstring.co.in/"
const val DESKTOP_IMAGE_SERVER_PORT = 8000

private val DESKTOP_IMAGE_EXTENSIONS = listOf(
    "jpg", "jpeg", "png", "webp", "gif", "svg", "jpe", "jep", "bmp", "heic", "tif", "tiff"
)

fun extractLastImagePath(imageValue: String?): String? {
    if (imageValue.isNullOrBlank()) return null
    val trimmed = imageValue.trim().trimEnd(',')
    if (trimmed.isBlank()) return null
    return trimmed
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .lastOrNull()
}

fun resolveImagePathToUrl(
    path: String,
    baseUrl: String = DEFAULT_PRODUCT_IMAGE_BASE_URL
): String {
    val trimmed = path.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    return normalizedBase + trimmed.removePrefix("/")
}

/**
 * Resolves a stored image value (relative path, full URL, or comma-separated list) to a loadable URL.
 * Returns null for blank values and absolute local file paths (starting with "/").
 */
fun resolveProductImageUrl(
    imageValue: String?,
    baseUrl: String = DEFAULT_PRODUCT_IMAGE_BASE_URL
): String? {
    val path = extractLastImagePath(imageValue) ?: return null
    if (path.startsWith("/")) return null
    return resolveImagePathToUrl(path, baseUrl)
}

/**
 * Extracts the host/IP from the saved custom API URL (e.g. http://192.168.1.54:8080/ -> 192.168.1.54).
 */
fun extractHostFromCustomApi(customApiUrl: String?): String? {
    if (customApiUrl.isNullOrBlank()) return null

    var url = customApiUrl.trim()
    if (!url.startsWith("http://", ignoreCase = true) &&
        !url.startsWith("https://", ignoreCase = true)
    ) {
        url = "http://$url"
    }

    return try {
        URI(url).host?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        url.removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .takeIf { it.isNotBlank() }
    }
}

/**
 * Builds desktop image URLs served from the user's local machine.
 * Uses saved custom API host with port 8000 and design name (then item code) as filename.
 * Example: http://192.168.1.54:8000/GPD1.jpg
 */
fun buildDesktopProductImageUrls(
    customApiUrl: String?,
    designName: String?,
    itemCode: String? = null,
): List<String> {
    val host = extractHostFromCustomApi(customApiUrl) ?: return emptyList()

    val baseNames = buildList {
        designName?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        itemCode?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.distinctBy { it.lowercase() }

    if (baseNames.isEmpty()) return emptyList()

    return baseNames.flatMap { name ->
        val extension = name.substringAfterLast('.', "").lowercase()
        val hasKnownExtension = extension in DESKTOP_IMAGE_EXTENSIONS

        if (hasKnownExtension && name.contains('.')) {
            listOf("http://$host:$DESKTOP_IMAGE_SERVER_PORT/$name")
        } else {
            DESKTOP_IMAGE_EXTENSIONS.map { ext ->
                "http://$host:$DESKTOP_IMAGE_SERVER_PORT/$name.$ext"
            }
        }
    }
}
