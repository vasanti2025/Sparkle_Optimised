package com.loyalstring.rfid.ui.utils

import android.content.Context

/**
 * Persists product image URLs by item code so Sample Out list/PDF can still
 * show images after items are removed from local labelled stock (bulk_items).
 */
object SampleOutImageCache {
    private const val PREFS_NAME = "sample_out_image_cache"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(itemCode: String) = "img_${itemCode.trim().lowercase()}"

    fun save(context: Context, itemCode: String?, imageUrl: String?) {
        val code = itemCode?.trim()?.takeIf { it.isNotBlank() } ?: return
        val url = imageUrl?.trim()?.takeIf { it.isNotBlank() } ?: return
        prefs(context).edit().putString(key(code), url).apply()
    }

    fun get(context: Context, itemCode: String?): String? {
        val code = itemCode?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return prefs(context).getString(key(code), null)?.takeIf { it.isNotBlank() }
    }

    fun saveAll(context: Context, entries: List<Pair<String?, String?>>) {
        val editor = prefs(context).edit()
        entries.forEach { (itemCode, imageUrl) ->
            val code = itemCode?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
            val url = imageUrl?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
            editor.putString(key(code), url)
        }
        editor.apply()
    }
}
