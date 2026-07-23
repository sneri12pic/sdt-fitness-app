---
aliases:
  - SDT Color System
  - Color Palette
tags:
  - sdt-fitness
  - design-system
  - color
status: adopted
updated: 2026-07-23
---

# Foundations — Color

Parent: [[Design System Hub]]  
Related: [[Foundations - Visual Language]]

## Decision

The canonical light theme is the warm palette established by Home. Color names are semantic so future screens ask “what is this element doing?” instead of copying a hex from a neighboring file.

The current code declares slightly different local palettes in Home, Profile, Progress, Quick Log, and Auth. Future implementation should migrate them to one token source. Until a dark palette is deliberately designed and tested, do not generate one by inverting these values or enabling arbitrary dynamic color.

## Canonical tokens

| Token | Hex | Purpose | Usage rule |
|---|---:|---|---|
| `canvas.primary` | `#EBC0B0` | App page background | Default top-level background |
| `surface.primary` | `#F4E3D7` | Main cards and sheets | Standard content surface; Home reference |
| `surface.raised` | `#F5E5DA` | Navigation and transient raised surfaces | Do not use interchangeably with every card |
| `surface.muted` | `#F1D5CB` | Icon tiles, nested neutral controls | One nested layer maximum |
| `surface.badge` | `#EBCBC0` | Neutral badges and source labels | Metadata only, never a primary CTA |
| `text.primary` | `#4F2912` | Headings, values, primary labels | Default high-emphasis text |
| `text.secondary` | `#6B4637` | Body copy and metadata | Use at full token value for readable body text |
| `action.primary` | `#F08A67` | Main action fill and selection emphasis | Pair small/normal text with `text.primary` |
| `accent.strong` | `#F05C2D` | Progress ring, chart highlight, decorative emphasis | Do not use with cream body text |
| `state.positive` | `#69C47A` | Positive graphic, dot, fill, or tint | Not body text on cream/peach |
| `text.positive` | `#276035` | Positive text | Accessible on both canonical canvas and surface |
| `state.info` | `#3FB6DE` | Water/data graphic accent | Do not use as small text on cream |
| `text.danger` | `#9A2E1F` | Error/destructive text | Preferred warning text on canvas and cream |
| `action.danger` | `#C62828` | Destructive filled action | Pair with cream; require confirmation when destructive |
| `border.warm` | `#D6AA98` | Dividers and meaningful borders | Use at sufficient alpha/contrast for the role |
| `track.neutral` | `#E6B8A5` | Progress tracks and quiet separators | Never place body text on it |

Alpha is a state modifier, not a new token. Common selection tint is `action.primary` at 12–14% over a cream surface. Disabled states reduce emphasis across fill, label, and interaction semantics; do not communicate disabled state only by dropping text below readability.

## Palette proportions

As a starting point for a typical screen:

- 60–70% peach canvas;
- 20–30% cream surfaces;
- up to 10% coral emphasis;
- green, aqua, and danger colors only where their meaning requires them.

This is a hierarchy guardrail, not a pixel quota. A screen with several full coral cards has lost the calm visual rhythm.

## Approved pairings

Approximate WCAG contrast ratios from the canonical hex values:

| Foreground / background | Ratio | Decision |
|---|---:|---|
| `text.primary` / `canvas.primary` | 7.66:1 | Approved for all text |
| `text.primary` / `surface.primary` | 10.14:1 | Approved for all text |
| `text.secondary` / `canvas.primary` | 4.97:1 | Approved for normal text |
| `text.secondary` / `surface.primary` | 6.58:1 | Approved for normal text |
| `text.primary` / `action.primary` | 5.15:1 | Approved for normal button text |
| `text.positive` / `canvas.primary` | 4.52:1 | Approved for normal text |
| `text.positive` / `surface.primary` | 5.98:1 | Approved for normal text |
| `text.danger` / `canvas.primary` | 4.56:1 | Approved for normal text |
| `text.danger` / `surface.primary` | 6.04:1 | Approved for normal text |

Check final rendered designs as well as raw hex values: alpha, overlays, image backgrounds, font weight, and device rendering can alter the effective result.

## Restricted pairings

These current combinations are visually on-brand but fail for normal-sized text:

| Pair | Approx. ratio | Corrective rule |
|---|---:|---|
| cream `#FCE8DA` on `action.primary` | 2.07:1 | Use `text.primary` on the coral button |
| light cream `#FFF2E9` on legacy `#F27F3E` | 2.43:1 | Use canonical coral + brown, or redesign a darker action token |
| `state.positive` on `surface.primary` | 1.72:1 | Use `text.positive` for words; keep green as graphic support |
| `state.info` on `surface.primary` | 1.88:1 | Use it for charts/icons, not small text |
| legacy inactive `#C48778` on raised cream | 2.41:1 | Use readable secondary labels; indicate selection separately |

Large text exceptions should not become a shortcut. Buttons, nav labels, chips, and metadata are usually normal-sized text and must meet the normal-text target.

## Semantic recipes

### Primary action

- Fill: `action.primary`
- Label/icon: `text.primary`
- Pressed: darken fill slightly or add a brown state overlay
- Disabled: muted warm fill plus readable secondary label
- Shape and dimensions: [[Foundations - Type Layout and Components]]

### Selected option

- Base: `surface.primary`
- Selected fill: 12–14% `action.primary`
- Selected border: full `action.primary`, 1.5–2 dp
- Selected label: `text.primary`, semibold
- Additional cue: checkmark or radio indicator

### Positive change

- Number/text: `text.positive`
- Optional arrow/dot/chart mark: `state.positive`
- Always include `+`, an arrow, or “up/down” language; green alone is insufficient.

### Imported or synced data

- Value: `text.primary`
- Source/timestamp: `text.secondary` in a neutral badge or metadata line
- Optional info icon: `state.info`
- Never make imported data look disabled merely because it is secondary.

### Destructive action

- Inline label: `text.danger`
- Confirming filled button: `action.danger` with cream label
- Do not use coral for sign out/delete; coral is the brand action color, not danger.

## Deprecation map

| Current local value | Future meaning |
|---|---|
| Home `CardBackground #F4E3D7` | `surface.primary` |
| Profile/Progress `#F5E5DA` card | Reserve as `surface.raised`; standard cards migrate to `surface.primary` |
| Home/Progress `#F08A67` accent | `action.primary` |
| Profile/Auth `#F27F3E` accent | Deprecated near-duplicate; migrate to `action.primary` unless a new accepted decision says otherwise |
| Profile `#F1D3C8` and Progress `#F1D5CB` tile | Consolidate on `surface.muted #F1D5CB` |
| `#C48778` inactive nav text | Retire for text; may remain decorative only after contrast review |

## Theme boundaries

- **Current supported visual theme:** warm light.
- **Dark theme:** open design project; no approved token set.
- **Dynamic color:** not approved for core branded screens because it can replace the warm identity and break known pairings.
- **Pure black/white:** avoid for main UI; use only where an external platform surface or asset requires it.
- **Charts:** start with coral, green, and aqua, but validate categorical distinguishability and add labels/pattern/shape. Never rely on three hues alone.

## Implementation note for future jobs

The current `ui/theme/Color.kt` still contains the starter Material purple palette, while most app screens use local hard-coded values. A future implementation task should centralize the semantic tokens before broad visual refactoring. That migration is not evidence that the screen design changed; preserve the accepted visuals while consolidating the source.

