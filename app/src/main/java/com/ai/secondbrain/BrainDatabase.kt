package com.ai.secondbrain

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class NoteItem(
    val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String,
    val timestamp: Long,
    val vector: FloatArray,
    var similarity: Float = 0.0f
)

class BrainDatabase(context: Context) : SQLiteOpenHelper(context, "second_brain.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                tags TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                vector_data TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS notes")
        onCreate(db)
    }

    fun insertNote(title: String, content: String, tags: String): Long {
        val fullText = "$title $content $tags"
        val vec = VectorEmbeddingEngine.embed(fullText)
        val vecStr = VectorEmbeddingEngine.serialize(vec)

        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", title)
            put("content", content)
            put("tags", tags)
            put("timestamp", System.currentTimeMillis())
            put("vector_data", vecStr)
        }
        return db.insert("notes", null, cv)
    }

    fun getAllNotes(): List<NoteItem> {
        val list = mutableListOf<NoteItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM notes ORDER BY timestamp DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToNote(it))
            }
        }
        return list
    }

    fun semanticSearch(query: String, threshold: Float = 0.35f): List<NoteItem> {
        if (query.isBlank()) return getAllNotes()

        val queryVec = VectorEmbeddingEngine.embed(query)
        val all = getAllNotes()
        for (n in all) {
            n.similarity = VectorEmbeddingEngine.cosineSimilarity(queryVec, n.vector)
        }

        return all.filter { it.similarity >= threshold }
            .sortedByDescending { it.similarity }
    }

    private fun cursorToNote(c: android.database.Cursor): NoteItem {
        val vecStr = c.getString(c.getColumnIndexOrThrow("vector_data"))
        return NoteItem(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            title = c.getString(c.getColumnIndexOrThrow("title")),
            content = c.getString(c.getColumnIndexOrThrow("content")),
            tags = c.getString(c.getColumnIndexOrThrow("tags")),
            timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp")),
            vector = VectorEmbeddingEngine.deserialize(vecStr)
        )
    }

    fun deleteNote(id: Long) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }
}
