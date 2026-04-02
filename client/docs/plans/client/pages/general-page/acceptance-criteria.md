## General Page Acceptance Criteria

Status: Draft
Last Updated: 2026-03-29

## Functional Criteria
1. General page loads without exception from `SceneRegistry.GENERAL_PAGE`.
2. Header and navbar are visible and aligned with design hierarchy.
3. Search bar is visible and interactive.
4. Theme toggle is visible and can switch themes without breaking layout.
5. Auction card area renders expected item rows/cards from viewmodel data.

## Visual Criteria
1. Spacing and alignment are consistent with design reference image.
2. Typography scale and weight follow token/theme system.
3. Colors come from theme variables (no hardcoded one-off page colors unless justified).
4. Layout remains stable at default window size defined in `AppConfig`.

## Quality Criteria
1. No duplicate/unused selectors introduced in page CSS.
2. Shared component CSS remains reusable by other pages.
3. Unit tests touching general-page logic pass.
4. Smoke/UI tests for general page pass.

## Non-Goals
- Full business workflow implementation for other pages.
- Backend feature changes unrelated to general-page rendering.

## Sign-off Checklist
- [ ] Functional criteria validated
- [ ] Visual criteria validated
- [ ] Quality criteria validated
- [ ] Team review sign-off recorded
