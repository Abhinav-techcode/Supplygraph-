# SupplyGraph Enterprise Design System (Phase 14)
## Design Tokens, Component Standards & UX Guidelines

**Status:** APPROVED & IMPLEMENTED  
**Date:** November 18, 2026  
**Architecture Lead:** Principal Enterprise UI/UX Architect  
**Location:** `supplygraph-web/src/main/frontend/themes/supplygraph/styles.css`

---

## 1. Overview & Architectural Philosophy

The SupplyGraph Design System extends the Vaadin Lumo theme to deliver an information-dense, high-precision visual language suitable for mission-critical industrial supply chain operations. It combines:
- **Calm, High-Contrast Typography & Surfaces:** Minimizes cognitive load during severe operational disruptions.
- **Strict Color Semantics:** Status colors convey unambiguous operational state across financial risk, supplier compliance, finite capacity, and quality holds.
- **Information-Dense Surfaces:** Standardized card layouts (`sg-card`, `sg-kpi-card`) with consistent margins, padding, and subtle elevations (`--sg-shadow-xs` through `--sg-shadow-lg`).

---

## 2. Design Token Architecture

### 2.1 Brand & Neutral Palettes
| Token | Value | Purpose |
|---|---|---|
| `--sg-color-brand-primary` | `#0F172A` (Slate 900) | Primary headers, top navigation, brand grounding |
| `--sg-color-brand-accent` | `#2563EB` (Blue 600) | Primary interactive actions, focus rings, selected items |
| `--sg-color-brand-secondary` | `#0D9488` (Teal 600) | Secondary analytical tools, provenance edges, links |
| `--lumo-base-color` | System Dynamic | Light / Dark surface base |
| `--lumo-contrast-10pct` | Translucent | Subtle divider borders (`--sg-border-subtle`) |

### 2.2 Semantic Operational Status Tokens
| Semantic Role | Hex Value | CSS Class | Operational Meaning |
|---|---|---|---|
| **Success** | `#10B981` | `.sg-badge-success` | Approved plans, verified CoAs, compliant CBAM/EUDR |
| **Warning** | `#F59E0B` | `.sg-badge-warning` | Pending approvals, high capacity load, minor delays |
| **Danger / Critical** | `#EF4444` | `.sg-badge-danger` | Systemic disruptions, 8D critical defects, stockouts |
| **Info / Primary** | `#3B82F6` | `.sg-badge-info` | Active RFQs, in-transit ASNs, published tenders |
| **Neutral** | `#64748B` | `.sg-badge-neutral` | Archived records, draft items, unverified logs |

### 2.3 Elevation & Surface Shadows
```css
--sg-shadow-xs: 0 1px 2px 0 rgb(0 0 0 / 0.05);
--sg-shadow-sm: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
--sg-shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
--sg-shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
```

---

## 3. Standard Component Patterns

### 3.1 KPI Metrics Card (`.sg-kpi-card`)
Provides a consistent vertical layout for high-altitude metrics:
- `.sg-kpi-title`: Small uppercase label (font-size: `var(--lumo-font-size-s)`, color: secondary text).
- `.sg-kpi-value`: Large bold display (font-size: `var(--lumo-font-size-xxl)`, bold, optional semantic theme badge).
- `.sg-kpi-subtitle`: Micro explanatory text describing scope or derivation.

### 3.2 High-Density Data Grid
- Alternating subtle row backgrounds.
- Header cells styled with uppercase tracked typography and 5% contrast background.
- Auto-width columns with wrapped text cells for long titles.
- Action buttons anchored in fixed right-aligned columns.

### 3.3 Command Center Search Bar (`.sg-search-bar`)
- Max-width 650px responsive search field.
- Immediate client-side and eager reactive filtering over cross-domain entities.
- Direct drill-down link buttons.

