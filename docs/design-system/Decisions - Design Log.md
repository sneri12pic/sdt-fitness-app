---
aliases:
  - Design Decision Log
  - Design ADRs
tags:
  - sdt-fitness
  - design-system
  - decisions
status: active
updated: 2026-07-23
---

# Decisions — Design Log

Parent: [[Design System Hub]]

This log records choices that should persist across design jobs. Newer accepted decisions override older conflicting ones. Proposed screen behavior remains proposed until product and implementation acceptance.

## Decision format

```text
ID · Date · Status · Scope
Decision
Why
Consequences
Supersedes / superseded by
```

## Accepted foundation decisions

### DS-001 · 2026-07-23 · Accepted · Visual baseline

**Decision:** Use the current Home screen as the visual-language baseline for the app: peach canvas, cream cards, dark brown type, coral actions, warm spacing, and friendly fitness illustration.

**Why:** Home is the screen the product owner currently likes and it expresses a distinct, coherent personality.

**Consequences:** Other screens should not introduce unrelated Material purple, cold gray, pure-white dashboard, or neon gym aesthetics. Home remains open to targeted accessibility/component improvements.

### DS-002 · 2026-07-23 · Accepted · Semantic color system

**Decision:** Consolidate local screen palettes into the semantic tokens in [[Foundations - Color]]. Canonical card surface is `#F4E3D7`; canonical primary action is `#F08A67`; `#F27F3E` is a deprecated near-duplicate.

**Why:** Home, Profile, Progress, Auth, and Quick Log currently redeclare similar colors, causing drift and inaccessible pairings.

**Consequences:** Future implementation should centralize tokens. New screens must not create local hex variants without a recorded decision.

### DS-003 · 2026-07-23 · Accepted · Accessible action labels

**Decision:** Use dark brown `#4F2912` for normal-size labels on the canonical coral `#F08A67` action fill.

**Why:** The pairing is approximately 5.15:1; current cream-on-coral pairings are approximately 2.07:1 and fail normal-text contrast.

**Consequences:** The app retains its coral action identity while button text changes from cream to brown during redesign/migration.

### DS-004 · 2026-07-23 · Accepted · Warm light theme boundary

**Decision:** Warm light is the only approved branded theme. Dark theme and dynamic color require separate design/token work.

**Why:** Automatic inversion or system-derived color can erase the established identity and invalidate contrast/state relationships.

**Consequences:** Do not improvise a dark palette screen by screen. A future dark-theme brief must cover every semantic token, chart, illustration, system bar, and state.

### DS-005 · 2026-07-23 · Accepted · Shared primitives

**Decision:** Centralize bottom navigation, card, settings row, buttons, type scale, spacing, and shape rather than copying them per Activity/screen.

**Why:** Current top-level screens duplicate navigation and local dimensions, so small inconsistencies accumulate.

**Consequences:** Visual refactors should first establish shared primitives or migrate incrementally without changing unrelated behavior.

### DS-006 · 2026-07-23 · Accepted · Functional affordances only

**Decision:** No inert edit labels, unit rows, custom choices, or chevrons in shipped UI.

**Why:** Controls that look interactive but do nothing damage trust and accessibility.

**Consequences:** Hide the affordance until behavior exists, or implement the full behavior and its states.

### DS-007 · 2026-07-23 · Accepted · Icon and illustration roles

**Decision:** Utility/navigation icons use a consistent vector family; illustrations provide brand personality at deliberate focal points; emoji do not ship as permanent UI icons.

**Why:** The current asset mixture makes screens feel assembled from different systems and varies across devices.

**Consequences:** Replace the Profile flame emoji and audit duplicated trophy/medal decoration.

## Proposed flow decisions

### DS-101 · 2026-07-23 · Accepted · Rest Day and Quick Log presentation

**Decision:** Present Home Rest Day and Quick Log as modal bottom sheets rather than full Home sub-screens with duplicate bottom navigation.

**Why:** Both are short, contextual actions; preserving Home context makes them feel lighter and removes redundant scaffolding.

**Consequences:** See [[Screen - Home Rest Day]] and [[Screen - Home Quick Log]]. Validate system Back, scroll restoration, snackbar timing, and sheet behavior before acceptance.

### DS-102 · 2026-07-23 · Proposed · Profile purpose

**Decision:** Profile prioritizes identity, training setup, connections, preferences, and account controls. Remove the three equal progress-stat tiles and duplicate sign-out entry.

**Why:** The current page mixes settings with a mini dashboard and obscures its main purpose.

**Consequences:** Detailed workout/quest charts move to Progress or a dedicated history destination. See [[Screen - Profile]].

### DS-103 · 2026-07-23 · Proposed · Progress story

**Decision:** Progress begins with a selected period, workout comparison, and training trend. Health Connect management moves to Profile while imported metrics retain visible source/freshness in Progress.

**Why:** The current long card inventory lacks a primary answer and lets configuration interrupt analysis.

**Consequences:** Queries must support consistent periods/comparisons; partial health errors must not replace local workout progress. See [[Screen - Progress]].

### DS-104 · 2026-07-23 · Accepted · Quick Log Custom option

**Decision:** Remove Custom from shipped Quick Log until users can enter a label and that label is stored and displayed meaningfully.

**Why:** The current “Custom” choice saves only a generic type and does not offer customization.

**Consequences:** Initial redesigned selector can contain Walk and Mobility. A later custom activity feature needs validation, persistence, history display, and error state.

### DS-105 · 2026-07-23 · Accepted · Rest Day scope

**Decision:** Rest Day is a single confirmation, while walk/mobility/custom activity remain in Quick Log.

**Why:** Combining recovery status and light activity as peer options makes the logged meaning unclear.

**Consequences:** The Rest Day sheet explains consequences and offers `Add light activity instead` as a route to Quick Log.

### DS-106 · 2026-07-23 · Accepted · Routine calendar activity markers

**Decision:** Persist account-scoped workout, Quick Log, and Rest Day dates separately and render each with a distinct calendar marker. A date may show multiple markers when activities coexist; unclassified legacy routine dates retain a neutral Activity marker.

**Why:** The prior calendar stored every routine contribution in one date set, so users could see consistency but could not tell what happened on a given day.

**Consequences:** Room schema version 13 adds Quick Log and Rest Day history date sets, with migration of the latest legacy entries. Workout uses a filled circular marker, Quick Log a filled square, Rest Day an outlined circle, and every marker has a textual legend and screen-reader description.

## Open product decisions

### OPEN-001 · Rest day effect

- Does rest preserve, pause, or not affect a workout streak?
- Does it satisfy a scheduled routine day?
- Can it coexist with a completed workout or quick activity?
- Can it be removed/undone?

Until resolved, copy may promise only that the day is marked as recovery and no workout/training volume is added.

### OPEN-002 · Progress period semantics

- Rolling periods or calendar periods?
- Which metrics obey the selector versus remain all-time?
- What qualifies a workout day and streak?
- How is volume defined for bodyweight/unweighted exercises?

Do not show a functional-looking range selector until supported by consistent queries.

### OPEN-003 · Profile editing

- Which identity fields can be edited?
- How do guest and authenticated names interact?
- Is avatar selection/upload in scope?

Until implemented, remove the current inert `Edit` affordance.

### OPEN-004 · Unit system

- Display-only conversion or storage migration?
- Does the setting apply to weight, body weight, distance, and future measurements?
- Is it account-scoped?

Until implemented end-to-end, remove the current inert Units row.

### OPEN-005 · Quick Log defaults

- Fixed default versus last-used per account?
- Is recommended initial duration 15 min acceptable?
- Does Quick Log affect daily goal percentage, a separate activity metric, or only history?
- Is Undo supported?

The screen brief recommends a visible Walk + 15 min first-version default, with an exact summary and CTA.

### OPEN-006 · Dark theme and dynamic color

No dark/dynamic color direction is accepted. A future proposal must include semantic palette, contrast validation, system bars, charts, illustrations, screenshots, and migration behavior.

## Superseded decisions

None recorded yet.
