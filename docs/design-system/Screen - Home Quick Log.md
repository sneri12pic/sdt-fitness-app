---
aliases:
  - Quick Log Redesign
  - Home Quick Log Brief
tags:
  - sdt-fitness
  - design-system
  - screen-brief
  - home
  - quick-log
status: implemented-with-followups
updated: 2026-07-23
---

# Screen — Home / Quick Log

Parent: [[Design System Hub]]  
Related screen: [[Screen - Home Rest Day]]

## Status and intent

This direction was implemented on 2026-07-23 in `quicklog/QuickLogScreen.kt` and the Home bottom-sheet host. The shipped flow uses Walk/Mobility tiles, a 5/15/20/30 minute selector with a 15-minute default, an exact pending summary, and an action-specific CTA. The nonfunctional Custom option was removed; Undo and a repository-backed error state remain follow-up work.

Quick Log should answer: **What light activity and duration am I adding to today?** It should be fast enough to use from Home while still preventing an ambiguous or accidental entry.

## Current implementation audit

References: `quicklog/QuickLogScreen.kt`, `QuickLogViewModel.kt`, Home integration, dimensions, and string resources.

Current strengths:

- simple two-step model: activity type then duration;
- radio/selectable semantics and a visible selected checkmark;
- clear Walk and Mobility illustrations;
- save blocks repeat taps and returns success to Home;
- palette is passed from Home, avoiding another completely separate visual theme.

Current problems:

- a full Home sub-screen with a large cream card and fixed bottom navigation is heavy for a quick action;
- `Back to Home` duplicates standard sheet/back behavior;
- three 98 dp option tiles are cramped inside roughly 320 dp content width;
- the `Custom` option is not actually custom: there is no label or details input;
- every opening resets to Walk and 5 min without acknowledging that default prominently;
- `Save Quick Log` does not state what will be recorded;
- four duration chips use intrinsic width rather than a balanced segmented layout and can become fragile under localization/font scaling;
- helper text says it adds to “today's progress” without specifying which visible metric changes;
- primary button is 46 dp and cream-on-coral contrast is insufficient;
- card-on-page nesting creates more container than hierarchy.

## Product decision

Use a modal bottom sheet launched from Home. Keep a compact activity selector, a balanced duration control, a live summary, and an action-specific CTA. Bottom navigation remains behind the sheet, not inside it.

`Other`/`Custom` appears only if the app can store and display a custom label meaningfully. Until that behavior exists, ship only Walk and Mobility rather than a fake customization affordance.

## Proposed composition

```text
             ─────
Quick log                                    [Close]
Add a light activity to today

Activity
┌ Walk ✓ ────────────┐  ┌ Mobility ────────────┐
│ [shoe illustration]│  │ [stretch illustration]│
└────────────────────┘  └────────────────────────┘
[Other, only when custom input is implemented]

Duration
┌ 5 min ┐ ┌ 15 min ┐ ┌ 20 min ┐ ┌ 30 min ┐

┌ Summary ────────────────────────────────────────┐
│ Walk · 15 min · Today                           │
└─────────────────────────────────────────────────┘

[ Log 15 min walk ]
```

At large font scale, activity tiles stack and duration options wrap into a 2 × 2 grid or become a scroll-safe list. The primary action remains reachable.

## Layout specification

### Sheet

- Modal bottom sheet, 24 dp top corners, `surface.primary`.
- 20 dp page padding and 16–24 dp section separation.
- No additional outer cream card; the sheet is already the surface.
- Respect system navigation inset and allow vertical scrolling.
- Close icon has a 48 dp target.

### Header

- Title: `title.section`, `Quick log`.
- Supporting line: `body.default`, `Add a light activity to today`.
- Avoid repeating “Quick Log” in an inner card.

### Activity selector

- Section label: `title.card` or accessible field label.
- Two options in the first shippable design: Walk and Mobility.
- Each tile: minimum 88 dp visual height, 16 dp radius, minimum 48 dp target, flexible width.
- Selected: coral-tinted fill, 1.5–2 dp coral border, checkmark, semibold dark label.
- Unselected: cream/muted surface, warm meaningful border, dark readable label.
- Illustration: 32–40 dp, consistent tile placement.
- Radio-group semantics announce the selected type.

If Custom is implemented:

- label it `Other` or `Custom activity` consistently;
- selecting it reveals a labeled text field such as `Activity name`;
- require nonblank, trimmed input with a sensible character limit;
- display that label in Home history/progress rather than saving only an enum with no name;
- preserve input after a failed save.

### Duration selector

- Visible choices retain current supported values: 5, 15, 20, and 30 min.
- Use four equal-width choices when they fit; otherwise 2 × 2.
- Minimum semantic target is 48 dp; visual height may be 40–44 dp within it.
- Selected state uses fill + border + text weight, not hue alone.
- If a custom duration is introduced later, use a numeric input/time picker with bounds and clear unit. Do not overload the current `Custom activity` option to mean custom duration.

### Summary

Always show the exact pending record:

> `Walk · 15 min · Today`

This can be a quiet one-line summary rather than a large card. It is especially important if defaults remain selected on open.

### Primary action

- Full width, 52 dp high.
- Dynamic label: `Log {duration} {activity}`, sentence case.
- Examples: `Log 15 min walk`, `Log 20 min mobility`.
- Fill `action.primary`, label `text.primary`.
- Loading keeps the same label geometry and announces `Logging activity`.

## Default behavior

The current code resets to Walk + 5 min every time. Choose one documented model:

### Recommended first version

- Default to Walk + 15 min because the pending record remains obvious in the summary and CTA.
- Do not submit on activity selection; explicit confirmation is still required.
- Reset only after a successful save or when reopening a clean sheet.

### Alternative after usage evidence

Remember the last successful activity and duration per account. If adopted, announce the pending selection clearly and store the preference under the active account. See [[docs/account-scoped-data|Account-scoped data]].

Do not silently mix global last-used state across accounts.

## Behavior

### Open

Home → Quick Log opens the sheet without losing Home scroll position. Initial selected state, summary, and CTA agree.

### Select

- Tap activity: update tile selection, summary, and CTA; optional light haptic.
- Tap duration: update duration selection, summary, and CTA.
- Selection transitions complete in 120–180 ms and do not move surrounding layout unexpectedly.

### Save

1. User taps the action-specific CTA.
2. Button disables and exposes a loading state.
3. Repository writes the entry for the active account and current date/time.
4. Sheet dismisses.
5. Home immediately reflects the entry where relevant.
6. Snackbar says `{duration} min {activity} logged` and offers Undo if supported.

The current success `Session logged — you’re building consistency` is too broad: a light activity is not necessarily a workout session, and the exact saved result is more useful.

### Error

Keep all selection/input and show:

> `Couldn't log this activity. Try again.`

The user should never need to reconstruct a custom label or duration after an error.

### Dismiss with custom input

For standard preset selections, dismiss without confirmation because no substantial work is lost. If a future custom text field contains user input, preserve it during the current Home session or confirm discard when appropriate.

### Duplicate taps/records

Blocking repeat taps prevents immediate duplicates. If the same activity/duration is legitimately logged twice, both may exist unless product rules say otherwise; history should distinguish timestamps. Do not silently merge entries without an explicit data decision.

## Relationship to Home and Progress

- Home should reflect the newly logged light activity in the most relevant daily area without calling it a completed workout.
- Quick Log must not increase completed workout/session count unless the product explicitly redefines that metric.
- A saved Quick Log adds the Quick Log marker to the account's Routine calendar date; it can coexist with a workout or rest marker.
- Progress may include it in a separate activity metric or timeline, not strength volume or completed-workout history.
- Copy like `Adds a light activity to today's progress` should be replaced until the exact affected metric is visible and defined.

## State matrix

| State | Activity | Duration | CTA |
|---|---|---|---|
| Default | Walk selected | 15 min selected | `Log 15 min walk` |
| Mobility | Mobility selected | retained | `Log {n} min mobility` |
| Custom available | Custom selected + field | retained | disabled until valid label |
| Saving | locked | locked | loading, no repeat tap |
| Error | preserved | preserved | enabled retry + error message |
| Success | record saved | record saved | sheet dismisses; exact snackbar |

## Accessibility

- Activity choices are a labeled radio group.
- Duration choices are a second labeled radio group.
- Screen reader announcement includes label, selected state, and role.
- Summary is live-updated politely, not announced multiple times for the same selection.
- CTA label contains enough context without relying on the summary.
- Tiles, chips, close, and CTA meet minimum targets.
- At 200% font scale controls stack/wrap; no horizontal clipping.
- Decorative illustrations are excluded from duplicate screen-reader announcements; the option label carries meaning.

## Acceptance criteria

- [ ] Quick Log is a contextual sheet with no duplicate bottom navigation or back row.
- [ ] Pending activity, duration, and date are visible before commit.
- [ ] CTA states the exact record being logged.
- [ ] `Custom` is removed unless a real custom input and stored/displayed label exist.
- [ ] Activity and duration selection use more than color.
- [ ] Controls survive long labels, localization, narrow screens, and 200% font scale.
- [ ] Primary action is 52 dp and uses accessible coral/brown pairing.
- [ ] Success copy does not call the entry a workout session.
- [ ] Quick Log does not silently affect workout or volume metrics.
- [ ] Loading, error, duplicate-tap, dismissal, and optional Undo behaviors are designed.
- [ ] Defaults and last-used behavior are account-safe and documented.
- [ ] All tokens follow the shared foundations.
