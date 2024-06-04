package com.example.notes

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetFactory(applicationContext)
    }
}

class NoteWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var notes: List<String> = listOf()

    override fun onCreate() {
        loadNotes()
    }

    override fun onDataSetChanged() {
        loadNotes()
    }

    private fun loadNotes() {
        // Загрузка заметки из SharedPreferences
        val sharedPreferences = context.getSharedPreferences("NoteWidgetPrefs", Context.MODE_PRIVATE)
        val note = sharedPreferences.getString("note", "No note selected")
        notes = listOf(note ?: "No note selected")
    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return notes.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.widget_note_text, notes[position])
        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}