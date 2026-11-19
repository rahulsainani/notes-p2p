# Note model

One note is one Automerge document. The app reads it into an immutable `Note` and changes it through
a `NoteEditor`, inside `NoteStore.editNote` ([NoteStore.md](NoteStore.md)). Code: the public
package `core.note`, and the internal package `core.internal.automerge` for the document format.

## The document

```
minReaderVersion, minWriterVersion   int, both 1 in release 1
title          text
body           text, with marks
kind           "text" | "checklist"
items          map: item ID -> { text: text with marks, checked: boolean, order: string }
colour         a stored colour ID, e.g. "oat", "butter" (Lemon), "tomato"; see below
pinned, archived, checkedItemsHidden   boolean
order          string            the note's order key in its vault
labels         map: label ID -> true
createdAt, updatedAt           int, milliseconds since the epoch
deletedAt      int | null        set = in the Trash
restored       map: a deletedAt value that a restore undid -> true
```

A mark is a format on a range of a text: `bold`, `italic`, `underline`, `strikethrough` = `true`;
`heading` = `1` or `2`, on the body only. Every mark grows with text typed at its end. A heading is
such a range too, not a line: it can cover several lines, and a new line typed at its end is in the
heading until the heading is cleared there. Automerge also has block markers for this; neither
binding in use exposes them.

## Types

```kotlin
Note(
    id, title,
    content = NoteContent.Checklist(items = listOf(ChecklistItem(id, FormattedText("Milk"), checked, order))),
    status = NoteStatus.Trashed(since = trashTime, fromArchive = false),
    ...
)
```

| Document value                                   | Kotlin type                                                                                    |
|--------------------------------------------------|------------------------------------------------------------------------------------------------|
| `kind` + `body` or `items`, `checkedItemsHidden` | `Note.content`: `NoteContent.Text(body)` or `NoteContent.Checklist(items, checkedItemsHidden)` |
| a text with marks                                | `FormattedText(text, spans)`                                                                   |
| `archived` + `deletedAt`                         | `Note.status`: `NoteStatus.Active`, `Archived` or `Trashed(since, fromArchive)`                |
| `colour`, `kind`                                 | `NoteColour`, `NoteKind` enums                                                                 |
| IDs                                              | `NoteId`, `VaultId`, `ItemId`, `LabelId` value classes                                         |
| `order`                                          | `OrderKey` ([Ordering.md](Ordering.md))                                                        |
| document state                                   | `Note.version`: `DocVersion` ([NoteStore.md](NoteStore.md))                                    |
| times                                            | `kotlin.time.Instant`                                                                          |
| `restored`                                       | none: only the store reads it ([TrashAndPurge.md](TrashAndPurge.md))                           |

- A note holds a text or a checklist, never both. `setKind` moves the content to the other kind, so
  the other kind is empty. It holds content only when another device added some at the same time as
  the switch (see Limits); that content shows after a switch back.
- A `FormattedText` checks each span: `0 ≤ start < end ≤ text length`.
- A trashed note keeps its archive state, so a restore returns it to the Archive.
- The document stores a colour ID, never a colour value, so each theme picks its values. Stored IDs
  never change ([SchemaChanges.md](SchemaChanges.md)); ten of them come from the first colour set:

  | Colour          | Stored ID | Colour | Stored ID | Colour | Stored ID |
  |-----------------|-----------|--------|-----------|--------|-----------|
  | Oat (new notes) | `oat`     | Grape  | `plum`    | Lime   | `lime`    |
  | Lemon           | `butter`  | Iris   | `iris`    | Cocoa  | `cocoa`   |
  | Tangerine       | `apricot` | Sky    | `sky`     | Slate  | `stone`   |
  | Tomato          | `tomato`  | Teal   | `moss`    | Sage   | `sage`    |
  | Pink            | `rose`    | Mint   | `mint`    | Brick  | `clay`    |

  A new note stores the default colour (`NoteColour.DEFAULT`, Oat) as a real colour, not as "the
  default". A later change of the default then affects new notes only; no existing note changes colour.

- An ID is 1 to 128 of `A-Z`, `a-z`, `0-9`, `_`, `-`: IDs name files. A key in a document that is
  not a valid ID (a label or item from another program) stays out of the model.

Text positions in `FormattedText` and `NoteEditor` count UTF-16 units, as Kotlin strings do.
Automerge counts Unicode code points; `TextUnits.kt` in `core.text` converts at the boundary. An
emoji is one code point and two UTF-16 units.

## Rules that keep merges clean

- Every map and text object is made with the note, by one device; a later field is a scalar at the
  root. Two devices that each make `items` get two maps, and a merge shows only one.
- `items` is a map with an order key per item, not a list. Automerge has no list move; a move as
  delete + insert duplicates an item that two devices move.
- A text setter stores one splice: the range between the common start and end of the old and new
  text. A full replace deletes what another device typed in the same text.
- A setter that repeats the current value writes nothing. Automerge records every put as a change.
- `updatedAt` reads as the newest of all conflicting values. Automerge keeps both values of two
  devices as a conflict and shows one winner, not by time.
- Each stay in the Trash gets its own `deletedAt` value: a restore names the value it undid.

## Limits

- An item deleted on one device and edited on another stays deleted; the edit is lost.
- `setKind` moves content and formats between `body` and `items`; a heading does not go into an
  item. With two devices at the same time: an item added during the switch to text shows again in
  a checklist; both switch to text: the body holds the lines twice; both switch to a checklist: each
  line is two items.
- Concurrent sets of one scalar (`colour`, `pinned`) give one winner, the same on every device.

## Format versions

- `minReaderVersion` is the oldest format version that may read the note; `minWriterVersion` the
  oldest that may change it. Release 1 writes 1 for both. Two conflicting values read as the
  highest. Rules for raising them: [SchemaChanges.md](SchemaChanges.md).
- A note with a higher `minWriterVersion` than the app knows, or without both versions, is read-only
  (`Note.isReadOnly`); an edit throws `NoteReadOnlyException`. An older app therefore never writes
  into a newer shape. `NoteStore.mergeLabels` skips such a note.
- A note with a higher `minReaderVersion` is not shown at all. The device keeps its bytes and syncs
  them, so a newer app version can read them.
- A stored ID that the app does not know reads as a fixed value: a colour from a newer app as Oat, a
  kind as text. The fallback is never "the default colour", so a later change of the default does not
  change how stored notes look. The value in the document stays until someone sets a new one.
