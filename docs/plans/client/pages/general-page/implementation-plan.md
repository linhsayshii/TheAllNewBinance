## General Page Implementation Plan

Status: In Progress
Owner: TBD
Last Updated: 2026-03-29

## Scope
- Implement and refine `general-page` UI from approved design.
- Keep behavior aligned with current architecture (`scene`, `page`, `component`, `service`).
- Keep CSS modular (`base`, `components`, `pages`, `themes`).

## Inputs
- Design reference: ../../designs/general-page.png
- Architecture source of truth: ../../client-ui-architecture-plan.md
- Existing page entrypoint: ../../../client/src/main/resources/fxml/pages/general-page.fxml

## Deliverables
- Updated `general-page.fxml` structure (if needed).
- Updated page/controller/viewmodel bindings.
- Updated page/component CSS for visual parity.
- Updated unit/UI tests for this page.

## Implementation Steps
1. Audit current `general-page` against design reference and list visual/function gaps.
2. Freeze component boundaries (header, navbar, search bar, theme toggle, auction card).
3. Update FXML layout and spacing to close structure gaps.
4. Update CSS tokens + page/component rules to close style gaps.
5. Validate light/dark compatibility for all updated selectors.
6. Wire or refine viewmodel data flow for card/list rendering.
7. Run compile + targeted tests; fix regressions.
8. Capture before/after screenshots for review.

## Dependencies
- Shared components should remain reusable by other pages.
- Theme variables in `themes/light.css` and `themes/dark.css` must stay backward compatible.
- Hot reload should remain functional in dev mode.

## Exit Criteria
- General page visually matches approved design at target desktop size.
- No scene load errors.
- Existing navigation and theme toggle still work.
- Related tests pass.

## Risks
- Over-customizing shared components can break other pages.
- CSS selector collisions between page and component scope.
- Hidden regressions when reworking layout containers.

## Notes
- Keep implementation details in this file.
- Keep cross-page/global decisions in `client-ui-architecture-plan.md` only.
