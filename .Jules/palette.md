## 2025-02-18 - Jetpack Compose Clickable Accessibility
**Learning:** In Jetpack Compose, the `clickable` modifier does not automatically provide semantic meaning to screen readers. Relying solely on `clickable` without context can leave visually impaired users unsure of the action being performed.
**Action:** Always include an `onClickLabel` inside the `clickable` modifier (using `stringResource`) to provide a clear description of the action, e.g., `.clickable(onClickLabel = stringResource(R.string.a11y_open_country_selector)) { ... }`.
