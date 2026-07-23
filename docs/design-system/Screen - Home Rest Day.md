---
aliases:
  - Rest Day Redesign
  - Home Rest Day Brief
tags:
  - sdt-fitness
  - design-system
  - screen-brief
  - home
  - recovery
status: implemented-with-followups
updated: 2026-07-23
---

# Screen — Home / Rest Day

Parent: [[Design System Hub]]  
Related screen: [[Screen - Home Quick Log]]

## Status and intent

This direction was implemented on 2026-07-23 in `Home.kt` as a contextual Material bottom sheet. The shipped flow includes date context, consequence rows, an already-logged state, an accessible primary action, and a direct route to Quick Log. Undo and a repository-backed error state remain follow-up work.

Rest Day should answer: **What will be recorded if I choose recovery today?** The flow should take seconds, preserve Home context, and avoid making recovery feel like either failure or a completed workout.

## Current implementation audit

References: `Home.kt` lines around `RestDayDetailsContent`, `res/values/dimens.xml`, and the `rest_day_*` strings.

Current strengths:

- warm palette and supportive message;
- a direct save action;
- copy correctly says no workout will be logged;
- back behavior returns to Home;
- success snackbar acknowledges recovery.

Current problems:

- a full Home sub-screen and persistent bottom navigation make a one-action confirmation feel heavier than necessary;
- the 136 dp pillow hero consumes prime space without explaining the consequence;
- the screen is a large hero card followed by another card, creating decorative layers rather than decision hierarchy;
- source code contains unused recovery-option and chip components/strings, suggesting an unresolved choice between a simple rest log and a multi-option check-in;
- the primary button is only 46 dp high and uses low-contrast cream-on-coral text;
- `Back to Home` takes layout space that a sheet close/back gesture can handle;
- there is no date context, undo path, or clearly documented effect on plans, quests, or streaks.

## Product decision

Treat Rest Day as a single focused action. Mobility, walk, and custom activity belong to [[Screen - Home Quick Log]], not to a second choice inside Rest Day.

Launch a modal bottom sheet over Home. This keeps the daily dashboard visible in context and avoids duplicating top-level navigation. If the product later adds a recovery journal, notes, soreness, or readiness score, that larger flow should get its own explicit brief rather than expanding this confirmation sheet ad hoc.

## Proposed composition

```text
             ─────
Log a recovery day                         [Close]
Today · Thursday, 23 July

        [small recovery illustration]

Recovery is part of your plan
This records today as a rest day. It won't add a workout
or training volume.

┌ What changes ─────────────────────────────────────┐
│ ✓ Today is marked as recovery                     │
│ — No workout or sets are added                    │
│ i Your daily quests keep their own progress       │
└───────────────────────────────────────────────────┘

[ Log rest day ]
Add light activity instead
```

The exact streak/plan/quest statements must reflect product logic. If behavior is still undecided, omit the unsupported line in the shipped UI and resolve the open decision in [[Decisions - Design Log]].

## Layout specification

### Sheet

- Modal bottom sheet, 24 dp top corners, `surface.primary`.
- 20 dp horizontal padding, 20–24 dp top/bottom content padding.
- Respect navigation-bar inset.
- Maximum width follows platform/tablet sheet guidance; on phone it uses available width.
- Home behind the sheet is dimmed but remains recognizable.
- Do not show bottom navigation inside the sheet.

### Header and date

- Title: `title.section`, `Log a recovery day`.
- Close icon: 48 dp target, meaningful content description.
- Date: `body.compact`, locale formatted; `Today` plus full date prevents accidental logging for an ambiguous day.
- If future flows allow another date, the date becomes an explicit control. Do not imply editability today.

### Illustration

- Use the approved recovery/pillow illustration at 64–80 dp, not as a full-width crop.
- It supports recognition but does not displace explanatory copy.
- Accessibility description is unnecessary when the adjacent heading carries the meaning; treat as decorative.

### Explanation block

- Heading: `title.card`, `Recovery is part of your plan`.
- Body: `body.default`, maximum three lines at default scale.
- Consequence panel uses a subtle muted surface only if the three lines need grouping.
- Each line has a text/symbol cue; no green-only meaning.

### Actions

- Primary: 52 dp, full width, `Log rest day`.
- Label uses `text.primary` on `action.primary`.
- Secondary text action: `Add light activity instead`; dismisses/replaces Rest Day and opens Quick Log without a flash back to Home.
- No generic `Save` label.

## Behavior

### Open

Tap Home → Rest Day. Sheet opens with current day context. No default mutable choices are required.

### Confirm

1. Button shows a stable loading state and blocks repeat taps.
2. Repository logs the recovery option for the shown date.
3. Sheet dismisses.
4. Home immediately reflects recovery state.
5. Snackbar says `Rest day logged` and offers `Undo` if atomic reversal is supported.

The account's Routine calendar shows the date with the outlined Rest marker. If a workout or Quick Log also exists on that date, the markers coexist rather than replacing one another.

The existing snackbar copy `Rest day logged — recovery counts too` has the right tone, but the shorter version is clearer if Home visibly explains the result.

### Dismiss

Close, Back, or scrim dismisses without change. There is no confirmation because the sheet contains no unsaved user input.

### Repeat/open after logging

If today is already marked as rest:

- Home tile should say `Rest day logged` rather than inviting a duplicate;
- reopening the sheet shows `Today is marked as a recovery day`;
- primary action becomes `Remove rest day` or the tile offers Undo/edit, based on repository behavior;
- never create duplicate recovery records silently.

### Failure

Keep the sheet open, preserve context, and show:

> `Couldn't log this rest day. Try again.`

Do not dismiss and show a generic error on Home.

## Copy rules

Recommended initial copy:

| Element | Copy |
|---|---|
| Title | `Log a recovery day` |
| Date | `Today · {localized full date}` |
| Heading | `Recovery is part of your plan` |
| Body | `This records today as a rest day. It won't add a workout or training volume.` |
| Primary | `Log rest day` |
| Secondary | `Add light activity instead` |
| Success | `Rest day logged` |
| Error | `Couldn't log this rest day. Try again.` |

Do not say recovery “keeps your routine consistent” until the exact streak/routine rule is accepted; that phrase can be interpreted as awarding workout continuity.

## Data and policy questions

The design must not invent answers to these:

- Does a rest day preserve, pause, or have no effect on the workout streak?
- Can users log rest for a scheduled workout day only, or any day?
- Does logging rest change the planned workout card?
- Can rest coexist with a completed workout or Quick Log on the same date?
- Does rest affect daily goal percentage or quest completion?
- Is undo/removal supported by the repository?

Until decided, UI copy should limit itself to known facts: rest is recorded and no workout/training volume is added.

## Accessibility

- Focus enters at sheet title and follows date, explanation, primary, secondary.
- Close, primary, and secondary targets meet 48 dp minimum.
- Consequence icons are redundant with text and decorative to screen readers.
- At 200% font scale, the sheet scrolls while the primary action remains reachable.
- Action state is announced during saving and after success.

## Acceptance criteria

- [ ] Flow appears as a contextual sheet, not another top-level page.
- [ ] No duplicate bottom navigation or “Back to Home” row is present.
- [ ] The exact date being logged is visible.
- [ ] Copy accurately distinguishes rest from a completed workout.
- [ ] Pillow/recovery art is supporting, not a full-width hero.
- [ ] Primary action is at least 52 dp and uses an accessible label pairing.
- [ ] Secondary action opens Quick Log directly.
- [ ] Repeat, error, loading, dismissal, and optional Undo states are designed.
- [ ] No unsupported promise is made about streaks, quests, or routine completion.
- [ ] All tokens follow the shared foundations.
