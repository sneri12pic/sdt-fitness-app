---
aliases:
  - Visual Language
tags:
  - sdt-fitness
  - design-system
  - foundations
status: adopted
updated: 2026-07-23
---

# Foundations — Visual Language

Parent: [[Design System Hub]]

## North star

SDT Fitness should feel **warm, encouraging, grounded, and useful**. It is a daily training companion, not a clinical health portal, a neon performance dashboard, or a game that punishes missed days.

The current Home screen is the visual reference because it already expresses that character well:

- a warm peach canvas rather than sterile white;
- dark brown typography with strong hierarchy;
- cream cards that separate information without heavy shadows;
- coral actions that feel energetic without becoming aggressive;
- friendly fitness illustrations used as moments of personality;
- clear daily actions and short, supportive copy.

Home is a reference language, not a pixel template. New screens should reuse its visual grammar while improving accessibility, hierarchy, and component consistency.

## Product personality

| Attribute | Express it through | Avoid |
|---|---|---|
| Warm | peach canvas, cream surfaces, brown text | pure white pages, cold gray scaffolding |
| Encouraging | acknowledge effort, recovery, and small wins | guilt, streak threats, failure language |
| Grounded | real metrics, dates, units, and sources | decorative statistics without meaning |
| Energetic | coral on the primary action and key progress marks | orange on every element |
| Personal | routine, recent activity, relevant insight | generic motivational filler |
| Calm | one dominant action and restrained motion | competing CTAs, flashing or celebratory overload |

## Adopted design principles

### One screen, one primary question

Each screen should answer one user question before presenting secondary detail.

- Home: **What should I do today?**
- Progress: **Am I improving in the selected period?**
- Profile: **How is my training experience configured?**
- Rest Day: **What happens if I log recovery today?**
- Quick Log: **What light activity am I adding right now?**

If the first viewport cannot answer the screen's question, the hierarchy needs another pass.

### Progress is a story, not a warehouse

A number needs context: period, comparison, direction, source, or a next action. A lifetime total can be supporting detail, but it should not automatically become a hero metric.

Use this order:

1. current period result;
2. comparison or trend;
3. explanation or contributing behavior;
4. optional drill-down.

### Recovery belongs to training

Rest is not framed as failure or an escape from a streak. Recovery copy should explain what is recorded and what is not recorded. Positive language must remain factual; do not imply a rest day is a completed workout.

### Progressive disclosure

Keep the first viewport focused. Configuration, source management, long histories, and destructive actions belong behind clearly labeled rows or secondary screens. Do not solve information density by shrinking type.

### Earn every card

A card should group related content, represent a selectable object, or create an intentional layer. Do not place a single card inside another card merely to create decoration. On a peach page, one cream surface is usually enough.

### State must not rely on color alone

Selection uses at least two of: fill, border, checkmark, label, or position. Trends use a sign or arrow plus text. Errors include a plain-language message. Imported data includes a source label.

## Hierarchy model

Every main screen should use no more than four visible hierarchy levels:

1. **Screen level** — title and optional period/context control.
2. **Primary insight/action** — the most important card, chart, or composer.
3. **Section level** — grouped secondary content.
4. **Metadata** — captions, source, unit, timestamp, helper text.

Avoid multiple 40 sp headings, a heading inside every card, or chips that compete visually with the primary button.

## Surface and depth language

Depth comes from color, spacing, and borders rather than large shadows.

- Peach is the page canvas.
- Cream is the standard information and interaction surface.
- Pale rose is a nested tile or selected-state tint, not a second page background.
- A 1 dp warm border can clarify a selectable or interactive surface.
- Shadows are reserved for transient overlays such as a bottom sheet or menu.
- Nested surfaces should have a functional reason. Keep the maximum visible nesting to two levels.

## Illustration and icon language

The current app mixes bitmap illustrations, navigation line icons, Material icons, and an emoji. Future work should use each category intentionally.

### Utility icons

Use a single vector family and consistent optical weight for back, more, edit, chevron, close, settings, calendar, and filter actions. Default sizes are 20–24 dp inside a minimum 48 dp target. Utility icons are monochrome and use semantic text/icon colors.

### Navigation icons

All four bottom-navigation icons must share stroke/fill style, baseline, and optical size. Selected state uses a coral indicator plus a dark readable label; inactive labels must remain readable and should not be faded into the background.

### Illustrations

Friendly fitness illustrations are a brand asset when they explain a quest, activity, or empty state. Use them at one deliberate focal point, usually inside a 48–56 dp tinted tile or a genuine hero. Preserve a transparent background and consistent crop/lighting.

Do not use a large image simply to fill space. The Rest Day pillow image currently occupies hero space without adding a decision or explanation; [[Screen - Home Rest Day]] replaces that pattern.

### Emoji

Do not ship emoji as permanent interface icons. Rendering varies by device and breaks the otherwise controlled visual language. The Profile streak flame should be replaced by an approved vector or illustration asset before visual sign-off.

## Motion character

Motion should confirm cause and effect, not decorate static pages.

- selection: 120–180 ms color/border transition;
- sheet enter/exit: platform-standard Material motion;
- chart range change: 200–300 ms crossfade or value interpolation;
- save success: immediate button feedback, then sheet dismissal and snackbar;
- reduced motion: no essential meaning may depend on animation.

Avoid long entrance choreography, bouncing cards, automatic carousels, and celebration after routine low-stakes actions.

## Preserve, refine, retire

### Preserve

- warm peach, cream, brown, and coral identity;
- Home's 20 dp horizontal gutter and compact vertical rhythm;
- rounded cards and friendly illustrations;
- supportive, direct language;
- fixed bottom navigation on top-level destinations.

### Refine

- centralize colors, typography, spacing, radii, and navigation;
- use accessible action-label and state-color pairings;
- make metrics time-bounded and comparable;
- give interactive surfaces explicit affordances;
- make empty, loading, stale, imported, and error states first-class designs.

### Retire

- near-duplicate hard-coded colors per screen;
- permanent emoji icons;
- inert “Edit,” “Units,” or chevrons that imply unavailable behavior;
- cream-on-coral small text that misses contrast;
- bright green used as body text on cream;
- configuration cards dominating Progress;
- full-screen treatment for quick Home sub-actions when a contextual sheet is sufficient.

Next: [[Foundations - Color]], [[Foundations - Type Layout and Components]], and [[Foundations - Interaction Accessibility and Content]].

