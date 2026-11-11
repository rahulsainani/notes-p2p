# Ordering

Notes in a vault and items in a checklist have an order that the person who uses the app sets by
drag. Each entry stores one order key; no list object exists. Code:
`core/src/commonMain/kotlin/app/droidster/notesp2p/core/order/`.

## Order keys

A key is a string such as `a0V`. The display order is a plain string sort by `(key, ID)`. A move
writes one new key between the keys of the two new neighbours, so it changes one entry.

A key is a number in base 62 (digits `0-9`, `A-Z`, `a-z`, in that order), written in three parts:

| Part         | In `a0V` | Meaning                                                                       |
|--------------|----------|-------------------------------------------------------------------------------|
| Head         | `a`      | How many digits the whole number has                                          |
| Whole number | `0`      | Counts up for each new entry at the end, down for each new entry at the start |
| Fraction     | `V`      | Optional. Puts the key between two whole numbers: `a0V` is about 0.5          |

The head is needed because a plain string sort compares character by character: without it, `10`
sorts before `9`. Each head letter stands for one digit count, and the letters sort in the order of
the numbers:

| Head | Digits        | Keys, smallest to largest |
|------|---------------|---------------------------|
| `Y`  | 2, below zero | `Y00` … `Yzz`             |
| `Z`  | 1, below zero | `Z0` … `Zz`               |
| `a`  | 1             | `a0` … `az`               |
| `b`  | 2             | `b00` … `bzz`             |

So after `az` comes `b00`, and `az` < `b00` in a string sort too. A key gets one character longer
only every 62 entries at the end or at the start. The scheme is the one of
[rocicorp/fractional-indexing](https://github.com/rocicorp/fractional-indexing); `FractionalIndexTest`
checks its reference cases.

The table shows the keys with the middle of each gap, `FractionalIndex.between(after, before)`:

| Action                           | Keys in display order |
|----------------------------------|-----------------------|
| First note                       | `a0`                  |
| New note, shown first            | `Zz`, `a0`            |
| New note, shown first            | `Zy`, `Zz`, `a0`      |
| `Zy` moves between `Zz` and `a0` | `Zz`, `ZzV`, `a0`     |

The app does not take the middle. It takes a random point in the gap (see "Equal keys"), so its keys
have a few more characters: three new notes, each shown first, get keys such as `Zx3e`, `ZxGl`,
`ZztO`.

Repeated moves into one gap add about one character per four moves. `OrderKeysTest` moves 600 times
into the gap that the move before left, and the longest key stays below 200 characters.

## Why not an Automerge list

|                                 | Order key per entry            | List of IDs                                           |
|---------------------------------|--------------------------------|-------------------------------------------------------|
| Move                            | One field write                | Delete + insert: Automerge has no move                |
| Two devices move the same entry | One key wins everywhere        | The entry can appear twice                            |
| Archive, trash, restore         | No order change; the key stays | The entry must stay in the list, or its place is lost |
| Shared state                    | None; each note holds its key  | One document that every move changes                  |

## Equal keys

With the middle of the gap, two devices that fill the same gap offline compute the same key. Three
measures handle that:

| Measure                                                                               | Where                                            |
|---------------------------------------------------------------------------------------|--------------------------------------------------|
| A new key is a random point in the gap, so two devices almost never make the same key | `FractionalIndex.between(after, before, random)` |
| Equal keys sort by ID, so every device shows the same order                           | `orderComparator`                                |
| A move between two entries with one key also gives the neighbour a new key            | `OrderKeys.place`                                |

## Scope and views

- A note's key orders it among all notes of its vault. Pinned and other notes share the key space.
- A view is a filter on the same keys: a label view shows a subset in the same relative order.
- The Archive and the Trash do not touch the key, so an unarchived or restored note returns to the
  place of its key.
- `NoteStore.moveNote(id, afterId, beforeId)` takes the visible neighbours and reads the keys of all
  notes, hidden ones included. A new key therefore never repeats the key of a hidden note.
- A move in a filtered view would jump over hidden notes; reorder belongs to the unfiltered list.
- A move that must also change a neighbour from a newer app (`Note.isReadOnly`) throws
  `NoteReadOnlyException` and changes nothing.
