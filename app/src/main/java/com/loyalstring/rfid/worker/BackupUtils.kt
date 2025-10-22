package com.loyalstring.rfid.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object BackupUtils {

    /** 🔹 Export Room database tables to CSV (creates one file per table) */
    suspend fun exportRoomDatabaseToCsv(context: Context, db: SQLiteDatabase) {
        withContext(Dispatchers.IO) {
            try {
                val tables = getAllTables(db)
                if (tables.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No tables found in database!", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                val folder = "DatabaseBackup"
                val combinedFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "$folder/Backup_All.csv"
                )
                if (!combinedFile.parentFile!!.exists()) combinedFile.parentFile!!.mkdirs()
                val combinedWriter = BufferedWriter(FileWriter(combinedFile, false))

                for (table in tables) {
                    val fileName = "$table.csv"
                    Log.d("BACKUP_DEBUG", "Backing up table: $table → $fileName")

                    // Write each table to its own CSV (MediaStore or legacy)
                    createOutputStream(context, folder, fileName).use { out ->
                        BufferedWriter(OutputStreamWriter(out)).use { writer ->
                            dumpTableToCsv(db, table, writer)
                        }
                    }

                    // Also write into unified backup file
                    combinedWriter.appendLine("=== TABLE: $table ===")
                    dumpTableToCsv(db, table, combinedWriter)
                    combinedWriter.appendLine()
                }

                combinedWriter.flush()
                combinedWriter.close()

                Log.d("BACKUP_DEBUG", "✅ Backup_All.csv saved at: ${combinedFile.absolutePath}")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "✅ Backup saved to Downloads/$folder (per-table + combined file)",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("BACKUP_DEBUG", "❌ Backup failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** 🔹 Get all user-defined tables from Room database */
    private fun getAllTables(db: SQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                    "AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%';",
            null
        )
        while (cursor.moveToNext()) tables.add(cursor.getString(0))
        cursor.close()
        return tables
    }

    /** 🔹 Dump one table’s content into a CSV writer */
    private fun dumpTableToCsv(db: SQLiteDatabase, table: String, writer: BufferedWriter) {
        val cursor = db.rawQuery("SELECT * FROM $table", null)
        cursor.use {
            // Header
            for (i in 0 until cursor.columnCount) {
                writer.append(cursor.getColumnName(i))
                if (i < cursor.columnCount - 1) writer.append(",")
            }
            writer.newLine()

            // Rows
            while (cursor.moveToNext()) {
                for (i in 0 until cursor.columnCount) {
                    val value = cursor.getString(i)?.replace("\n", " ") ?: ""
                    writer.append(value)
                    if (i < cursor.columnCount - 1) writer.append(",")
                }
                writer.newLine()
            }
            writer.flush()
        }
    }

    /** 🔹 Handle file creation (MediaStore for Q+, classic for older) */
    private fun createOutputStream(
        context: Context,
        subFolder: String,
        fileName: String
    ): OutputStream {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val downloads = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + subFolder)
                }
                val uri: Uri = context.contentResolver.insert(downloads, values)
                    ?: throw IOException("Failed to create file $fileName")
                context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Failed to open stream for $uri")
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subFolder)
                if (!dir.exists()) dir.mkdirs()
                FileOutputStream(File(dir, fileName))
            }
        } catch (e: Exception) {
            // fallback for Q+ if MediaStore fails
            Log.w("BACKUP_DEBUG", "MediaStore failed, using legacy path: ${e.message}")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subFolder)
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName))
        }
    }
}
