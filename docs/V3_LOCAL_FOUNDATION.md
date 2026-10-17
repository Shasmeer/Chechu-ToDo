# Chechu V3 — Offline task and note management foundation

## Purpose
Move beyond the V2 single Markdown-file widget into an offline, editable productivity tool while preserving the existing Obsidian integration. **This is an additive, opt-in architecture, not a vault migration.**

## User-visible scope
- App Home: Today dashboard, To-Do and Notes tabs, Active / Archive / Trash filters, edit forms, add forms, and per-row actions.
- Tasks: stable UUID, editable title/details/due date, mark complete, daily/weekday/weekly recurrence, archive/unarchive, move to Trash, restore, permanent delete with confirmation.
- Notes: stable UUID, editable title and multiline body (newlines retained), archive/unarchive, Trash and restore.
- Recurrence: future occurrence calculated deterministically, each completion logged. A repeating task remains active; it is not permanently checked. Duplicate completion within the same local calendar day is rejected.
- Launcher: the same Today widget can show either V3 offline records or legacy Obsidian file data. The user selects 'Use offline To-Dos & Notes' in widget settings.
- Launcher item interaction: checkbox toggles task; task title/note content opens its editor; footer creates an offline task or note.
- Existing Obsidian-based widget settings remain unless the user chooses offline source. The app must not silently modify or import existing Markdown.

## Storage
Room database `chechu_v3.db`: `tasks`, `notes`, `task_completions`.
- PKs are random UUIDs; never use Markdown line numbers as V3 identifiers.
- Archived and deleted records are retained, excluded from active lists.
- Purge requires an explicit confirmation and only applies to already-trashed records.
- All database I/O is off the main thread, using a single worker in MainActivity and an asynchronous launcher-update path.

## Import/migration boundary
Existing Obsidian vault files must remain untouched on V3 install and during source switching. The legacy path remains available until a separate migration/import workflow is reviewed and tested. No automatic migration is implemented.

## Non-goals of this foundation
- No AI, network sync or API keys.
- No scheduled device notifications or precise alarm delivery.
- No custom selected-day recurrence or fully featured calendar view.
- No widget-native inline menu for destructive actions; open the item editor / management app first.
- No background import or reconciliation with Obsidian changes.

## Release gates
1. `:app:testDebugUnitTest` including recurrence edge cases and `:app:assembleDebug` pass.
2. UAT on Samsung One UI: create task/note, reopen and edit, check/uncheck, archive, Trash, restore and confirm permanent deletion.
3. Recurrence: Friday-to-Monday weekday schedule; Sunday completion; repeated tap must not duplicate same-day occurrence.
4. Widget: enabled offline source displays local items; tabs, checkbox and editor deep links work. Switch back to Obsidian source and verify original content.
5. Install over V2 and ensure Obsidian files remain unchanged.
6. Keep draft PR unmerged until manual UAT and data-safety sign-off.

## Risks and follow-up
- Old V2 Markdown notes are line-based: V3 does **not** infer distinct notes from them. Explicit import mapping is future work.
- Recurrence uses device-local dates; timezone-change and DST behavior require additional UAT.
- Room schema migrations must be explicit for all future version changes; never use destructive fallback.
