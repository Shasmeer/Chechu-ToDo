package com.obsidianwidget

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.obsidianwidget.local.ChechuDatabase
import com.obsidianwidget.local.LocalRepository
import com.obsidianwidget.local.NoteRecord
import com.obsidianwidget.local.TaskRecord

/**
 * Real local task and note manager. No Obsidian data is silently imported or rewritten.
 * All Room I/O executes off the UI thread.
 */
class MainActivity : AppCompatActivity() {
    private enum class Tab { TASKS, NOTES }
    private enum class Folder { ACTIVE, ARCHIVE, TRASH }
    private var tab = Tab.TASKS
    private var folder = Folder.ACTIVE
    private lateinit var rows: LinearLayout
    private lateinit var subtitle: TextView
    private lateinit var section: TextView
    private lateinit var folderButton: Button
    private lateinit var tasksButton: Button
    private lateinit var notesButton: Button
    private var loadSerial = 0

    private data class Snapshot(
        val tasks: List<TaskRecord>,
        val notes: List<NoteRecord>,
        val completed: Map<String, Boolean>,
        val repeats: Map<String, String?>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rows = findViewById(R.id.home_items)
        subtitle = findViewById(R.id.home_subtitle)
        section = findViewById(R.id.home_section_title)
        folderButton = findViewById(R.id.home_folder)
        tasksButton = findViewById(R.id.home_tasks)
        notesButton = findViewById(R.id.home_notes)
        tasksButton.setOnClickListener { tab = Tab.TASKS; updateTabs(); reload() }
        notesButton.setOnClickListener { tab = Tab.NOTES; updateTabs(); reload() }
        folderButton.setOnClickListener { showFolderMenu() }
        findViewById<Button>(R.id.home_add_task).setOnClickListener { editTask(null) }
        findViewById<Button>(R.id.home_add_note).setOnClickListener { editNote(null) }
        updateTabs()
        reload()
    }

    override fun onResume() {
        super.onResume()
        if (::rows.isInitialized) reload()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun notifyUser(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun updateTabs() {
        tasksButton.setBackgroundResource(if (tab == Tab.TASKS) R.drawable.today_segment_active else R.drawable.today_segment_inactive)
        notesButton.setBackgroundResource(if (tab == Tab.NOTES) R.drawable.today_segment_active else R.drawable.today_segment_inactive)
        val selected = getColor(R.color.obsidian_text)
        val muted = getColor(R.color.obsidian_text_secondary)
        tasksButton.setTextColor(if (tab == Tab.TASKS) selected else muted)
        notesButton.setTextColor(if (tab == Tab.NOTES) selected else muted)
    }

    private fun showFolderMenu() {
        val labels = arrayOf("Active", "Archive", "Trash")
        AlertDialog.Builder(this).setTitle("Show").setItems(labels) { _, selected ->
            folder = Folder.entries[selected]
            reload()
        }.show()
    }

    private fun reload() {
        val request = ++loadSerial
        val currentTab = tab
        val currentFolder = folder
        section.text = when (currentFolder) {
            Folder.ACTIVE -> "Active"
            Folder.ARCHIVE -> "Archived"
            Folder.TRASH -> "Trash"
        }
        folderButton.text = "${section.text}  ▾"
        subtitle.text = "Local and private · Your Obsidian vault remains unchanged"
        LocalRepository.async(this, { db ->
            val dao = db.records()
            val tasks = if (currentTab == Tab.TASKS) when (currentFolder) {
                Folder.ACTIVE -> dao.activeTasks()
                Folder.ARCHIVE -> dao.archivedTasks()
                Folder.TRASH -> dao.trashedTasks()
            } else emptyList()
            val notes = if (currentTab == Tab.NOTES) when (currentFolder) {
                Folder.ACTIVE -> dao.activeNotes()
                Folder.ARCHIVE -> dao.archivedNotes()
                Folder.TRASH -> dao.trashedNotes()
            } else emptyList()
            Snapshot(tasks, notes,
                tasks.associate { it.id to LocalRepository.isCompleteToday(db, it) },
                tasks.associate { it.id to LocalRepository.nextDueLabel(db, it) })
        }) { result ->
            if (request != loadSerial) return@async
            result.onSuccess { render(it) }.onFailure { notifyUser("Could not open local data: ${it.message}") }
        }
    }

    private fun render(snapshot: Snapshot) {
        rows.removeAllViews()
        if (snapshot.tasks.isEmpty() && snapshot.notes.isEmpty()) {
            val message = when (folder) {
                Folder.ACTIVE -> if (tab == Tab.TASKS) "No tasks yet. Add your first to-do below." else "No notes yet. Add a thought below."
                Folder.ARCHIVE -> "Nothing archived yet."
                Folder.TRASH -> "Trash is empty."
            }
            rows.addView(label(message, 14, getColor(R.color.obsidian_text_secondary)).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(75), dp(12), dp(75))
            })
        }
        snapshot.tasks.forEach { item ->
            rows.addView(taskRow(item, snapshot.completed[item.id] == true, snapshot.repeats[item.id]))
        }
        snapshot.notes.forEach { item -> rows.addView(noteRow(item)) }
    }

    private fun label(content: String, size: Int, color: Int): TextView = TextView(this).apply {
        text = content
        textSize = size.toFloat()
        setTextColor(color)
        fontFamily = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(13), dp(13), dp(13), dp(13))
        setBackgroundResource(R.drawable.today_row)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) }
    }

    private fun body(title: String, details: String, complete: Boolean): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 15, if (complete) getColor(R.color.obsidian_text_secondary) else getColor(R.color.obsidian_text)).apply {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (complete) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                maxLines = 3
            })
            if (details.isNotBlank()) addView(label(details, 12, getColor(R.color.obsidian_text_secondary)).apply {
                maxLines = 4
                setPadding(0, dp(3), 0, 0)
            })
        }

    private fun taskRow(item: TaskRecord, done: Boolean, repeat: String?): View {
        val row = card()
        val check = label(if (done) "● ✓" else "◯", 21,
            if (done) getColor(R.color.obsidian_primary) else getColor(R.color.obsidian_text)).apply {
            setPadding(0, 0, dp(12), 0)
            contentDescription = if (done) "Mark incomplete" else "Mark complete"
            setOnClickListener {
                if (folder != Folder.ACTIVE) return@setOnClickListener
                change("Cannot update task") { LocalRepository.toggle(it, item.id) }
            }
        }
        row.addView(check)
        val descriptive = listOfNotNull(item.details.takeIf(String::isNotBlank), repeat).joinToString("\n")
        val center = body(item.title, descriptive, done)
        center.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        center.setOnClickListener { if (folder == Folder.ACTIVE) editTask(item) else taskMenu(item) }
        row.addView(center)
        row.addView(label("⋮", 24, getColor(R.color.obsidian_text_secondary)).apply {
            contentDescription = "Task actions"
            setPadding(dp(12), dp(3), dp(2), dp(3))
            setOnClickListener { taskMenu(item) }
        })
        return row
    }

    private fun noteRow(item: NoteRecord): View {
        val row = card()
        val center = body(item.title, item.body, false)
        center.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        center.setOnClickListener { if (folder == Folder.ACTIVE) editNote(item) else noteMenu(item) }
        row.addView(center)
        row.addView(label("⋮", 24, getColor(R.color.obsidian_text_secondary)).apply {
            contentDescription = "Note actions"
            setPadding(dp(12), dp(3), dp(2), dp(3))
            setOnClickListener { noteMenu(item) }
        })
        return row
    }

    private fun <T> change(errorMessage: String, operation: (ChechuDatabase) -> T) {
        LocalRepository.async(this, operation) { result ->
            result.onSuccess {
                if (it == false || it == 0) notifyUser("This item changed. Refresh and try again.")
                else {
                    reload()
                    ObsidianWidgetProvider.updateAllWidgets(this)
                }
            }.onFailure { notifyUser("$errorMessage: ${it.message}") }
        }
    }

    private fun editorFields(title: String, detail: String, isNote: Boolean): Triple<LinearLayout, EditText, EditText> {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(5), dp(20), dp(6))
        }
        val head = EditText(this).apply {
            hint = if (isNote) "Title" else "What needs doing?"
            setSingleLine(true)
            setText(title)
            textSize = 16f
        }
        val body = EditText(this).apply {
            hint = if (isNote) "Write your note..." else "Details (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = if (isNote) 5 else 2
            maxLines = 10
            gravity = Gravity.TOP
            setText(detail)
            textSize = 14f
        }
        container.addView(head)
        container.addView(body)
        return Triple(container, head, body)
    }

    private fun editTask(item: TaskRecord?) {
        val (layout, title, details) = editorFields(item?.title.orEmpty(), item?.details.orEmpty(), false)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (item == null) "New To-Do" else "Edit To-Do")
            .setView(layout).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = title.text.toString().trim()
                if (text.isEmpty()) { title.error = "Enter a task"; return@setOnClickListener }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                LocalRepository.async(this, { db ->
                    if (item == null) { LocalRepository.addTask(db, text, details.text.toString()); true }
                    else db.records().editTask(item.id, text, details.text.toString(), item.revision, System.currentTimeMillis()) == 1
                }) { result ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    result.onSuccess {
                        if (it) { dialog.dismiss(); reload(); ObsidianWidgetProvider.updateAllWidgets(this) }
                        else notifyUser("Task changed elsewhere. Copy your text and retry.")
                    }.onFailure { notifyUser("Could not save: ${it.message}") }
                }
            }
        }
        dialog.show()
    }

    private fun editNote(item: NoteRecord?) {
        val (layout, title, body) = editorFields(item?.title.orEmpty(), item?.body.orEmpty(), true)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (item == null) "New Note" else "Edit Note")
            .setView(layout).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val heading = title.text.toString().trim()
                val text = body.text.toString()
                if (heading.isEmpty() && text.isBlank()) { title.error = "Write a title or note"; return@setOnClickListener }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                LocalRepository.async(this, { db ->
                    if (item == null) { LocalRepository.addNote(db, heading, text); true }
                    else db.records().editNote(item.id, heading.ifEmpty { "Untitled" }, text, item.revision, System.currentTimeMillis()) == 1
                }) { result ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    result.onSuccess {
                        if (it) { dialog.dismiss(); reload(); ObsidianWidgetProvider.updateAllWidgets(this) }
                        else notifyUser("Note changed elsewhere. Copy your text and retry.")
                    }.onFailure { notifyUser("Could not save: ${it.message}") }
                }
            }
        }
        dialog.show()
    }

    private fun taskMenu(task: TaskRecord) {
        val options = when (folder) {
            Folder.ACTIVE -> arrayOf("Edit", "Repeat", "Archive", "Move to Trash")
            Folder.ARCHIVE -> arrayOf("Restore to Active", "Move to Trash")
            Folder.TRASH -> arrayOf("Restore", "Delete permanently")
        }
        AlertDialog.Builder(this).setTitle(task.title).setItems(options) { _, index ->
            when (folder) {
                Folder.ACTIVE -> when (index) {
                    0 -> editTask(task)
                    1 -> repeatMenu(task)
                    2 -> change("Archive failed") { it.records().archiveTask(task.id, System.currentTimeMillis(), task.revision, System.currentTimeMillis()) }
                    3 -> confirm("Move to Trash?", task.title) {
                        change("Delete failed") { it.records().trashTask(task.id, System.currentTimeMillis(), task.revision, System.currentTimeMillis()) }
                    }
                }
                Folder.ARCHIVE -> when (index) {
                    0 -> change("Restore failed") { it.records().archiveTask(task.id, null, task.revision, System.currentTimeMillis()) }
                    1 -> confirm("Move to Trash?", task.title) {
                        change("Delete failed") { it.records().trashTask(task.id, System.currentTimeMillis(), task.revision, System.currentTimeMillis()) }
                    }
                }
                Folder.TRASH -> when (index) {
                    0 -> change("Restore failed") { it.records().trashTask(task.id, null, task.revision, System.currentTimeMillis()) }
                    1 -> confirm("Permanently delete task?", "This cannot be undone.") {
                        change("Permanent deletion failed") { it.records().purgeTask(task.id) }
                    }
                }
            }
        }.show()
    }

    private fun noteMenu(note: NoteRecord) {
        val options = when (folder) {
            Folder.ACTIVE -> arrayOf("Edit", "Archive", "Move to Trash")
            Folder.ARCHIVE -> arrayOf("Restore to Active", "Move to Trash")
            Folder.TRASH -> arrayOf("Restore", "Delete permanently")
        }
        AlertDialog.Builder(this).setTitle(note.title).setItems(options) { _, index ->
            when (folder) {
                Folder.ACTIVE -> when (index) {
                    0 -> editNote(note)
                    1 -> change("Archive failed") { it.records().archiveNote(note.id, System.currentTimeMillis(), note.revision, System.currentTimeMillis()) }
                    2 -> confirm("Move to Trash?", note.title) {
                        change("Delete failed") { it.records().trashNote(note.id, System.currentTimeMillis(), note.revision, System.currentTimeMillis()) }
                    }
                }
                Folder.ARCHIVE -> when (index) {
                    0 -> change("Restore failed") { it.records().archiveNote(note.id, null, note.revision, System.currentTimeMillis()) }
                    1 -> confirm("Move to Trash?", note.title) {
                        change("Delete failed") { it.records().trashNote(note.id, System.currentTimeMillis(), note.revision, System.currentTimeMillis()) }
                    }
                }
                Folder.TRASH -> when (index) {
                    0 -> change("Restore failed") { it.records().trashNote(note.id, null, note.revision, System.currentTimeMillis()) }
                    1 -> confirm("Permanently delete note?", "This cannot be undone.") {
                        change("Permanent deletion failed") { it.records().purgeNote(note.id) }
                    }
                }
            }
        }.show()
    }

    private fun repeatMenu(task: TaskRecord) {
        val options = arrayOf("Never", "Every day", "Weekdays (Mon–Fri)", "Every week", "Every month", "Custom every N days", "Choose weekdays")
        AlertDialog.Builder(this).setTitle("Repeat task").setItems(options) { _, selected ->
            when (selected) {
                0 -> change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "NONE") }
                1 -> change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "DAILY") }
                2 -> change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "WEEKDAYS", daysMask = 0b0011111) }
                3 -> change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "WEEKLY") }
                4 -> change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "MONTHLY") }
                5 -> {
                    val field = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; hint = "2"; setText("2") }
                    AlertDialog.Builder(this).setTitle("Repeat every how many days?")
                        .setView(field).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                            val interval = field.text.toString().toIntOrNull()
                            if (interval == null || interval !in 1..365) { notifyUser("Enter a number from 1 to 365"); return@setPositiveButton }
                            change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "CUSTOM", interval = interval) }
                        }.show()
                }
                6 -> {
                    val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    val chosen = booleanArrayOf(true, true, true, true, true, false, false)
                    AlertDialog.Builder(this).setTitle("Repeat on")
                        .setMultiChoiceItems(days, chosen) { _, which, checked -> chosen[which] = checked }
                        .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                            var mask = 0
                            chosen.forEachIndexed { idx, set -> if (set) mask = mask or (1 shl idx) }
                            if (mask == 0) { notifyUser("Choose at least one day"); return@setPositiveButton }
                            change("Repeat failed") { LocalRepository.setRecurrence(it, task.id, "WEEKDAYS", daysMask = mask) }
                        }.show()
                }
            }
        }.show()
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setNegativeButton("Cancel", null).setPositiveButton("Continue") { _, _ -> action() }.show()
    }
}
