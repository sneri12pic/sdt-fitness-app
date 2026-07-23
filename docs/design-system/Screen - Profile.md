---
aliases:
  - Profile Redesign
  - Profile Screen Brief
tags:
  - sdt-fitness
  - design-system
  - screen-brief
  - profile
status: proposed
updated: 2026-07-23
---

# Screen — Profile

Parent: [[Design System Hub]]  
Foundation: [[Foundations - Visual Language]], [[Foundations - Color]], [[Foundations - Type Layout and Components]], [[Foundations - Interaction Accessibility and Content]]

## Status and intent

This is a **proposed redesign**, not a description of the shipped Profile screen.

Profile should answer: **How is my training experience configured, and where do I manage my account?** It should feel personal without becoming a second Progress page. The first viewport should make identity, routine, reminders, and connections easy to understand.

## Current implementation audit

Reference: `app/src/main/java/com/stepandemianenko/sdtfitness/Profile.kt`.

The current sketch has useful ingredients but weak prioritization:

- a good single-word 40 sp title and warm palette;
- identity card, three equal stat cards, a “Personal setup” container, and sign-out action;
- workout count, streak, and quests compete equally even though Profile is primarily configuration;
- three tiny stat cards create a dashboard-like row and make labels fragile at larger font scales;
- `Edit` on the identity card is inert;
- `Units` looks navigable but is inert;
- Sign out appears both in the overflow path and as a visible page action;
- the streak uses a device-dependent emoji while neighboring icons are bitmap assets;
- the quest chart is hidden behind one stat tile, placing progress analysis in an unexpected destination;
- routine and reminders are presented as separate rows even though reminder configuration belongs to the routine flow;
- local Profile colors and components duplicate Home rather than consuming shared tokens.

The redesign keeps the warmth and identity card, removes misleading controls, reduces dashboard noise, and makes settings groups explicit.

## Information architecture

```text
Profile                                      [Settings]
Your training setup and account

┌ Identity ─────────────────────────────────────────┐
│ [Avatar]  Stepan                    Member        │
│           3 workouts this week                    │
└───────────────────────────────────────────────────┘

Training setup
┌ Your routine                              Tue · Thu ›
│ Strength · 3 days per week
├ Reminders                                     On ›
│ Training days · 18:00
└───────────────────────────────────────────────────┘

Connections
┌ Health Connect                         Connected ›
│ Steps and weight · synced 2h ago
└───────────────────────────────────────────────────┘

Preferences
┌ Units                                      kg ›
└───────────────────────────────────────────────────┘

Account
┌ Account details                               ›
│ name, email, membership
├ Sign out
└───────────────────────────────────────────────────┘

[Bottom navigation]
```

Only show behaviors that exist. If editing name/account or changing units is not implemented, remove those rows from the shipped page instead of leaving inert affordances.

## Hierarchy and layout specification

### Header

- Page gutter: 20 dp; top padding: 30 dp plus safe inset behavior.
- Title: `display.screen`, text `Profile`, one line.
- Supporting line: `body.default`, “Your training setup and account.”
- One 48 dp trailing settings icon only if it opens a real settings/account destination. Do not use an overflow menu merely to hide the only account action.
- Gap to identity card: 16 dp.

### Identity summary

- Standard cream card, 16 dp radius and padding.
- Avatar: 52–56 dp, approved illustration/photo treatment.
- Name: `title.section` or `title.card` depending on length.
- Membership badge: `Guest` or `Member`; status metadata, not a CTA.
- One contextual supporting line: current-week workout summary or routine goal. It must include a period.
- If profile editing exists, use a clear 48 dp edit icon/action with a destination. Otherwise show no edit affordance.

Do not bring back the three equal stat cards here. Detailed quest/workout history belongs to [[Screen - Progress]]. A single small weekly summary can make the page personal without changing its purpose.

### Section groups

- Section title: `title.section` with 24 dp separation from the prior group.
- Each group uses one cream container with divider-separated settings rows.
- Rows are at least 64 dp and expand for font scaling.
- Leading assets use one consistent 40–44 dp tile treatment.
- Trailing content is one of current value/status or chevron; avoid redundant chevron plus switch.

### Training setup

`Your routine` summary should expose the most decision-relevant configuration:

- primary line: goal, e.g. `Strength`;
- secondary line: `3 days per week · Tue, Thu, Sat`;
- tap opens the existing routine editor.

`Reminders` should either:

1. be part of the routine editor only; or
2. be a navigable row with summary `On · training days at 18:00`.

Do not make the whole row toggle while also using the row to open time configuration. If quick toggling is essential, the switch is a separate target and the row copy explains where times are edited.

### Connections

Health Connect belongs here as the management destination.

- status values: `Connected`, `Needs permission`, `Unavailable`, `Error`;
- supporting line includes scopes and last sync where known;
- tap opens connection detail with permission and refresh management;
- use factual status, not green alone;
- Progress may display imported metrics, but does not duplicate the full permission-management card.

### Preferences

Units appears only when unit switching and conversion are implemented end-to-end. The row shows the current system (`kg` or `lb`) and opens a focused choice screen/dialog. Changing it must update display and input consistently; it must not rewrite stored historical meaning incorrectly.

### Account

- `Account details` may show name/email/edit flows when implemented.
- `Sign out` uses `text.danger`, not coral, because it is an account action.
- One sign-out entry point on the page; remove the duplicate.
- Confirmation copy must explain that sign out does not delete saved workout data, matching [[app/overview|Login architecture and screen overview]].
- Guest handling may use `Create account` as the primary account row and present sign out only when relevant.

## Visual treatment

- Canvas: `canvas.primary`.
- Cards: `surface.primary`; nav: `surface.raised`.
- Primary and body type: canonical brown tokens.
- Coral is reserved for selection, small emphasis, and true primary actions.
- Status uses a label plus optional semantic mark.
- Replace emoji flame with an approved vector/illustration if streak remains anywhere in Profile.
- Avoid a large decorative hero; identity is the focal point.

## Interaction and state behavior

### Loading

Render the known account shell and skeleton only the dynamic summary/status values. Do not cover the entire Profile page with a long logo animation.

### Guest

- Badge: `Guest`.
- Identity uses a neutral generated avatar.
- Account group prioritizes `Create account` or `Sign in` if supported.
- Clearly preserve local data implications; never imply that creating an account automatically merges data unless implemented.

### Health Connect states

| State | Row title/status | Supporting text |
|---|---|---|
| Connected | `Connected` | `Steps and weight · synced 2h ago` |
| Permission needed | `Needs permission` | `Allow access to import steps and weight` |
| No recent data | `Connected` | `No new data today · last synced 2h ago` |
| Error | `Sync issue` | `Your saved workouts are still available` |
| Unavailable | `Unavailable` | `Health Connect isn't available on this device` |

### Sign out

Confirmation title: `Sign out?`  
Body: `You’ll return to sign in. Workouts saved on this device won’t be deleted.`  
Actions: `Cancel` and `Sign out`.

### Long content

- Names truncate only after the supporting content has reflowed; expose full identity to accessibility services.
- Routine days wrap to a second line.
- At 200% font scale, trailing values can move below the title; do not compress text into ellipses by default.

## Copy deck

| Element | Recommended copy |
|---|---|
| Subtitle | `Your training setup and account` |
| Routine section | `Training setup` |
| Routine row | `Your routine` |
| Connections section | `Connections` |
| Preference section | `Preferences` |
| Account section | `Account` |
| Health supporting text | `Steps and weight · synced {relative time}` |
| No routine | `Choose a goal and training days` |
| Reminder off | `Off` / `Set reminders for training days` |
| Units | `Units` / current value `kg` or `lb` |

Avoid `Settings and preferences` as the subtitle; it repeats the destination without adding meaning.

## Navigation ownership

- Profile owns routine configuration, reminders, connections, preferences, and account controls.
- Progress owns charts, personal records, workout history, and achievements.
- Home owns today’s actionable plan and quick logs.
- Health Connect detail can be shared, but Profile is its management entry point.

## Acceptance criteria

- [ ] First viewport shows identity plus the beginning of Training setup.
- [ ] No visible control is inert.
- [ ] Sign out has one entry point and accurate data-retention copy.
- [ ] Profile does not present three equal progress-stat tiles.
- [ ] Health Connect state is readable without relying on green.
- [ ] Routine and reminder behavior does not combine navigation and toggle semantics ambiguously.
- [ ] All rows survive 200% font scale and long localized text.
- [ ] Icons use one controlled family; no emoji ships as UI icon.
- [ ] Bottom navigation exactly matches other top-level destinations.
- [ ] Screen uses semantic tokens rather than new local hex values.

Open product dependencies are tracked in [[Decisions - Design Log]].

