## 2024-05-18 - [Accessibility] Jetpack Compose Clickable
**Learning:** Found that `clickable` modifiers in Jetpack Compose lacked `onClickLabel` which is an accessibility issue for screen readers.
**Action:** Always add `onClickLabel` with a descriptive, localized string resource to `clickable` modifiers for better screen reader accessibility.
