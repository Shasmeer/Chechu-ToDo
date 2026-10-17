package com.obsidianwidget

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuickCaptureActivity : AppCompatActivity() {

    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_quick_capture)

        vaultManager = VaultManager(this)

        val isTodo = intent.getBooleanExtra(ObsidianWidgetProvider.EXTRA_CAPTURE_TODO, false) ||
            intent.getBooleanExtra(ObsidianWidgetProvider.EXTRA_APPEND_TO_WIDGET, false)
        val widgetId = intent.getIntExtra(ObsidianWidgetProvider.EXTRA_WIDGET_ID, -1)
        val widgetVaultManager = if (widgetId >= 0) VaultManager(this, widgetId) else vaultManager
        val captureInput = findViewById<EditText>(R.id.capture_input)
        val captureTitle = findViewById<android.widget.TextView>(R.id.capture_title)
        val addToTopToggle = findViewById<Switch>(R.id.capture_add_to_top)
        val addToTopRow = findViewById<View>(R.id.capture_add_to_top_row)
        val sectionRow = findViewById<View>(R.id.capture_section_row)
        val sectionSpinner = findViewById<Spinner>(R.id.capture_section)
        val headings = if (isTodo) widgetVaultManager.getNoteHeadings() else emptyList()

        if (headings.isNotEmpty()) {
            sectionRow.visibility = View.VISIBLE
            val sectionNames = listOf("Whole note") + headings.map { heading ->
                "  ".repeat((heading.level - 1).coerceAtMost(3)) + heading.title
            }
            sectionSpinner.adapter = ArrayAdapter(
                this,
                R.layout.capture_section_item,
                sectionNames
            ).apply {
                setDropDownViewResource(R.layout.capture_section_dropdown_item)
            }
        }

        // Show/hide add-to-top toggle based on widget config
        if (widgetVaultManager.showAddToTop) {
            addToTopRow.visibility = View.VISIBLE
            addToTopToggle.isChecked = widgetVaultManager.addToTop
        }

        if (isTodo) {
            captureTitle.text = "New To-Do"
            captureInput.hint = "One task per line..."
            captureInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            captureInput.setSingleLine(false)
            captureInput.maxLines = 8
        } else {
            captureTitle.text = "New Note"
            captureInput.hint = "Capture a thought..."
        }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val text = captureInput.text.toString().trim()
            if (text.isBlank()) {
                finish()
                return@setOnClickListener
            }

            if (!vaultManager.isVaultConfigured) {
                Toast.makeText(this, R.string.vault_not_configured, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = if (isTodo) {
                widgetVaultManager.addToTop = addToTopToggle.isChecked
                val headingLineIndex = headings
                    .getOrNull(sectionSpinner.selectedItemPosition - 1)
                    ?.lineIndex
                widgetVaultManager.addWidgetTasks(text, headingLineIndex)
            } else {
                vaultManager.addToTop = addToTopToggle.isChecked
                widgetVaultManager.addWidgetNotes(text)
            }
            if (success) {
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
                ObsidianWidgetProvider.updateAllWidgets(this)
                finish()
            } else {
                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_LONG).show()
                // Keep the unsaved text and dialog open so the user can retry.
            }
        }

        // Handle shared text from other apps
        if (intent?.action == android.content.Intent.ACTION_SEND) {
            intent.getStringExtra(android.content.Intent.EXTRA_TEXT)?.let {
                captureInput.setText(it)
            }
        }

        captureInput.requestFocus()
        captureInput.setSelection(captureInput.text.length)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}
