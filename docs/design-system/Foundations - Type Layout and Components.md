---
aliases:
  - Type Layout and Components
  - UI Specifications
tags:
  - sdt-fitness
  - design-system
  - typography
  - components
status: adopted
updated: 2026-07-23
---

# Foundations — Type, Layout, and Components

Parent: [[Design System Hub]]  
Related: [[Foundations - Visual Language]] and [[Foundations - Color]]

## Typography

Use the Android system sans family until a brand typeface is deliberately chosen, licensed, bundled, and tested. The current issue is not the font family; it is that most screens declare arbitrary sizes instead of sharing a scale.

### Canonical scale

| Style | Size / line | Weight | Use |
|---|---:|---|---|
| `display.screen` | 40 / 40 sp | Bold | One short top-level title such as “Profile” or “Progress” |
| `headline.hero` | 28 / 32 sp | Bold | Primary metric or insight inside a hero |
| `title.section` | 22 / 26 sp | Bold | Major section heading |
| `title.card` | 17 / 22 sp | Semibold | Card title or settings-row label |
| `metric.primary` | 22 / 26 sp | Bold | Standard stat value |
| `body.default` | 14 / 20 sp | Normal | Explanations and supporting copy |
| `body.compact` | 13 / 18 sp | Normal | Dense rows and secondary metrics |
| `label.action` | 15 / 18 sp | Semibold | Button and prominent action label |
| `label.meta` | 12 / 16 sp | Medium | Badge, source, timestamp, unit helper |

Use platform font scaling. Do not clamp user font scale to protect a sketch. At larger font sizes, allow cards and rows to grow, move side-by-side content into a column, and keep actions visible without overlap.

### Typography rules

- One `display.screen` per page.
- Keep the screen title to one line at the 360 dp reference width. Prefer “Progress” over shrinking or wrapping “Progress Overview.”
- Use sentence case: “Workout days,” not “Workout Days,” except proper nouns.
- Keep the number and unit together where possible: `75 kg`, `2,400 steps`, `45 min`.
- Use tabular figures for columns, chart axes, or rapidly changing metrics if the selected font supports them.
- Do not use color as the only difference between heading, value, and metadata; size and weight carry hierarchy.

## Responsive frame

The current Home structure is adopted as the narrow-screen baseline:

- reference design viewport: 360–402 dp wide;
- centered content container: maximum 360 dp;
- page horizontal gutter: 20 dp;
- standard top content padding: 30 dp, adjusted for system insets where needed;
- content scrolls behind neither bottom navigation nor the system navigation area;
- larger screens keep readable line length and may introduce a wider/tablet composition only through an explicit responsive design.

Do not scale the entire phone layout proportionally on tablets. Expand relationships: two-pane settings, wider charts, or grouped columns while preserving readable content widths.

## Spacing scale

Use an intentional 4 dp base rhythm.

| Token | Value | Typical use |
|---|---:|---|
| `space.1` | 4 dp | label-to-caption, compact icon gap |
| `space.2` | 8 dp | related items inside a component |
| `space.3` | 12 dp | standard row/card internal gap |
| `space.4` | 16 dp | card padding, section item gap |
| `space.5` | 20 dp | page gutter, large internal padding |
| `space.6` | 24 dp | major section separation |
| `space.8` | 32 dp | intentional page-level break |

Use `12 dp` between sibling cards and `24 dp` between conceptually different sections. The current Progress page uses a uniform 16 dp stack, making headings, cards, and transitions feel equally related; future layouts should express grouping through variable spacing.

## Shape scale

| Token | Value | Use |
|---|---:|---|
| `radius.control` | 12 dp | buttons, compact rows, text fields |
| `radius.card` | 16 dp | cards, large selectable tiles |
| `radius.sheet` | 24 dp top corners | modal bottom sheets |
| `radius.pill` | 50% | badges and short filters only |

Avoid mixing 10, 12, 14, and 16 dp simply because individual screens were built at different times. Inner elements should generally use an equal or smaller radius than their container.

## Touch and control dimensions

- minimum touch target: 48 × 48 dp;
- primary button: 52 dp high, full content width when there is one dominant action;
- secondary button: 48 dp high;
- compact chips: minimum 40 dp visual height and 48 dp semantic target;
- icon button: 48 dp target with 20–24 dp glyph;
- standard settings row: minimum 64 dp, growing with wrapped copy;
- bottom-nav item: equal width, minimum 48 dp high target above system inset.

The current 46 dp Quick Log and Rest Day buttons should migrate to the 52 dp primary action standard when redesigned.

## Core components

### App header

Contains a single-line screen title and, only when necessary, one trailing utility action or a compact period selector. Supporting copy is optional and limited to one or two lines. Do not repeat the destination name in a subtitle.

### Standard card

- Fill: `surface.primary`
- Radius: 16 dp
- Internal padding: 16 dp
- Default border/shadow: none
- Interactive card: add a semantic click target and a clear chevron, selection state, or action label
- Static card: do not add a chevron

### Settings row

- Leading utility icon or illustration tile: 40–44 dp
- Title: `title.card`
- Supporting text: `body.compact`, maximum two lines
- Trailing element: exactly one of chevron, switch, current value, or status badge
- Entire row is clickable when it opens a destination; a switch row toggles directly and must not also navigate unexpectedly.

### Metric card

A metric card includes:

1. metric label;
2. value and unit;
3. period/comparison/source;
4. optional visualization or drill-down.

Do not use a medal or trophy as generic empty space. Achievement imagery represents an actual achievement; trend data uses a chart, arrow, or sparkline.

### Filter/period control

Use a compact pill or segmented control near the relevant header. The selected period must be visible without opening a dialog. Standard periods should be consistent across progress views, such as `7D`, `4W`, `3M`, and `1Y`; the product/data layer must confirm availability.

### Badge

Badges communicate metadata such as `Imported`, `Manual`, `Guest`, or `Synced 2h ago`. They are not buttons unless they have an explicit interaction style and accessibility role. Prefer plain-language labels over unexplained color dots.

### Primary action

- one per focused flow;
- full width in sheets and single-task pages;
- label begins with a verb and names the result: “Log rest day,” “Log 15 min walk,” “Save routine”;
- use `action.primary` with `text.primary` under the current accessible recipe;
- loading state preserves width and height and prevents duplicate submission.

### Bottom sheet

Use a modal bottom sheet for a short task launched from Home when the user should retain Home context. It includes:

- 24 dp top radii and a drag handle;
- title and concise consequence/description;
- task controls;
- a fixed or reliably reachable primary action;
- dismissal by close/back/scrim unless data loss requires confirmation;
- no duplicate bottom navigation inside the sheet.

Rest Day and Quick Log follow this pattern in their proposed redesigns.

### Bottom navigation

Bottom navigation appears only on top-level destinations. It remains structurally identical across Home, Workout, Progress, and Profile.

- background: `surface.raised`;
- four equal items;
- same icon family and optical size;
- label style: `label.meta`, never below readable contrast;
- selected state: coral indicator plus dark icon/label;
- system navigation inset uses the same background;
- no per-screen copies with diverging padding, colors, or icon dimensions.

## Data visualization baseline

- Label the metric and period outside or above the plot.
- Keep axes and gridlines quiet but readable.
- Use direct labels or a nearby legend; do not make users infer icon meanings.
- Show units at the value or axis.
- Handle zero, one-point, and sparse datasets deliberately.
- A chart tap should expose exact value and date with accessible text.
- Trend color is redundant with a signed value, arrow, or textual direction.
- Keep decorative illustration out of the data plotting area.

## Component state inventory

Every reusable component handed to engineering should include:

- default;
- pressed/focused;
- selected/unselected where relevant;
- disabled;
- loading if it performs work;
- success feedback;
- error;
- long text and 200% font-scale behavior;
- empty/zero data for metric components.

Missing states are incomplete design work, not engineering details to improvise.

