# Tasks

## 1. Entry model

- [ ] 1.1 Add `zeroPrefix` to `OdometerEntry` (serialized with a default of false, invariant `zeroPrefix` implies `steps > 0`) with the press, backspace, clear, `withUnit` and `digits` rules from the design, and verify entry tests cover: 0 then 5 in a tenths unit reading 0.0, 0.5 and backspace reading 0.0 then empty; 0, 5, 3 reading 0.0, 0.5, 5.3 and three backspaces reading 0.5, 0.0, empty; the same in a whole-number unit (0, 5, 0 then empty); 0, 0, 0, 5 reading 0.0 then 0.5; 1 then 0 as an ordinary digit (0.1, 1.0, backspace 0.1); a pasted "0123" reading 12.3 with backspaces 1.2, 0.1, 0.0, empty; the prefix zero not counting towards the cap; the unit change dropping the prefix while a typed zero stays a typed zero; and the earlier sequences (1, 2, 3 and 1, 2, backspace, backspace, 2, 3, 0) unchanged
- [ ] 1.2 Verify `applyEdit` with prefixed text (`"05"` deleted to `"0"`, `"0"` extended to `"05"`, a second `"0"` typed on `"0"`, a paste of "0123" onto an empty entry) and that an entry serialized without the new field still deserializes, with tests

- [ ] 1.3 Audit every place an odometer is entered (search the source for odometer inputs and `KeyboardType.Number` fields) and verify each goes through `OdometerField` and `OdometerEntry` with no rule duplicated in a processor or screen; record the result (today only the add-vehicle form) in the design, and add the convention that every odometer input uses these shared pieces to the project context in `openspec/config.yaml`, then verify `openspec validate --all --strict` passes

## 2. Add screen and field

- [ ] 2.1 Add add-screen processor tests for the new sequences through the keyboard's text edits (0 then 5 then backspace twice, 0, 5, 3 with backspaces, extra zeros ignored, a typed zero still satisfying the required odometer and a prefixed entry saving its value) and verify `./gradlew :shared:allTests` passes
- [ ] 2.2 Verify on the emulator with the real number keyboard, in a tenths unit and a whole-number unit, that 0 then 5 draws 0.0 then 0.5 (0 then 5) and that backspace draws 0.0 (0) and then an empty field, and that the field never draws a doubled zero

## 3. Maestro and final verification

- [ ] 3.1 Extend the odometer Maestro flow with the zero-prefix sequences (`inputText` and `eraseText`) for a tenths unit and a whole-number unit, and verify the flow passes
- [ ] 3.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug`, the whole Maestro suite and `openspec validate --all --strict`, and verify all pass
