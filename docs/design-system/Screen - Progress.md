---
aliases:
  - Progress Redesign
  - Progress Screen Brief
tags:
  - sdt-fitness
  - design-system
  - screen-brief
  - progress
status: proposed
updated: 2026-07-23
---

# Screen — Progress

Parent: [[Design System Hub]]  
Foundation: [[Foundations - Visual Language]], [[Foundations - Color]], [[Foundations - Type Layout and Components]], [[Foundations - Interaction Accessibility and Content]]

## Status and intent

This is a **proposed redesign**, not a description of the shipped Progress screen.

Progress should answer: **Am I improving in the selected period, and what is driving the change?** The redesign turns a long stack of totals into a compact story: consistency first, training trend second, records/history third.

## Current implementation audit

References: `app/src/main/java/com/stepandemianenko/sdtfitness/Progress.kt`, `progress/ProgressCharts.kt`, and README screenshots.

The current sketch has real data and useful destinations, but its layout behaves like a metric inventory:

- `Progress Overview` wraps at 40 sp on the reference width, consuming much of the first viewport;
- the subtitle promises consistency, mastery, and outcomes, but the page does not establish a clear primary insight;
- “Sessions” and “Workout days” are lifetime/current-looking counts mixed with a seven-day comparison and continuity;
- “Best lift,” lifetime volume, and session load are labeled “Mastery” although they measure different concepts;
- duplicate medal/trophy illustrations occupy card space without explaining a trend;
- green delta text uses a low-contrast green directly on cream;
- the Health Connect permission/status management card interrupts the progress story;
- imported step/weight outcome cards, achievements, and setup/status all compete in one long scroll;
- achievement chevrons appear interactive even when the row behavior is not clearly implemented;
- fixed minimum heights and small 13–15 sp metric type create sparse cards and weak hierarchy;
- local palette/navigation components duplicate Home and Profile.

Keep the underlying metrics and drill-downs. Change the story and visual priority.

## Recommended information architecture

```text
Progress                                  [7D ▾]
Your training trend

┌ Weekly insight ───────────────────────────────────┐
│ 3 workouts                                         │
│ 1 more than the previous 7 days                   │
│ ▁▃▁▆▁▁▃   Mon … Sun                               │
└───────────────────────────────────────────────────┘

Training trend
┌ Volume                                  +12%      │
│ 4,167 kg this period                              │
│ [labeled line/bar chart]                           │
└───────────────────────────────────────────────────┘

Highlights
┌ Best set               ┐  ┌ Current streak       ┐
│ 75 kg                  │  │ 3 days               │
│ Lying leg curl         │  │ Trained Tue/Thu/Sat  │
└────────────────────────┘  └───────────────────────┘

Recent workouts                                  See all
┌ Thu 23 Jul · Lower body · 42 min                  ›
│ 14 sets · 4,167 kg volume
└───────────────────────────────────────────────────┘

Activity & body
┌ Steps today     2,400 · Health Connect            ›
├ Weight latest   66.0 kg · 21 Jul                  ›
└───────────────────────────────────────────────────┘

Achievements                                    View all
[latest earned achievement or calm empty state]

[Bottom navigation]
```

## Period model

Period is part of the meaning of every top metric. Use one visible selector in the header.

Recommended options, subject to repository/query support:

- `7D` — current rolling seven days compared with prior seven;
- `4W` — current 28 days compared with prior 28;
- `3M` — monthly or weekly aggregation;
- `1Y` — monthly aggregation.

Rules:

- The selected period applies to the weekly insight, chart, volume, session count, and relevant records.
- Clearly label lifetime records as `All-time` if they do not follow the selector.
- Do not compare incomplete periods misleadingly. If using calendar week/month, explain that model consistently.
- Preserve selection during drill-down and when returning to Progress.

If the data layer currently supports only seven-day comparisons, ship a static `Last 7 days` label rather than a nonfunctional dropdown.

## Hierarchy and layout specification

### Header

- Title: `Progress`, one line at `display.screen`.
- Optional subtitle: `Your training trend` at `body.default`; remove it if the period control and hero already make purpose clear.
- Period control: compact pill, 48 dp target, visible selected value.
- Gap to hero: 16 dp.

### Weekly insight hero

This is the only visually dominant card.

- Hero value: completed workouts in selected period, `headline.hero`.
- Comparison: plain language such as `1 more than the previous 7 days`; use `text.positive` plus arrow/sign where positive.
- Visualization: compact daily/weekly bars with labeled selected point and meaningful zero state.
- Tap: opens completed sessions already filtered to the period.
- Avoid a trophy/medal; the data visualization is the visual content.

For zero workouts, use `No workouts yet in this period` and provide a relevant action or allow period change. Do not paint zero as a negative red state.

### Training trend

Default metric is volume because it already exists, but metric switching may later include sets or duration.

- Label and unit are explicit.
- Show period total plus comparison.
- Chart has accessible exact-value inspection.
- One data point uses a point card/annotation rather than pretending to be a trend.
- No data uses a calm empty plot and explanation.

### Highlights

Use at most two compact cards in the first version:

- `Best set` — value, unit, exercise, date or `All-time` label;
- `Current streak` — days plus exact qualifying days/last activity.

On narrow widths or at large font scale, stack the cards. Do not label volume as mastery or attach generic medals to ordinary totals.

### Recent workouts

- Show the latest one to three completed sessions.
- Row includes date, session name/fallback, duration if available, set count, and volume.
- `See all` opens the existing Completed Sessions destination.
- Each visible chevron must open a session review.
- Empty state explains that completed workouts will appear here and offers `Start workout` when appropriate.

### Activity and body

Steps and weight remain visible but become source-aware rows, not a permission-management panel.

- `Steps today` includes source (`Health Connect` or `Manual`) and sync freshness.
- `Weight latest` includes value, unit, and measurement date.
- Tapping opens the existing chart dialog/destination.
- Permission, access management, and connection errors route to Profile → Health Connect.
- If import fails, local workout progress remains visible; show a scoped inline status only in this section.

### Achievements

Achievements are tertiary. Show the latest earned item or a compact count and `View all` only if a real destination exists. Remove decorative chevrons from static rows. An achievement requires a defined threshold, earned date, and stable name.

## Visual treatment

- Peach canvas, canonical cream surfaces, dark brown hierarchy.
- One coral chart/selection emphasis; do not fill every metric card with icons.
- Positive text uses `text.positive`; green may reinforce it with an arrow/mark.
- Neutral/negative change is worded and signed. Not every decrease is bad: lower body weight or training load needs context.
- Charts use coral as primary series, green/aqua only for semantically distinct series, with direct labels.
- Standard card padding is 16 dp; major sections have 24 dp separation.
- Replace the current uniform 16 dp stack with clear within-section versus between-section rhythm.

## Metric definitions shown in UI

Design and engineering should align visible labels with definitions. Do not finalize chart copy until the query semantics are confirmed.

| UI label | Proposed definition | Context required |
|---|---|---|
| Workouts | completed workout sessions in selected period | period and previous-period comparison |
| Workout days | distinct local dates with a completed workout | selected period |
| Volume | sum of completed `weight × reps` where supported | unit, selected period, comparison |
| Best set | highest completed weight for one set under current repository logic | exercise and all-time/period label |
| Sets | completed/logged sets under repository definition | selected period |
| Streak | consecutive qualifying training days under product rule | exact rule and current status |
| Steps | daily displayed steps under source precedence logic | date, source, sync state |
| Weight | latest valid measurement | unit, date, source |

“Session load” is too ambiguous for a set count. Use `Sets` unless a real load formula is defined.

## States

### Initial loading

Use a stable page shell and skeleton chart/cards. The current delayed branded loader may remain for cold start only if timing research supports it, but routine refresh must not obscure existing progress.

### Refreshing

Keep last known data, show scoped progress near the affected source, and update timestamps. Avoid a global `Refreshing progress...` line that shifts all content.

### Partial Health Connect error

Keep workout sections intact. In Activity & body show:

> `Couldn't refresh Health Connect · last synced 8 days ago`  
> Action: `Review connection`

### No workouts in period

- Hero: `No workouts yet in the last 7 days`.
- Chart shows an honest zero baseline.
- Recent sessions can still show an older session labeled outside the period, or remain separate as recent history.
- Primary action: `Start workout` if navigation permits.

### Sparse data

- One session: show the point and date, no directional trend claim.
- No previous-period data: `No previous period to compare`.
- No weight: `No weight data yet`, with source/connection action only if relevant.

## Copy deck

| Element | Recommended copy |
|---|---|
| Title | `Progress` |
| Subtitle | `Your training trend` |
| Hero positive | `{count} workouts` / `{delta} more than the previous 7 days` |
| Hero flat | `{count} workouts` / `Same as the previous 7 days` |
| Hero zero | `No workouts yet in this period` |
| Volume | `{value} kg this period` |
| Best record | `Best set` |
| History | `Recent workouts` / `See all` |
| Health section | `Activity & body` |
| Stale sync | `Last synced {relative time}` |

Avoid `Consistency, mastery, and outcomes in one place`; it is broad marketing copy and does not help interpret the page.

## Acceptance criteria

- [ ] The 360 dp first viewport shows the header, period, weekly insight, and beginning of the trend section.
- [ ] Title remains one line at default font scale.
- [ ] Every hero metric has a period and comparison/empty explanation.
- [ ] Health Connect management no longer interrupts workout progress.
- [ ] Local workout progress remains usable when health import fails.
- [ ] “Session load” is removed unless a real load metric is defined.
- [ ] Static achievement rows have no chevrons.
- [ ] Positive text meets contrast and does not rely on green alone.
- [ ] Charts define zero, one-point, loading, error, and exact-value states.
- [ ] Side-by-side cards stack under font scaling/narrow constraints.
- [ ] Recent workout rows open the correct session review.
- [ ] Bottom navigation exactly matches the shared top-level component.
- [ ] Screen uses semantic tokens rather than local hard-coded colors.

Open metric and product dependencies are tracked in [[Decisions - Design Log]].

