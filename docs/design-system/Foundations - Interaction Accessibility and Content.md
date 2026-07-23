---
aliases:
  - Interaction Accessibility and Content
  - UX Writing and States
tags:
  - sdt-fitness
  - design-system
  - accessibility
  - content-design
status: adopted
updated: 2026-07-23
---

# Foundations — Interaction, Accessibility, and Content

Parent: [[Design System Hub]]  
Related: [[Foundations - Type Layout and Components]] and [[Foundations - Color]]

## Interaction rules

### Make the result predictable

Before a user commits, the interface should state what will be logged or changed. After the action, show a short confirmation and make the resulting state visible on return.

Examples:

- “Log 15 min walk” is better than “Save Quick Log.”
- “Log rest day” plus “No workout will be added” is better than “Save.”
- A routine switch should show when reminders apply and where times are edited.

### Separate navigation from mutation

- Chevron rows navigate.
- Switch rows change a boolean immediately.
- Buttons commit or start an action.
- Chips select or filter.
- A row should not toggle and navigate at the same time unless the two targets are visually and semantically separate.

### Do not expose dead affordances

An edit label, unit row, achievement chevron, or Custom option must work in the shipped build. If the behavior is not implemented, remove the affordance or label it as unavailable in an internal prototype only. Inert controls train users not to trust the interface.

### Preserve context for quick actions

Short tasks launched from Home use a contextual bottom sheet. Longer setup or history tasks use a dedicated destination. Back and dismiss return to the exact prior scroll/context when feasible.

### Feedback sequence

For a successful quick action:

1. press state appears immediately;
2. duplicate submission is disabled;
3. save completes;
4. sheet closes;
5. Home data updates;
6. snackbar confirms the exact result and offers Undo when the data model safely supports it.

Errors keep the user's selection/input in place and explain the next action.

## Accessibility baseline

These are acceptance requirements, not polish tasks.

### Visual

- Normal text targets at least 4.5:1 contrast.
- Large text and meaningful non-text UI target at least 3:1 where the standard permits.
- Support Android font scaling through 200% without clipped text, inaccessible actions, or overlapping cards.
- Do not encode source, selected state, trend, or error only with color.
- Keep text off detailed illustration regions unless a tested scrim guarantees readability.

### Touch and input

- Interactive targets are at least 48 × 48 dp.
- Back, close, overflow, and chevrons have meaningful content descriptions.
- Radio-like selections use a radio/selectable semantic role and announce selected state.
- Switch labels describe the setting, and state is announced by the control.
- Keyboard and switch-access focus order follows visual reading order.

### Screen reader semantics

Merge a card only when its combined announcement is concise and the card has one action. Keep separate semantics when users need to inspect values or operate multiple controls.

Recommended metric announcement:

> “Workouts, 3 this week, up 1 from previous week. Button.”

Recommended selection announcement:

> “Walk, activity type, selected, radio button.”

Decorative images use no spoken description. Informative illustrations describe meaning, not appearance: “Recovery day” rather than “pink pillow picture.”

### Motion and feedback

- Respect reduced-motion settings.
- Haptics may confirm a discrete selection or successful log but never carry essential meaning.
- Snackbar duration must allow reading and action; screen readers must announce it.
- Loading indicators have a state description when the delay is perceptible.

## Content voice

The voice is supportive, concise, and factual.

### Use

- direct verbs: Start, Log, Review, Connect, Edit;
- specific nouns and units;
- acknowledgement without judgment;
- contractions where they sound natural;
- sentence case.

### Avoid

- guilt: “Don’t break your streak”;
- inflated praise for routine actions: “Legendary!” after every log;
- vague buttons: “Continue,” “Done,” or “Save” when a clearer result fits;
- technical storage language in user-facing UI;
- claims not supported by the data, such as calling a rest day a workout completion.

## Metric-writing rules

| Case | Preferred | Avoid |
|---|---|---|
| Period | “3 workouts this week” | “3 workouts” with no timeframe |
| Comparison | “1 more than last week” | green `+1` with no comparison label |
| Zero | “No workouts yet this week” | “0 Sessions” as a failure state |
| Weight | “66.0 kg · latest” | “Weight 66” |
| Volume | “4,167 kg total volume” | “Mastery: 4,167 kg” |
| Source | “2,400 steps · Health Connect” | a color-only imported badge |
| Stale data | “Last synced 8 days ago” plus action | “Synced” when stale |

Use locale-aware number formatting and the user's selected unit when that setting exists. Until unit switching is implemented, do not show an interactive Units row.

## State-writing patterns

### Loading

Use loading UI only when there is a real delay. Preserve layout where possible with a skeleton or known last value. Avoid hiding an entire screen behind a branded animation for routine refreshes.

### Empty

An empty state explains what is absent and offers the next useful action.

- Progress: “No workouts in this period” + “Start workout” or period change.
- Recent sessions: “Your completed workouts will appear here.”
- Health data: distinguish no permission, no data, and no data today.

### Error

State what failed, what remains safe, and what the user can do.

> “Couldn’t refresh Health Connect. Your saved workouts are still available. Try again.”

Do not replace the entire Progress page when only one imported source failed and local workout data is available.

### Offline/stale

Show the last successful value with its timestamp when safe. Label it as stale; do not silently show it as current.

### Success

Confirm the object and value:

- “15 min walk logged”
- “Rest day logged”
- “Routine saved”

Use Undo for reversible logs when implementation supports an atomic reversal.

## Privacy and sensitive surfaces

- Do not expose email, health permissions, or account state more prominently than the task requires.
- Health Connect status belongs under Profile → Connections; progress metrics may show their source without duplicating permission management.
- Destructive account actions require clear scope: signing out is not deleting local workout data. See [[app/overview|Login architecture and screen overview]].
- Account-scoped metrics must not visually merge across identities. See [[docs/account-scoped-data|Account-scoped data]].

## Review questions

Before accepting a flow, answer yes to all:

- Can a user identify the primary action in two seconds?
- Does the action label predict the result?
- Are period, unit, and source visible for important metrics?
- Can selection, error, and trend be understood without hue?
- Are all visible controls functional?
- Does 200% font scale preserve access to every action?
- Does Back/dismiss return to the expected context?
- Are loading, empty, error, stale, and success states designed?
- Does the copy encourage without making unsupported claims?

