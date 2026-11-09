## Palette's Journal - Critical Learnings
## $(date +%Y-%m-%d) - Added onClickLabel for screen reader support in Jetpack Compose
**Learning:** For Jetpack Compose applications, `clickable` modifiers without `onClickLabel` are poorly supported by screen readers. Explicitly defining the click action label improves accessibility.
**Action:** Always add `onClickLabel` to `clickable` modifiers and extract the strings to resources files (e.g. `strings.xml`) to support internationalization.
