## 2024-05-18 - Expand Tap Targets for Switches
**Learning:** When using Switch components in a Row (like SettingSwitch), making only the Switch widget clickable creates a poor, frustrating UX because the tap target is too small. Applying the `.toggleable(role = Role.Switch)` modifier to the parent Row dramatically increases the tap area and improves accessibility.
**Action:** Always make the entire Row toggleable for boolean settings.
