# Styling Guide for StreamsHub Console (Lit + Web Components)

## PatternFly v5 Integration

This application uses **PatternFly v5** CSS variables for consistent styling across all components.

### CSS Variable System

PatternFly v5 uses CSS variables with the prefix `--pf-v5-global--`:

```css
/* Colors */
--pf-v5-global--primary-color--100
--pf-v5-global--success-color--100
--pf-v5-global--danger-color--100
--pf-v5-global--warning-color--100
--pf-v5-global--info-color--100
--pf-v5-global--Color--100
--pf-v5-global--Color--200

/* Spacing */
--pf-v5-global--spacer--xs
--pf-v5-global--spacer--sm
--pf-v5-global--spacer--md
--pf-v5-global--spacer--lg
--pf-v5-global--spacer--xl

/* Typography */
--pf-v5-global--FontSize--sm
--pf-v5-global--FontSize--md
--pf-v5-global--FontSize--lg
--pf-v5-global--FontWeight--normal
--pf-v5-global--FontWeight--bold

/* Borders */
--pf-v5-global--BorderWidth--sm
--pf-v5-global--BorderRadius--sm
--pf-v5-global--BorderColor--100

/* Backgrounds */
--pf-v5-global--BackgroundColor--100
--pf-v5-global--BackgroundColor--200
```

## Shared Styles Library

Common styles are centralized in `src/styles/shared-styles.ts` to avoid duplication:

### Available Style Modules

1. **`tableStyles`** - Common table styling
   - Sortable columns with indicators
   - Hover states
   - Link styling

2. **`pageStyles`** - Page layout patterns
   - Toolbar with search and filters
   - Pagination controls
   - Loading and error states

3. **`badgeStyles`** - Badges and labels
   - Primary, info, success, warning, danger variants
   - Label groups for tags

4. **`statusStyles`** - Status indicators
   - Success, info, warning, danger colors
   - Icon spacing

5. **`utilityStyles`** - Common utilities
   - Text truncation
   - Responsive helpers

### Usage Example

```typescript
import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import { tableStyles, pageStyles, statusStyles } from '../styles/shared-styles';

@customElement('my-component')
export class MyComponent extends LitElement {
  static styles = [
    tableStyles,
    pageStyles,
    statusStyles,
    css`
      /* Component-specific styles */
      .custom-class {
        color: var(--pf-v5-global--primary-color--100);
      }
    `
  ];

  render() {
    return html`<div>Content</div>`;
  }
}
```

## Best Practices

### 1. Use Shared Styles First

Before writing custom styles, check if a shared style module exists:

```typescript
// ✅ Good - Use shared styles
import { tableStyles, pageStyles } from '../styles/shared-styles';
static styles = [tableStyles, pageStyles];

// ❌ Avoid - Duplicating common styles
static styles = css`
  table { width: 100%; border-collapse: collapse; }
  /* ... */
`;
```

### 2. Use CSS Variables

Always use PatternFly v5 CSS variables instead of hardcoded values:

```css
/* ✅ Good - Uses CSS variables */
.my-element {
  padding: var(--pf-v5-global--spacer--md);
  color: var(--pf-v5-global--primary-color--100);
  border-radius: var(--pf-v5-global--BorderRadius--sm);
}

/* ❌ Avoid - Hardcoded values */
.my-element {
  padding: 16px;
  color: #0066cc;
  border-radius: 3px;
}
```

### 3. Compose Styles

Lit allows composing multiple style modules:

```typescript
static styles = [
  tableStyles,      // Base table styles
  statusStyles,     // Status indicators
  css`
    /* Component-specific overrides */
    .special-row {
      background: var(--pf-v5-global--BackgroundColor--200);
    }
  `
];
```

### 4. Semantic Color Usage

Use semantic color variables for meaning:

```css
/* Status colors */
.success { color: var(--pf-v5-global--success-color--100); }
.warning { color: var(--pf-v5-global--warning-color--100); }
.danger { color: var(--pf-v5-global--danger-color--100); }
.info { color: var(--pf-v5-global--info-color--100); }

/* Primary colors */
.primary { color: var(--pf-v5-global--primary-color--100); }
.link { color: var(--pf-v5-global--link--Color); }
```

## Loading PatternFly CSS

The PatternFly v5 CSS is loaded in `index.html`:

```html
<link rel="stylesheet" href="https://unpkg.com/@patternfly/patternfly@5/patternfly.css">
```

Or install via npm:

```bash
npm install @patternfly/patternfly@5
```

Then import in your main entry point:

```typescript
import '@patternfly/patternfly/patternfly.css';
```

## Component-Specific Styles

When a component needs unique styling not covered by shared styles:

```typescript
static styles = [
  tableStyles,  // Include shared styles
  css`
    /* Component-specific styles */
    .unique-feature {
      /* Use CSS variables */
      margin: var(--pf-v5-global--spacer--lg);
    }
  `
];
```

## Resources

- [PatternFly v5 Documentation](https://www.patternfly.org/v5/)
- [PatternFly CSS Variables](https://www.patternfly.org/v5/developer-resources/global-css-variables)
- [Lit Styling Guide](https://lit.dev/docs/components/styles/)