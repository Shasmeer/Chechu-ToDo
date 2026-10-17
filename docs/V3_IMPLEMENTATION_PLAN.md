# Chechu V3 — Implementation and release gates

Status: architecture stage, not a finished feature. Base: V2 unmerged branch. Do not merge into main without passing gates.

## Non-negotiable
- Never modify original Obsidian Markdown during migration/import.
- Back up data before destructive changes; use a recoverable Trash.
- Do not silently split existing plain-text lines into independent notes.
- Existing widgets continue to function while the new repository is introduced.
- No AI, accounts, remote storage or API keys in this phase.

## Stage 0 — immediate reliability fixes
1. Make null output streams fail; preserve draft when save fails.
2. For checklist toggles, guard against stale line-index targeting using expected original text and current file identity; on conflict refuse to write and refresh.
3. Add tests for file I/O failure, concurrent edits, and existing Markdown preservation.

## Stage 1 — local structured model
- Room entities: Task(id UUID, title, details, dueAt, createdAt, updatedAt, completedAt, archivedAt, deletedAt, revision, recurrenceRuleId), Note(id UUID, title, body, createdAt, updatedAt, archivedAt, deletedAt, revision), TaskOccurrence(id UUID, taskId, scheduledFor, completedAt), RecurrenceRule(id UUID, frequency, interval, weekdays, monthDay, timezone).
- DAO transactional updates and optimistic revision guards; indexes for active, due and archived queries.
- Repository boundary for widget, quick capture, and app screens. Room is canonical for *new* entries only after explicitly tested cutover; Obsidian remains untouched as legacy source.
- Explicit import preview and export with roundtrip tests, duplicate detection and no silent deletion.

## Stage 2 — full CRUD
- Main app list + detail editor; quick widget editor launched from item tap.
- Edit, archive, restore, soft delete, Trash recovery and permanent delete with confirmation.
- Notes preserve multiline formatting, blank lines and titles.
- No accidental checkbox toggle from note rows or action menus.

## Stage 3 — recurrence
- Daily, weekdays, selected weekdays, weekly, monthly, custom interval.
- Completion records occurrence, not template; idempotent next-occurrence creation.
- Time zone, DST, month-end and reboot tests.
- Deferrable scheduling via WorkManager where appropriate, not an exact-alarm promise.

## Stage 4 — widget experience
- Compact/medium/expanded responsive layouts, accessible targets, ellipsis action launcher, graceful empty states.
- Opaque cream/coral visual family, no Chechu branding in widget heading.
- Home-screen checkbox remains one tap; Edit/Delete/Archive/Repeat available from context action sheet.

## Acceptance gates
- No data loss on failed writes, migrations, reboot or app update.
- Stable IDs; no line-index destructive actions.
- Daily incomplete tasks persist across midnight.
- Archive is distinct from complete; Trash recoverable.
- All recurrence scenarios deterministic and tested.
- CI debug APK passes, then Samsung One UI device UAT with screenshots.
- Main stays unchanged until explicit UAT approval.
