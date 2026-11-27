# UX review panel

Six test personas that use KinetiFrame on a real phone, the way people would, and write a report.
An AI agent plays each persona. Another agent (or a person) reads the reports, checks each finding
against the screenshots and the code, and turns the confirmed ones into work.

The panel is small on purpose. Around five testers find most of the usability problems in one area,
and each extra tester finds less that's new. So every persona covers a different audience, task or
risk, and a run only calls the personas a change affects.

## The panel

| Persona | Brief | Why KF needs them | Run when |
|---|---|---|---|
| Impatient beginner critic | [beginner](personas/beginner.md) | First impressions decide whether people stay | UI or onboarding changes, every release |
| Experienced frame-by-frame animator | [expert](personas/expert.md) | Sets the bar for the core drawing workflow | Editor, timeline or tool changes |
| Teen short-form creator | [creator](personas/creator.md) | The largest audience for mobile animation: memes, lip sync, vertical video | Audio, packs, export or sharing changes |
| Rig and puppet animator | [rigger](personas/rigger.md) | Tests KF's bet on actors, skeletons and performances | Actor and Actor Studio changes |
| Accessibility and ergonomics tester | [access](personas/access.md) | Finds what the others can't see: labels, targets, contrast, large text | Every release, big UI changes |
| Production stress tester | [stress](personas/stress.md) | Lost work breaks trust faster than anything | Storage, format or performance changes, every release |

## How to run one

1. Install the build under test on a **test phone** or as a fresh install. Never run the panel on a
   phone that holds someone's real work.
2. Set the device and output folder, then check the helper works:

   ```sh
   export KF_DEVICE=<adb serial>          # from `adb devices`
   export KF_UX_OUT=notes/ux-review/2026-11-24/beginner
   source tools/ux-review/phone.sh && launch && shot smoke
   ```

3. Start an agent with the persona's brief, this README's rules and the
   [report template](report-template.md). Run personas one at a time, since they share the phone.
4. Each run writes `notes/ux-review/<date>/<persona>/report.md` with its screenshots beside it.
   `notes/` is ignored by Git.

## Rules for every run

- Use only the KinetiFrame app. Don't open other apps, the gallery, Android's photo or file pickers,
  notifications or phone settings, and don't rotate the phone unless the brief says so. Back out of
  anything that opens a system screen, and delete any screenshot that shows personal content.
- Before the first action, check the phone has been idle for at least 20 seconds (`idle_ms`). If it's
  in use, wait and check again up to three times, then stop and say so.
- Name everything you create with the persona's prefix, for example "Beginner review". Don't delete,
  archive or change anything you didn't create.
- Stay within the brief's action budget. Use `labels` to find things, and screenshots after each
  meaningful step. Every finding cites a screenshot.
- Report only what you saw. Mark anything you're not sure of as "unsure", and note when a problem
  may come from scripted input rather than the app.
- Don't edit files other than the report, and don't build or install anything.

## After a round

The agent that synthesises the round:

1. reads every report and checks each finding against its screenshots and, where it can, the code;
2. merges findings into `notes/ux-review/findings.md` with stable IDs, the personas that hit them and
   how many rounds they've come up in. A finding several personas hit ranks higher;
3. sorts them into fix now, next, later and needs discussion, and brings the discussion items to the
   maintainer before any work starts;
4. adds agreed work to the UX fixes in the [restructuring tracker](../../docs/restructuring.md).

Fixed findings are rechecked by the persona that found them in the next round.

## Files

- `personas/`: one brief per persona.
- `report-template.md`: the report every persona writes.
- `phone.sh`: adb helpers for screenshots, labels, taps, strokes and typing.
