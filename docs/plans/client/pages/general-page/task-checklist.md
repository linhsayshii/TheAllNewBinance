## General Page Task Checklist

Status: Active
Last Updated: 2026-03-29

## Planning
- [x] Create page-level plan files (`implementation-plan`, `acceptance-criteria`, `task-checklist`).
- [ ] Confirm latest design baseline and annotate differences.

## UI Structure
- [ ] Review `general-page.fxml` container hierarchy.
- [ ] Confirm component include boundaries (`header`, `navbar`, `search-bar`, `theme-toggle`, `auction-card`).
- [ ] Refactor FXML layout where design mismatch exists.

## Styling
- [ ] Audit `pages/general-page.css` for missing or stale selectors.
- [ ] Align component styles with design while preserving reusability.
- [ ] Verify token/theme usage for light and dark themes.

## Behavior
- [ ] Validate viewmodel bindings and placeholder data rendering.
- [ ] Verify theme toggle behavior on general page.
- [ ] Verify navigation back/forth does not break styles.

## Verification
- [ ] Compile client module.
- [ ] Run targeted unit tests.
- [ ] Run smoke/UI tests for general page.
- [ ] Capture before/after screenshots.

## Documentation
- [ ] Update architecture doc if cross-page impact is introduced.
- [ ] Mark acceptance criteria sign-off.
