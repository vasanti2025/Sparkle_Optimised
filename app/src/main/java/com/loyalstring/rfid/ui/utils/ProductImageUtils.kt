package com.loyalstring.rfid.ui.utils

import android.content.Context
import java.io.File

const val PRODUCT_IMAGES_DIR_NAME = "product_images"

private val NON_IMAGE_EXTENSIONS = setOf(
    "tmp", "temp", "log", "txt", "json", "xml", "db", "db-journal", "nomedia"
)

private val PREFERRED_IMAGE_EXTENSIONS = listOf(
    "jpg", "jpeg", "jpe", "jep", "png", "webp", "gif", "bmp",
    "svg", "heic", "heif", "tif", "tiff", "ico", "avif", "wmf"
)

fun getProductImageDirectories(context: Context): List<File> {
    return listOfNotNull(
        File(context.filesDir, PRODUCT_IMAGES_DIR_NAME).takeIf { it.exists() },
        context.getExternalFilesDir(null)
            ?.let { File(it, PRODUCT_IMAGES_DIR_NAME) }
            ?.takeIf { it.exists() }
    )
}

private fun isLocalImageCandidate(file: File): Boolean {
    if (!file.isFile || !file.canRead()) return false
    val ext = file.extension.lowercase()
    if (ext.isBlank()) return false
    return ext !in NON_IMAGE_EXTENSIONS
}

private fun extensionPriority(ext: String): Int {
    val normalized = ext.lowercase()
    val index = PREFERRED_IMAGE_EXTENSIONS.indexOf(normalized)
    return if (index >= 0) index else PREFERRED_IMAGE_EXTENSIONS.size
}

private fun findImageByBaseName(dir: File, baseName: String): File? {
    val trimmed = baseName.trim()
    if (trimmed.isBlank()) return null

    val exactPath = File(dir, trimmed)
    if (isLocalImageCandidate(exactPath)) return exactPath

    PREFERRED_IMAGE_EXTENSIONS.forEach { ext ->
        listOf(ext, ext.uppercase()).forEach { extension ->
            val file = File(dir, "$trimmed.$extension")
            if (isLocalImageCandidate(file)) return file
        }
    }

    return dir.listFiles()
        ?.filter { file ->
            isLocalImageCandidate(file) &&
                file.nameWithoutExtension.equals(trimmed, ignoreCase = true)
        }
        ?.minWithOrNull(compareBy({ extensionPriority(it.extension) }, { it.name }))
}

/**
 * Finds a locally saved product image by item code and/or design name.
 * Item code is checked first to preserve existing behaviour.
 * Supports any common image extension (jpg, jpeg, jep, png, webp, gif, svg, etc.).
 */
fun getLocalProductImageFile(
    context: Context,
    itemCode: String? = null,
    designName: String? = null,
): File? {
    val candidates = buildList {
        itemCode?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        designName?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.distinctBy { it.lowercase() }

    if (candidates.isEmpty()) return null

    getProductImageDirectories(context).forEach { dir ->
        candidates.forEach { candidate ->
            findImageByBaseName(dir, candidate)?.let { return it }
        }
    }

    return null
}

fun ensureProductImagesFolder(context: Context): File? {
    return try {
        val imageDir = File(context.getExternalFilesDir(null), PRODUCT_IMAGES_DIR_NAME)
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        imageDir
    } catch (_: Exception) {
        null
    }
}

/**
 * Builds the full image fallback chain without breaking any existing source:
 * 1. Priority local paths (picked/captured images)
 * 2. Absolute path stored in imageUrl
 * 3. product_images folder (item code, then design name, any extension)
 * 4. Online URL (full CloudFront/https URL or relative path on rrgold server)
 * 5. Desktop IP URL (saved custom API host + port 8000 + design/item code)
 */
fun buildProductImageLoadCandidates(
    context: Context,
    imageUrl: String?,
    itemCode: String?,
    designName: String?,
    customApiUrl: String?,
    baseUrl: String = DEFAULT_PRODUCT_IMAGE_BASE_URL,
    priorityLocalPaths: List<String> = emptyList(),
): List<Any> {
    val candidates = mutableListOf<Any>()
    val seenPaths = mutableSetOf<String>()
    val seenUrls = mutableSetOf<String>()

    fun addFile(file: File?) {
        if (file == null || !file.exists() || !file.isFile) return
        val key = file.absolutePath
        if (seenPaths.add(key)) {
            candidates.add(file)
        }
    }

    fun addUrl(url: String?) {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.isBlank()) return
        if (seenUrls.add(trimmed.lowercase())) {
            candidates.add(trimmed)
        }
    }

    priorityLocalPaths.forEach { path ->
        addFile(File(path.trim()))
    }

    imageUrl?.trim()?.trimEnd(',')
        ?.takeIf { it.startsWith("/") }
        ?.let { addFile(File(it)) }

    addFile(getLocalProductImageFile(context, itemCode, designName))

    addUrl(resolveProductImageUrl(imageUrl, baseUrl))

    buildDesktopProductImageUrls(customApiUrl, designName, itemCode).forEach { addUrl(it) }

    return candidates
}
