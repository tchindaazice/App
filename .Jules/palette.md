## 2024-10-24 - Expand Switch Tap Targets
**Learning:** Making the entire row containing a label and a toggle switch clickable, rather than just the switch itself, significantly improves usability and accessibility by providing a much larger tap target.
**Action:** Always wrap toggle switches and their associated text in a toggleable container (like a Row with Modifier.toggleable and Role.Switch) to ensure both a larger tap target and correct semantics for screen readers. Disable the individual switch's click listener to avoid duplicate events.
