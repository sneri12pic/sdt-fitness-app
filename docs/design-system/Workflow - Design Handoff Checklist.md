---
aliases:
  - Design Handoff Checklist
  - Future Design Job Checklist
tags:
  - sdt-fitness
  - design-system
  - workflow
  - handoff
status: adopted
updated: 2026-07-23
---

# Workflow — Design Handoff Checklist

Parent: [[Design System Hub]]

Use this note for every future screen or component design job. Copy the brief template into a new note, link it to the hub, and complete the acceptance checks before calling the design ready.

## 1. Frame the job

- [ ] Name the single user question the screen answers.
- [ ] Identify entry point, exit point, and what context must be preserved.
- [ ] State whether this is a top-level destination, dedicated sub-screen, dialog, or contextual bottom sheet.
- [ ] List the real data/actions already available in the app.
- [ ] Identify product behavior that is still open; do not invent it in visual design.
- [ ] Link the relevant implementation, model, repository, test, screenshot, and existing Obsidian notes.
- [ ] Mark the brief `proposed` until accepted and implemented.

## 2. Reuse the system

- [ ] Use semantic colors from [[Foundations - Color]].
- [ ] Use the canonical type, spacing, radius, and target scales from [[Foundations - Type Layout and Components]].
- [ ] Reuse or extend a shared component before creating a screen-local duplicate.
- [ ] Match the shared bottom navigation exactly on top-level screens.
- [ ] Use the approved utility icon family and illustration rules.
- [ ] Add a decision-log entry if a foundation rule must change.

New components need a name, purpose, anatomy, token mapping, behavior, all states, accessibility semantics, and responsive rules. A screenshot alone is not a component specification.

## 3. Build the information hierarchy

- [ ] The first viewport answers the screen's primary question.
- [ ] There is one visually dominant insight or action.
- [ ] Major sections use 24 dp separation; related items use the smaller spacing scale.
- [ ] Cards group meaningful content rather than decorate every block.
- [ ] Important metrics include period, unit, comparison, and source where relevant.
- [ ] Configuration is separated from analysis.
- [ ] Destructive actions are separated from routine actions.
- [ ] Every chevron, switch, edit label, and button maps to real behavior.

## 4. Specify responsive behavior

Review at minimum:

| Case | Required evidence |
|---|---|
| 360 × 800 dp class | Primary phone composition |
| 402 × 868 dp class | Current preview/readme reference |
| Short viewport | Primary action remains reachable |
| 200% font scale | No clipping, overlap, or unreachable control |
| Long localized copy | Rows/cards grow or reflow |
| Large/tablet width | Intentional max width or adaptive composition |

Document which side-by-side blocks stack, which chips wrap, how charts resize, and how fixed navigation/sheets handle system insets.

## 5. Design every state

- [ ] Default/populated
- [ ] Loading and refresh
- [ ] Empty/zero
- [ ] One-point or sparse data where relevant
- [ ] Error, including partial-source error
- [ ] Offline/stale data
- [ ] Pressed/focused
- [ ] Selected/unselected
- [ ] Disabled
- [ ] Saving and duplicate-tap prevention
- [ ] Success confirmation and optional Undo
- [ ] Guest/signed-in variants where relevant
- [ ] Permission denied/unavailable where relevant

For each state, specify visible copy, retained data, enabled actions, navigation result, and accessibility announcement.

## 6. Accessibility review

- [ ] Normal text contrast is at least 4.5:1.
- [ ] Meaningful non-text controls/graphics meet the applicable 3:1 target.
- [ ] No state or trend relies on color alone.
- [ ] Touch targets are at least 48 × 48 dp.
- [ ] Focus order matches reading order.
- [ ] Icons have correct descriptions; decorative art is excluded.
- [ ] Radio, switch, tab, heading, chart, and button roles are explicit.
- [ ] Charts have a textual summary and exact-value access.
- [ ] Reduced motion retains all meaning.
- [ ] Snackbar/error changes are announced and readable.

## 7. Content review

- [ ] Title names the destination and fits one line at the reference width.
- [ ] CTA begins with a verb and predicts the result.
- [ ] Empty/error copy explains the next action.
- [ ] Metric wording matches its real definition.
- [ ] Recovery and small activity are not mislabeled as completed workouts.
- [ ] Imported/manual source and freshness are visible where meaningful.
- [ ] Copy is supportive but makes no unsupported health or performance claim.
- [ ] Sign-out/delete copy states scope and consequence accurately.

## 8. Handoff package

Every ready design should contain:

- screen/flow link and version/date;
- user question and success criterion;
- annotated default composition;
- component/token mapping;
- interaction/event map;
- full state matrix;
- responsive/font-scale behavior;
- exact copy deck;
- analytics requirement, if any;
- data definitions and source;
- known product/technical dependencies;
- acceptance checklist;
- before/after screenshot plan.

### Interaction/event map template

| User action | Immediate UI | Data/domain event | Success result | Failure result |
|---|---|---|---|---|
| `{tap/select}` | `{pressed/loading}` | `{event/repository call}` | `{navigation + feedback}` | `{retained state + error}` |

### State matrix template

| State | Header/hero | Main content | Actions | Accessibility announcement |
|---|---|---|---|---|
| Loading | | | | |
| Populated | | | | |
| Empty | | | | |
| Error | | | | |
| Success | | | | |

## 9. Implementation QA

Design is not complete until the implementation is checked against the brief.

- [ ] Compare screenshots at the same viewport and state, not from memory.
- [ ] Confirm semantic tokens are used; search for newly introduced local hex values.
- [ ] Confirm spacing/radii/type use shared values rather than near-duplicates.
- [ ] Exercise every visible control.
- [ ] Test Back, system gesture, sheet dismissal, and bottom-nav transitions.
- [ ] Test process/state restoration where the flow holds draft input.
- [ ] Test active-account scoping for defaults and metrics.
- [ ] Test with no data, realistic data, long data, and failed Health Connect.
- [ ] Run accessibility scanner/manual TalkBack pass and font scaling.
- [ ] Update README screenshots only after the design is accepted and the shown data is representative.
- [ ] Change the brief status to implemented/accepted and add a dated decision-log entry.

## New screen brief template

```markdown
---
aliases: []
tags: [sdt-fitness, design-system, screen-brief]
status: proposed
updated: YYYY-MM-DD
---

# Screen — {Name}

Parent: [[Design System Hub]]

## Status and intent
## User question
## Current implementation audit
## Product/data constraints
## Proposed information architecture
## Layout specification
## Components and token mapping
## Interaction behavior
## State matrix
## Copy deck
## Accessibility
## Responsive behavior
## Acceptance criteria
## Open decisions
```

## Change discipline

Do not overwrite historical reasoning when a design changes. Update the active brief, add the dated decision and rationale to [[Decisions - Design Log]], and mark the superseded rule. Future collaborators should be able to understand not only what the current choice is, but why it replaced the prior one.

