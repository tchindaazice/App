## 2024-11-06 - Improve Accessibility of Clickable Elements
**Learning:** Found that `clickable` modifiers in Compose without `onClickLabel` are less accessible for screen readers, as the user lacks context about what the action performs.
**Action:** Always provide an `onClickLabel` utilizing string resources when making elements clickable via `Modifier.clickable`.
