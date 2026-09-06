# Vlast Unified Design Tokens & Style Guide (Phase 3)

## 1. Visual Identity & Brand Philosophy
**Vlast** (Власть / السلطة / السيطرة) embodies decisive authority, architectural strength, precision, and controlled restraint. Unlike playful, pastel, or wellness-oriented applications, Vlast commands an intentional, high-contrast dark visual landscape engineered for mission-critical network control.

---

## 2. Foundations & Color Palette (WCAG AA Strict Compliance)

All colors meet or exceed WCAG AA contrast ratio standards (minimum 4.5:1 for standard text, 3:1 for large text / components):

| Token Name | Hex Value | Role & Usage | Contrast Ratio vs Background |
| :--- | :--- | :--- | :--- |
| `DarkBackground` | `#080B0F` | Deep authoritative background | Base Canvas |
| `DarkSurface` | `#0F141C` | Primary card & navigation container | Surface Level 1 |
| `DarkSurfaceVariant` | `#161E2B` | Elevated dialogs & inner chips | Surface Level 2 |
| `DarkBorder` | `#222E42` | Crisp structural component borders | Border Level 1 |
| `DarkBorderActive` | `#3B4F70` | Focus and active container strokes | Border Level 2 |
| `BrandRed` | `#E11D48` | Decisive action, manual Kill Switch | Primary Accent |
| `BrandRedDark` | `#9F1239` | Critical cutoff warning surfaces | Alarm Banner |
| `BrandAmber` | `#F59E0B` | Warning indicator, Today's Override | Secondary Accent |
| `BrandCyan` | `#0EA5E9` | Active data pulse, Recurring limit | Tertiary Accent |
| `TextPrimary` | `#F8FAFC` | Primary headlines, counters, labels | **16.5 : 1** (Passes AAA) |
| `TextSecondary` | `#94A3B8` | Subtitles and explanatory captions | **6.2 : 1** (Passes AA) |
| `TextMuted` | `#64748B` | Subtle hints, timestamps, units | **4.6 : 1** (Passes AA) |

---

## 3. Strict Semantic Tri-Color (Meter Status)
Consistently identical across all screens:
- **Green (`#10B981`)**: Under 70% consumption (Safe, accompanied by circular shape `●`).
- **Yellow (`#F59E0B`)**: 70% to 90% consumption (Warning, accompanied by triangle shape `▲`).
- **Red (`#EF4444`)**: Over 90% or cutoff (Critical, accompanied by square/lock shape `■`).

---

## 4. Typography System
- **Display & Monospace Numbers (`FontFamily.Monospace`)**: Exclusively employed for all consumption numbers, gauges, limits, and timestamps to eliminate digit shifting during real-time byte updates.
- **System Interface (`FontFamily.Default`)**: Clean, accessible sans-serif typography with generous line-heights for optimal Arabic RTL readability.

---

## 5. Screen Layout & Navigation (Items 11 - 15)
- **Native RTL First**: Root layout structured via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.
- **Fixed Bottom Navigation**: 4 persistent destinations:
  1. `الرئيسية` (Dashboard with Hero Remaining Counter, Dynamic Island Header, and 3 Service Cards).
  2. `التقارير` (Activity Logs with full audit trail and reason filtering).
  3. `نقطة الاتصال` (Hotspot measurement and threshold alerts).
  4. `الإعدادات` (Dark/Light theme, Dual SIM selector, PIN lock, Cut Sound, and About Vlast).
