package com.obsidianwidget

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.obsidianwidget.local.ChechuRepository
import com.obsidianwidget.local.NoteRecord
import com.obsidianwidget.local.TaskRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

/**
 * Real offline management screen: all database I/O runs on a single worker.
 * Legacy Obsidian notes remain available through the original widget settings.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_TASK_ID = "v3_open_task_id"
        const val EXTRA_OPEN_NOTE_ID = "v3_open_note_id"
        const val EXTRA_CREATE_TASK = "v3_create_task"
        const val EXTRA_CREATE_NOTE = "v3_create_note"
    }
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var repository: ChechuRepository
    private lateinit var listContainer: LinearLayout
    private lateinit var tabTasks: TextView
    private lateinit var tabNotes: TextView
    private lateinit var stateActive: TextView
    private lateinit var stateArchive: TextView
    private lateinit var stateTrash: TextView
    private lateinit var addAction: TextView
    private var notesMode = false
    private var category = "active"

    private val ink = Color.rgb(31, 32, 36)
    private val muted = Color.rgb(111, 113, 119)
    private val coral = Color.rgb(255, 120, 92)
    private val pale = Color.rgb(246, 245, 244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ChechuRepository(this)
        buildScreen()
        refresh()
        handleLaunch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunch(intent)
    }

    private fun handleLaunch(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra(EXTRA_OPEN_TASK_ID)
        val noteId = intent.getStringExtra(EXTRA_OPEN_NOTE_ID)
        val newTask = intent.getBooleanExtra(EXTRA_CREATE_TASK, false)
        val newNote = intent.getBooleanExtra(EXTRA_CREATE_NOTE, false)
        intent.removeExtra(EXTRA_OPEN_TASK_ID)
        intent.removeExtra(EXTRA_OPEN_NOTE_ID)
        intent.removeExtra(EXTRA_CREATE_TASK)
        intent.removeExtra(EXTRA_CREATE_NOTE)
        when {
            taskId != null -> worker.execute {
                val item = repository.getTask(taskId)
                runOnUiThread { if (!isFinishing && item != null) editTask(item) }
            }
            noteId != null -> worker.execute {
                val item = repository.getNote(noteId)
                runOnUiThread { if (!isFinishing && item != null) editNote(item) }
            }
            newTask -> editTask(null)
            newNote -> editNote(null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::listContainer.isInitialized) refresh()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun surface(color: Int, radius: Int = 22, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun label(text: String, size: Float, bold: Boolean = false, color: Int = ink): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface = if (bold) android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun vertical(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    private fun horizontal(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun buildScreen() {
        val background = vertical().apply {
            setBackgroundColor(Color.rgb(251, 250, 249))
        }
        val main = vertical().apply {
            setPadding(dp(20), dp(20), dp(20), dp(8))
        }
        val top = horizontal()
        val heading = vertical().apply {
            addView(label("Today", 30f, true))
            addView(label("Your tasks and notes, in one place", 12f, false, muted))
        }
        top.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        val settings = label("⚙", 23f).apply {
            gravity = Gravity.CENTER
            contentDescription = "Legacy vault settings"
            setBackground(surface(pale, 16))
            setOnClickListener {
                val ids = AppWidgetManager.getInstance(this@MainActivity).getAppWidgetIds(
                    ComponentName(this@MainActivity, ObsidianWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    startActivity(Intent(this@MainActivity, WidgetConfigActivity::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[0])
                    })
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Widget setup")
                        .setMessage("Add the Today widget to your home screen first, then open its settings to choose offline tasks or Obsidian notes.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
        top.addView(settings, LinearLayout.LayoutParams(dp(46), dp(46)))
        main.addView(top)
        val tabs = horizontal().apply {
            setBackground(surface(Color.rgb(239, 238, 237), 23))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        tabTasks = label("To-Do", 14f, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { notesMode = false; refresh() }
        }
        tabNotes = label("Notes", 14f, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { notesMode = true; refresh() }
        }
        tabs.addView(tabTasks, LinearLayout.LayoutParams(0, dp(40), 1f))
        tabs.addView(tabNotes, LinearLayout.LayoutParams(0, dp(40), 1f))
        main.addView(tabs, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(25) })
        val filters = horizontal().apply { setPadding(0, dp(15), 0, dp(13)) }
        stateActive = label("Active", 13f, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { category = "active"; refresh() }
        }
        stateArchive = label("Archive", 13f).apply {
            gravity = Gravity.CENTER
            setOnClickListener { category = "archive"; refresh() }
        }
        stateTrash = label("Trash", 13f).apply {
            gravity = Gravity.CENTER
            setOnClickListener { category = "trash"; refresh() }
        }
        for (button in listOf(stateActive, stateArchive, stateTrash)) {
            filters.addView(button, LinearLayout.LayoutParams(0, dp(38), 1f))
        }
        main.addView(filters)
        background.addView(main)
        val scroller = ScrollView(this).apply {
            setFillViewport(true)
            clipToPadding = false
        }
        listContainer = vertical().apply {
            setPadding(dp(20), dp(2), dp(20), dp(12))
        }
        scroller.addView(listContainer)
        background.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        addAction = label("+   Add To-Do", 16f, true, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setBackground(surface(coral, 25))
            setOnClickListener {
                if (notesMode) editNote(null) else editTask(null)
            }
        }
        background.addView(addAction, LinearLayout.LayoutParams(-1, dp(54)).apply {
            leftMargin = dp(20); rightMargin = dp(20)
            bottomMargin = dp(20); topMargin = dp(8)
        })
        setContentView(background)
    }

    private fun refresh() {
        tabTasks.setBackground(surface(if (!notesMode) Color.rgb(255, 227, 216) else Color.TRANSPARENT))
        tabNotes.setBackground(surface(if (notesMode) Color.rgb(255, 227, 216) else Color.TRANSPARENT))
        tabTasks.setTextColor(if (!notesMode) ink else muted)
        tabNotes.setTextColor(if (notesMode) ink else muted)
        for ((name, view) in listOf("active" to stateActive, "archive" to stateArchive, "trash" to stateTrash)) {
            view.setBackground(surface(if (category == name) Color.WHITE else Color.TRANSPARENT, 14))
            view.setTextColor(if (category == name) coral else muted)
        }
        addAction.text = if (notesMode) "+   New Note" else "+   Add To-Do"
        addAction.visibility = if (category == "active") View.VISIBLE else View.GONE
        val requestNotes = notesMode
        val requestCategory = category
        worker.execute {
            try {
                val archive = requestCategory == "archive"
                val trash = requestCategory == "trash"
                val tasks = if (!requestNotes) repository.tasks(archive, trash) else emptyList()
                val notes = if (requestNotes) repository.notes(archive, trash) else emptyList()
                runOnUiThread {
                    if (isFinishing || isDestroyed || notesMode != requestNotes || category != requestCategory) return@runOnUiThread
                    render(tasks, notes)
                }
            } catch (e: Exception) {
                runOnUiThread { showError("Could not load records: " + e.javaClass.simpleName) }
            }
        }
    }

    private fun render(tasks: List<TaskRecord>, notes: List<NoteRecord>) {
        listContainer.removeAllViews()
        if (tasks.isEmpty() && notes.isEmpty()) {
            val message = when (category) {
                "trash" -> "Nothing in Trash"
                "archive" -> "Nothing archived yet"
                else -> if (notesMode) "Your notes will appear here." else "A fresh start. Add your first to-do."
            }
            listContainer.addView(label(message, 15f, false, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(85), dp(20), dp(30))
            })
            return
        }
        if (notesMode) notes.forEach(::renderNote) else tasks.forEach(::renderTask)
    }

    private fun renderTask(item: TaskRecord) {
        val row = horizontal().apply {
            setBackground(surface(pale))
            setPadding(dp(14), dp(13), dp(10), dp(13))
        }
        val check = label(if (item.completed) "✓" else "○", 27f, true,
            if (item.completed) coral else ink).apply {
            gravity = Gravity.CENTER
            contentDescription = "Toggle " + item.title
            setOnClickListener { changeTask(item.id) }
        }
        row.addView(check, LinearLayout.LayoutParams(dp(41), dp(48)))
        val body = vertical()
        val title = label(item.title, 15f, true, if (item.completed) muted else ink)
        if (item.completed) title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        body.addView(title)
        val info = mutableListOf<String>()
        if (item.repeatRule != "NONE") info.add("↻ " + item.repeatRule.lowercase().replaceFirstChar { it.uppercase() })
        item.dueEpochDay?.let { info.add(LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM d"))) }
        if (item.details.isNotBlank()) info.add(item.details.replace("\n", " ").take(85))
        if (info.isNotEmpty()) body.addView(label(info.joinToString(" · "), 12f, false, muted).apply { maxLines = 2 })
        body.setOnClickListener { editTask(item) }
        row.addView(body, LinearLayout.LayoutParams(0, -2, 1f))
        val menu = label("⋮", 23f, true, muted).apply {
            gravity = Gravity.CENTER
            contentDescription = "Task actions"
            setOnClickListener { taskMenu(this, item) }
        }
        row.addView(menu, LinearLayout.LayoutParams(dp(38), dp(48)))
        addCard(row)
    }

    private fun renderNote(item: NoteRecord) {
        val row = horizontal().apply {
            setBackground(surface(pale))
            setPadding(dp(18), dp(17), dp(9), dp(17))
        }
        val body = vertical()
        body.addView(label(item.title, 15f, true))
        body.addView(label(item.body.ifBlank { "Empty note" }.replace("\n", " ").take(160),
            12f, false, muted).apply { maxLines = 3 })
        body.setOnClickListener { editNote(item) }
        row.addView(body, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label("⋮", 23f, true, muted).apply {
            gravity = Gravity.CENTER
            contentDescription = "Note actions"
            setOnClickListener { noteMenu(this, item) }
        }, LinearLayout.LayoutParams(dp(38), dp(48)))
        addCard(row)
    }

    private fun addCard(view: View) {
        listContainer.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }

    private fun taskMenu(anchor: View, item: TaskRecord) {
        val menu = PopupMenu(this, anchor)
        if (category == "active") {
            menu.menu.add("Edit")
            menu.menu.add("Archive")
            menu.menu.add("Move to Trash")
        } else if (category == "archive") {
            menu.menu.add("Edit")
            menu.menu.add("Unarchive")
            menu.menu.add("Move to Trash")
        } else {
            menu.menu.add("Restore")
            menu.menu.add("Delete permanently")
        }
        menu.setOnMenuItemClickListener {
            when (it.title.toString()) {
                "Edit" -> editTask(item)
                "Archive" -> mutate(item.id, false, "archive")
                "Unarchive" -> mutate(item.id, false, "unarchive")
                "Move to Trash" -> mutate(item.id, false, "trash")
                "Restore" -> mutate(item.id, false, "restore")
                "Delete permanently" -> confirmPurge(item.id, false)
            }
            true
        }
        menu.show()
    }

    private fun noteMenu(anchor: View, item: NoteRecord) {
        val menu = PopupMenu(this, anchor)
        when (category) {
            "active" -> { menu.menu.add("Edit"); menu.menu.add("Archive"); menu.menu.add("Move to Trash") }
            "archive" -> { menu.menu.add("Edit"); menu.menu.add("Unarchive"); menu.menu.add("Move to Trash") }
            else -> { menu.menu.add("Restore"); menu.menu.add("Delete permanently") }
        }
        menu.setOnMenuItemClickListener {
            when (it.title.toString()) {
                "Edit" -> editNote(item)
                "Archive" -> mutate(item.id, true, "archive")
                "Unarchive" -> mutate(item.id, true, "unarchive")
                "Move to Trash" -> mutate(item.id, true, "trash")
                "Restore" -> mutate(item.id, true, "restore")
                "Delete permanently" -> confirmPurge(item.id, true)
            }
            true
        }
        menu.show()
    }

    private fun confirmPurge(id: String, note: Boolean) {
        AlertDialog.Builder(this).setTitle("Delete permanently?")
            .setMessage("This item cannot be recovered after permanent deletion.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> mutate(id, note, "purge") }
            .show()
    }

    private fun mutate(id: String, note: Boolean, operation: String) {
        worker.execute {
            try {
                val ok = repository.changeState(id, note, operation)
                runOnUiThread {
                    if (ok) { refresh(); notifyWidget() }
                    else showError("This item was not changed.")
                }
            } catch (e: Exception) {
                runOnUiThread { showError("Unable to change item.") }
            }
        }
    }

    private fun changeTask(id: String) {
        worker.execute {
            try {
                val ok = repository.toggleTask(id)
                runOnUiThread {
                    if (ok) { refresh(); notifyWidget() }
                    else showError("Already completed today, or task unavailable.")
                }
            } catch (e: Exception) {
                runOnUiThread { showError("Could not update task.") }
            }
        }
    }

    private fun editTask(item: TaskRecord?) {
        val form = vertical().apply { setPadding(dp(22), dp(5), dp(22), 0) }
        val title = EditText(this).apply {
            hint = "Task"
            setSingleLine(true)
            setText(item?.title ?: "")
        }
        val detail = EditText(this).apply {
            hint = "Details (optional)"
            minLines = 2
            maxLines = 5
            setText(item?.details ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        form.addView(title)
        form.addView(detail)
        val choices = listOf("Does not repeat", "Daily", "Weekdays", "Weekly")
        val codes = listOf("NONE", "DAILY", "WEEKDAYS", "WEEKLY")
        form.addView(label("Repeat", 13f, true, muted))
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices)
        spinner.setSelection(codes.indexOf(item?.repeatRule ?: "NONE").coerceAtLeast(0))
        form.addView(spinner)
        var due = item?.dueEpochDay
        val dueLabel = label("Due: " + (due?.let { LocalDate.ofEpochDay(it).toString() } ?: "Not set"), 14f)
        dueLabel.setPadding(0, dp(18), 0, dp(18))
        dueLabel.setOnClickListener {
            val starting = due?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
            DatePickerDialog(this, { _, year, month, day ->
                due = LocalDate.of(year, month + 1, day).toEpochDay()
                dueLabel.text = "Due: " + LocalDate.ofEpochDay(due!!).toString()
            }, starting.year, starting.monthValue - 1, starting.dayOfMonth).show()
        }
        form.addView(dueLabel)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (item == null) "New To-Do" else "Edit To-Do")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear date", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                due = null
                dueLabel.text = "Due: Not set"
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = title.text.toString()
                if (text.isBlank()) { title.error = "Task name required"; return@setOnClickListener }
                val description = detail.text.toString()
                val rule = codes[spinner.selectedItemPosition]
                worker.execute {
                    val ok = try { repository.saveTask(item?.id, text, description, due, rule) }
                    catch (e: Exception) { false }
                    runOnUiThread {
                        if (ok) { dialog.dismiss(); refresh(); notifyWidget() }
                        else showError("Task could not be saved. Your draft is still here.")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun editNote(item: NoteRecord?) {
        val form = vertical().apply { setPadding(dp(22), dp(5), dp(22), 0) }
        val title = EditText(this).apply { hint = "Title"; setSingleLine(true); setText(item?.title ?: "") }
        val body = EditText(this).apply {
            hint = "Write anything..."
            minLines = 5
            maxLines = 12
            gravity = Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(item?.body ?: "")
        }
        form.addView(title)
        form.addView(body)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (item == null) "New Note" else "Edit Note")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val noteTitle = title.text.toString()
                val noteBody = body.text.toString()
                if (noteTitle.isBlank() && noteBody.isBlank()) {
                    title.error = "Enter a title or note"
                    return@setOnClickListener
                }
                worker.execute {
                    val ok = try { repository.saveNote(item?.id, noteTitle, noteBody) }
                    catch (e: Exception) { false }
                    runOnUiThread {
                        if (ok) { dialog.dismiss(); refresh(); notifyWidget() }
                        else showError("Note could not be saved. Your draft is still here.")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun notifyWidget() = ObsidianWidgetProvider.updateAllWidgets(this)
    private fun showError(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
