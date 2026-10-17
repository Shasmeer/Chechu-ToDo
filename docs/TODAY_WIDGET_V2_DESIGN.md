# Today Widget V2 — Approved visual baseline

## Intent
Replaces the experimental semi-transparent Liquid Glass home-screen widget with a refined **opaque** cream/white productivity card. The widget must **not** display the Chechu-ToDo product name in the header. Brand identity remains in the installed Android app and launcher label, not the widget surface.

This document tracks the approved user-supplied premium minimal UI reference and subsequent unbranded mockup.

## Style tokens
| Token | Target |
|---|---|
| Outer widget | Warm white `#FFFDFC`, 28dp corners, 1dp pale border |
| Task/Note rows | Pale neutral `#F5F5F4`, 20dp corners |
| Primary text | Near black `#202125` |
| Secondary text | Mid gray `#777982` |
| Coral accent | `#FF785C` |
| Selected tab | Muted pale coral `#FFE3D8` |
| Action sizes | 34dp header controls; 44dp quick-capture buttons |
| Fonts | System sans-serif, bold restrained; 23sp main heading, 13–14sp content |
| Chrome | Minimal; no fake AI call-to-action or non-functional overflow affordances |

## Widget hierarchy
1. **Today** heading, small subtitle / remaining-task count; refresh & settings on the right.
2. **To-Do | Notes** genuine segmented navigation, per widget.
3. Individual list rows; tasks show circular checkboxes and strikethrough after toggle; notes use multiline readable text, one rendered row per nonempty source line.
4. Footer: `+ To-Do` saves task-formatted Markdown; `Note` saves plain Markdown text.

## Data preservation
- Existing Obsidian daily/pinned Markdown files are **not bulk-rewritten** to convert historical text into tasks.
- Task creation explicitly writes `- [ ] ...` for each nonempty entered line.
- Note creation writes plain text (no checkbox marker) to the active selected file.
- Existing checklist toggling updates the clicked Markdown line by line index.
- A historical single line containing several phrases cannot be automatically split reliably; users can edit it in Obsidian. The new capture UI prevents this for separate lines.

## Deferred
AI functionality (API keys, backend, AI-generated task classification) is future work. Never embed provider keys in Android code, resources, commits or APKs. Real AI integration requires a secure gateway or a safe user-managed key architecture.

## UAT and release gate
- Confirm Android debug build success with downloadable Actions artifact.
- Samsung One UI launcher: tab switching, completed-task toggle, note capture, task capture, update refresh, responsive list wrapping, resizing.
- Confirm transparent/opaque card appearance, font legibility and status of existing vault settings.
- Test existing daily and pinned notes without data loss; test no-vault empty state.
- Do not merge or publish until the user has reviewed screenshots and device behaviour.
