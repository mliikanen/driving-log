# Design

## Context

See proposal.md for motivation. Current state, from the archived `add-vehicles` change:

- `OdometerEntry(unit, steps: Long?)` holds the entry as one nullable count in the unit's step. `null` is empty, `0` is a
  typed zero. `press` computes `steps * 10 + digit`, so a leading zero disappears the moment another digit follows:
  the digits 0, 5 give `steps = 5`, the same as typing just 5.
- `backspace` divides by ten and gives `null` below ten, so from 0.5 it goes straight to empty and the typed zero is lost.
- `applyEdit(newText)` turns the system keyboard's text edits into `press` and `backspace` calls by comparing digit strings
  with `digits` (the value's digits, "" when empty). `OdometerField` shows `digits` as the raw text and draws the number
  through a `VisualTransformation`.

## Goals / Non-Goals

**Goals:**
- Backspace undoes exactly what was typed, including a first zero, one digit at a time.
- No visible change to what the field draws: the zero prefix is never an extra drawn digit.

**Non-Goals:**
- Changing the digit limit, the units, the stored value, or how readings are shown outside the entry field.

## Decisions

### 1. A `zeroPrefix` flag next to `steps`

`OdometerEntry` gains `zeroPrefix: Boolean = false` (serialized, defaulting to false so a state saved by an earlier build
still restores). Invariant: `zeroPrefix` implies `steps > 0`. The prefix is a fact about typing history, not about the
value, so it stays out of `steps` and all the conversion and formatting code keeps using `steps` alone.

`digits`, the text of the field, becomes `(if (zeroPrefix) "0" else "") + steps`, or `""` when empty. Typed 0, 5, 3 is the
text `"053"` while `steps` is 53 and the field draws 5.3 (tenths) or 53 (whole).

Alternative: keep the raw digit string as the state and derive `steps` from it. Rejected: every consumer would parse it
again and the digit cap and unit rescaling would have to work on strings.

### 2. The rules

| Entry (text) | Press | Result |
|---|---|---|
| empty | any digit `d` | `steps = d`, no prefix (a 0 is the typed zero) |
| `"0"` (typed zero) | 0 | unchanged, so there is at most one prefix zero |
| `"0"` (typed zero) | `d` from 1 to 9 | `steps = d`, `zeroPrefix = true`, text `"0d"` |
| any value above zero | any digit | `steps * 10 + d`, prefix unchanged, ignored past the cap |

`backspace`:

| Entry | Result |
|---|---|
| empty | empty |
| typed zero (`steps = 0`) | empty |
| `steps >= 10` | `steps / 10`, prefix unchanged |
| `steps < 10`, prefix set (`"0d"`) | typed zero: `steps = 0`, no prefix |
| `steps < 10`, no prefix | empty |

So typed 0, 5, 3 then backspace three times reads 5.3, 0.5, 0.0, empty; typed 1, 2, 3 (no first zero) still reads
1.2, 0.1, empty; and a pasted "0123" reads 12.3, 1.2, 0.1, 0.0, empty. `clear()` gives empty. The cap compares `steps` only,
so the prefix zero never uses up a digit of the limit.

`applyEdit` keeps working from digit strings, unchanged: the text `"05"` is the previous digits of a prefixed entry, so a
deletion to `"0"` becomes one `backspace`, and an append becomes `press`.

### 3. Unit change drops the prefix

`withUnit` rescales `steps` as before and clears the prefix (a value of zero stays a typed zero). A rescale re-expresses the
number, so the typing history no longer describes it: 0, 5 in Kilometers becomes 5.0 in a tenths unit, and backspace
then shortens that number as usual. Keeping a prefix through the rescale would only produce text like `"050"` that the
user never typed.

### 4. The field needs no new behavior

The raw text is `digits`, including the prefix zero, and the `VisualTransformation` already formats `text.toLongOrNull()`,
which reads `"05"` as 5. So the field draws 0.5 or 5 and the system keyboard sees one more character than is drawn, which
is what makes its backspace remove the prefix zero last. The cursor stays forced to the end. Accessibility announces the
formatted reading, not the raw text.

### 5. One implementation for every odometer input

The behavior lives entirely in two shared pieces: `OdometerEntry` (all the rules: press, backspace, clear, prefix, cap,
unit change, `applyEdit`) and `OdometerField` (the only composable that draws an odometer input). A screen's processor
holds an `OdometerEntry` in its state and forwards the field's text to `applyEdit`; it never re-implements a rule. Audit
of the code (task 1.3): `KeyboardType.Number` is used only inside `OdometerField`; the other text fields are the vehicle
name and plate; `OdometerField` has one call site, the add-vehicle form; and nothing outside `OdometerEntry` implements
an entry rule (the field only reads `digits` to draw, and unit conversion lives in `OdometerUnit`). So the add-vehicle form
is the only odometer input, it already works this way, and nothing needs to move. To keep it true for later inputs (refueling, trips, camera confirmation) the project context gains a convention:
every odometer input uses `OdometerField` and `OdometerEntry`. What a screen still decides for itself is only whether an
empty entry may be saved (required when adding a vehicle, possibly optional elsewhere).

Alternative: move `OdometerEntry` out of the `vehicle` package into a neutral one now. Deferred: it is one import change
to make when the second input arrives, and doing it now would touch code this change otherwise leaves alone.

## Risks / Trade-offs

- [The keyboard's text ("05") is longer than what is drawn ("5" or "0.5"), so what the IME thinks is in the field differs
  from what the user sees] → It is only ever edited at the end, the cursor is forced there, and `applyEdit` works on the
  digits it is given. Checked on the emulator with the real keyboard and with Maestro `inputText` and `eraseText`.
- [The prefix is invisible, so a user who types 0 then 5 and presses backspace sees 0.0 (or 0) appear] → That is the
  requested behavior, and the typed zero it returns to is exactly what they entered first.
- [A state saved by an older build has no prefix field] → It defaults to false, and an entry without a prefix is valid.
