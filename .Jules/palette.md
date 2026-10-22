## 2024-10-23 - Switch and IconButton Accessibility
**Learning:** Found multiple instances where semantic modifiers or semantic properties are missing for components like `Switch` and `IconButton`. In Jetpack Compose, icons without text need a `contentDescription` for accessibility.
**Action:** Adding semantic `contentDescription` appropriately for interactive components to ensure screen readers provide useful information to the user.
