## 2024-05-15 - Improved SettingSwitch Accessibility
**Learning:** Replaced non-interactive entire Row clicks to specific toggleable components by moving the explicit onCheckedChange from Switch directly into a Row modifier `clickable(role = Role.Switch)` to improve accessibility for screen readers and better hit targets.
**Action:** Always wrap small interactive components within a larger semantic row using `clickable(role = Role.Switch/Button)` to provide better UX and a11y.
