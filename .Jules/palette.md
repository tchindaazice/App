## 2024-05-23 - Compose Clickable Accessibility
**Learning:** In Jetpack Compose, the `clickable` modifier does not automatically provide a descriptive action label to screen readers like TalkBack (it just says "double tap to activate").
**Action:** Always add `onClickLabel` with a localized string resource when using the `clickable` modifier to ensure screen reader users understand the action they are about to perform.
