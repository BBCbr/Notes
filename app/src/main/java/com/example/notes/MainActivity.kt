package com.example.notes

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), Adapter1.OnButtonClickListener {
    private val items = arrayListOf<Noteitem>()
    private val filteredItems = arrayListOf<Noteitem>()
    private lateinit var notelist: RecyclerView
    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: Adapter1
    private lateinit var searchView: SearchView
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var isDeleting: Boolean = false // флаг для блокировки операций
    private val deleteDelay: Long = 500 // время задержки между операциями удаления


    private val DATABASE_VERSION = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            db = baseContext.openOrCreateDatabase( "app.db" , MODE_PRIVATE, null)

            // проверка и удаление существующей таблицы
            val currentVersion = getSchemaVersion()
            if (currentVersion < DATABASE_VERSION) {
                db.execSQL("DROP TABLE IF EXISTS notes")
                createNotesTable()
                setSchemaVersion(DATABASE_VERSION)
            } else {
                createNotesTable()
            }

            debugDatabase() // Вызов функции отладки базы данных

        } catch (e: Exception) {
            Log.e("DEBUG", "Error while creating/opening the database: ${e.message}")
            e.printStackTrace()
        }

        findViewById<Button>(R.id.add_n).setOnClickListener {
            addNote()
        }

        notelist = findViewById(R.id.recycle)
        notelist.layoutManager = LinearLayoutManager(this)

        adapter = Adapter1(filteredItems, this, this)
        notelist.adapter = adapter

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    Log.d( "DEBUG" ,  "Search submit: $it" )
                    filterNotes(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    Log.d( "DEBUG" ,  "Search text change: $it" )
                    filterNotes(it)
                }
                return true
            }
        })

        loadNotes()
    }

    private fun createNotesTable() {
        db.execSQL( "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT, note TEXT)" )
    }

    private fun getSchemaVersion(): Int {
        return db.rawQuery( "PRAGMA user_version;" , null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun setSchemaVersion(version: Int) {
        db.execSQL( "PRAGMA user_version = $version;" )
    }

    private fun loadNotes() {
        Log.d( "DEBUG" ,  "Loading notes from database" )
        items.clear()
        try {
            val cursor = db.rawQuery( "SELECT id, note FROM notes" , null)
            if (cursor.moveToFirst()) {
                do {
                    val idIndex = cursor.getColumnIndex( "id" )
                    val noteIndex = cursor.getColumnIndex( "note" )
                    if (idIndex != -1 && noteIndex != -1) {
                        val id = cursor.getLong(idIndex)
                        val note = cursor.getString(noteIndex)
                        Log.d( "DEBUG" ,  "Loaded note: id=$id, note=$note" )
                        items.add(Noteitem(id, note))
                    } else {
                        Log.e( "DEBUG" ,  "Error: Column 'id' or 'note' not found" )
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e( "DEBUG" ,  "Error while loading notes: ${e.message}" )
            e.printStackTrace()
        }
        filterNotes("") // Показать все заметки при загрузке
    }

    private fun filterNotes(query: String) {
        Log.d( "DEBUG" ,  "Filtering notes with query: $query" )
        filteredItems.clear()
        filteredItems.addAll(items.filter { it.note.contains(query, ignoreCase = true) })
        adapter.notifyDataSetChanged()
    }

    private fun addNote() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle( "Add Note" )

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton( "OK" ) { dialog, which ->
            val noteText = input.text.toString()
            if (noteText.isNotEmpty()) {
                try {
                    val values = ContentValues().apply {
                        put( "note" , noteText)
                    }
                    val newId = db.insert( "notes" , null, values)
                    if (newId != -1L) {
                        Log.d( "DEBUG" ,  "Note added with id $newId" )
                        debugDatabase()
                        loadNotes()
                    } else {
                        Toast.makeText(this,  "Error adding note" , Toast.LENGTH_SHORT).show()
                        Log.e( "DEBUG" ,  "Failed to add note to database" )
                    }
                } catch (e: Exception) {
                    Log.e( "DEBUG" ,  "Error adding note: ${e.message}" )
                    e.printStackTrace()
                }
            }
        }
        builder.setNegativeButton( "Cancel" ) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    override fun onButtonClick(position: Int) {
        val item = filteredItems[position]
        Toast.makeText(this,  "Clicked on: ${item.note}" , Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteButtonClick(position: Int) {
        if (isDeleting) return

        isDeleting = true
        executorService.execute {
            // удаление в отдельном потоке
            runOnUiThread {
                val item = filteredItems.getOrNull(position) ?: run {
                    isDeleting = false // сброс флага если элемент не найден
                    return@runOnUiThread
                }

                Log.d( "DEBUG" ,  "onDeleteButtonClick: position=$position, id=${item.id}" )

                if (deleteNoteFromDB(item.id)) {
                    Log.d( "DEBUG" ,  "Note deleted from database: id=${item.id}" )

                    items.remove(item)
                    filterNotes(searchView.query.toString())

                    Log.d( "DEBUG" ,  "Note removed from list and adapter notified" )
                } else {
                    Toast.makeText(this,  "Failed to delete note" , Toast.LENGTH_SHORT).show()
                    Log.e( "DEBUG" ,  "Failed to delete note from database: id=${item.id}" )
                }

                // задержка перед тем, как сбросить флаг
                handler.postDelayed({
                    isDeleting = false // Сброс флага
                }, deleteDelay)
            }
        }
    }

    override fun onEditButtonClick(position: Int) {
        val item = filteredItems[position]
        Log.d( "DEBUG" ,  "onEditButtonClick: position=$position, id=${item.id}" )

        editNoteDialog(item, position)
    }

    private fun editNoteDialog(item: Noteitem, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle( "Edit Note" )

        val input = EditText(this)
        input.setText(item.note)
        builder.setView(input)

        builder.setPositiveButton( "OK" ) { dialog, which ->
            val editedText = input.text.toString()
            if (editNoteInDB(item.id, editedText)) {
                Log.d( "DEBUG" ,  "Note edited in database: id=${item.id}, new note=$editedText" )

                item.note = editedText
                adapter.notifyItemChanged(position)

                Log.d( "DEBUG" ,  "Note updated in list and adapter notified" )
            } else {
                Toast.makeText(this,  "Failed to edit note" , Toast.LENGTH_SHORT).show()
                Log.e( "DEBUG" ,  "Failed to edit note in database: id=${item.id}" )
            }
        }

        builder.setNegativeButton( "Cancel" ) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun editNoteInDB(id: Long, newNote: String): Boolean {
        val values = ContentValues().apply {
            put( "note" , newNote)
        }
        val result = db.update( "notes" , values,  "id = ?" , arrayOf(id.toString()))
        Log.d( "DEBUG" ,  "editNoteInDB: result=$result" )
        return result > 0
    }

    private fun deleteNoteFromDB(id: Long): Boolean {
        Log.d( "DEBUG" ,  "deleteNoteFromDB: id=$id" )
        val result = db.delete( "notes" ,  "id = ?" , arrayOf(id.toString()))
        Log.d( "DEBUG" ,  "deleteNoteFromDB: result=$result" )
        return result > 0
    }

    override fun onSelectButtonClick(position: Int) {
        val item = filteredItems[position]

        // сохранение выбранной заметки в SharedPreferences
        val sharedPreferences = getSharedPreferences("NoteWidgetPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("note", item.note)
            apply()
        }

        // Проверка, что данные сохранены
        val savedNote = sharedPreferences.getString("note", "No note selected")
        Log.d("MainActivity", "Selected note for widget: $savedNote")

        // Обновление виджета
        NoteWidgetProvider.updateAllWidgets(this)

        Toast.makeText(this, "Selected note for widget: ${item.note}", Toast.LENGTH_SHORT).show()
    }




    private fun debugDatabase() {
        val cursor = db.rawQuery( "PRAGMA table_info(notes);" , null)
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow( "name" ))
                val type = cursor.getString(cursor.getColumnIndexOrThrow( "type" ))
                Log.d( "DEBUG" ,  "Column: $name, Type: $type" )
            } while (cursor.moveToNext())
        }
        cursor.close()
        val cursor2 = db.rawQuery( "SELECT * FROM notes" , null)
        if (cursor2.moveToFirst()) {
            do {
                for (i in 0 until cursor2.columnCount) {
                    Log.d( "DEBUG" , cursor2.getColumnName(i) +  ": "  + cursor2.getString(i))
                }
            } while (cursor2.moveToNext())
        }
        cursor2.close()
    }
}