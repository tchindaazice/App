## 2024-05-19 - Dynamic Accessibility Labels for State-Toggling Buttons
**Learning:** For toggle buttons (like a VPN power button), a static `contentDescription` (e.g., "Activer VPN") is misleading when the state has already changed (e.g., the VPN is already connected). Screen readers will announce "Activer VPN" even if the action will disconnect it.
**Action:** Always use dynamic ARIA labels or `contentDescription` attributes that reflect the *action* the button will perform in its current state (e.g., switch between "Activer VPN" and "Désactiver VPN").
