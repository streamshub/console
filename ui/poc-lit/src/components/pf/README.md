# PatternFly Web Components

Lightweight Lit-based web components that wrap PatternFly v6 CSS classes. These components provide a declarative API while leveraging PatternFly's existing styles.

## Philosophy

These components are **thin wrappers** around PatternFly v6 CSS classes. They:
- Use PatternFly CSS classes directly (e.g., `pf-v6-c-button`, `pf-v6-c-card`)
- Provide minimal custom CSS (only for layout/display properties)
- Offer a declarative, component-based API
- Maintain full compatibility with PatternFly v6 design system

## Available Components

### Core Components

#### `<pf-button>`
Button component with various style variants.

```html
<pf-button variant="primary">Save</pf-button>
<pf-button variant="secondary">Cancel</pf-button>
<pf-button variant="danger" disabled>Delete</pf-button>
<pf-button variant="link">Learn more</pf-button>
<pf-button loading>Processing...</pf-button>
```

**Attributes:**
- `variant`: `'primary' | 'secondary' | 'tertiary' | 'danger' | 'warning' | 'link' | 'plain' | 'control'`
- `disabled`: boolean
- `loading`: boolean (shows spinner)
- `type`: `'button' | 'submit' | 'reset'`
- `small`: boolean
- `block`: boolean (full width)

#### `<pf-card>`
Flexible container component.

```html
<pf-card title="My Card">
  <p>Card content</p>
</pf-card>

<pf-card compact rounded>
  <div slot="header">Custom Header</div>
  <p>Body content</p>
  <div slot="footer">Footer</div>
</pf-card>
```

**Attributes:**
- `title`: string
- `compact`: boolean
- `flat`: boolean (no border/shadow)
- `rounded`: boolean
- `hoverable`: boolean
- `selectable`: boolean
- `selected`: boolean

**Slots:**
- Default: Card body
- `header`: Custom header
- `footer`: Footer content
- `actions`: Action buttons (top right)

#### `<pf-badge>`
Badge for counts or status.

```html
<pf-badge count="5"></pf-badge>
<pf-badge variant="unread">New</pf-badge>
```

**Attributes:**
- `variant`: `'read' | 'unread'`
- `count`: string

#### `<pf-label>`
Label/tag component with colors.

```html
<pf-label color="blue">Status: Active</pf-label>
<pf-label color="red" closable @close=${handleClose}>Error</pf-label>
<pf-label color="green" outline>Success</pf-label>
```

**Attributes:**
- `color`: `'blue' | 'cyan' | 'green' | 'orange' | 'purple' | 'red' | 'grey' | 'gold'`
- `compact`: boolean
- `outline`: boolean
- `closable`: boolean

**Events:**
- `close`: Dispatched when close button clicked

### Form Components

#### `<pf-text-input>`
Text input field.

```html
<pf-text-input 
  placeholder="Enter text"
  @input=${handleInput}>
</pf-text-input>

<pf-text-input 
  type="search"
  value="initial"
  required>
</pf-text-input>
```

**Attributes:**
- `type`: `'text' | 'email' | 'password' | 'search' | 'tel' | 'url' | 'number'`
- `value`: string
- `placeholder`: string
- `disabled`: boolean
- `readonly`: boolean
- `required`: boolean
- `name`: string
- `aria-label`: string

**Events:**
- `input`: Dispatched on value change
- `change`: Dispatched on blur

#### `<pf-select>`
Select dropdown.

```html
<pf-select 
  .options=${[
    {value: '1', label: 'Option 1'},
    {value: '2', label: 'Option 2'}
  ]}
  @change=${handleChange}>
</pf-select>
```

**Attributes:**
- `value`: string
- `placeholder`: string
- `disabled`: boolean
- `required`: boolean
- `name`: string
- `options`: `SelectOption[]`

**Events:**
- `change`: Dispatched on selection change

#### `<pf-checkbox>`
Checkbox with label.

```html
<pf-checkbox 
  label="Accept terms"
  @change=${handleChange}>
</pf-checkbox>

<pf-checkbox checked disabled>
  <span>Custom label content</span>
  <span slot="description">Helper text</span>
</pf-checkbox>
```

**Attributes:**
- `checked`: boolean
- `disabled`: boolean
- `required`: boolean
- `name`: string
- `value`: string
- `label`: string
- `description`: string

**Events:**
- `change`: Dispatched on state change

### Layout Components

#### `<pf-toolbar>`
Toolbar for filters and actions.

```html
<pf-toolbar>
  <div slot="filter">
    <pf-text-input placeholder="Search..."></pf-text-input>
  </div>
  <div slot="actions">
    <pf-button>Create</pf-button>
  </div>
</pf-toolbar>
```

**Attributes:**
- `sticky`: boolean (sticky at top)

**Slots:**
- `filter`: Filter section
- `actions`: Action buttons
- `pagination`: Pagination controls

#### `<pf-empty-state>`
Empty state for no data or errors.

```html
<pf-empty-state title="No results found">
  <p>Try adjusting your search criteria</p>
  <pf-button slot="actions" variant="primary">
    Clear filters
  </pf-button>
</pf-empty-state>
```

**Attributes:**
- `variant`: `'xs' | 'sm' | 'lg' | 'xl' | 'full'`
- `title`: string
- `icon`: string

**Slots:**
- Default: Body content
- `icon`: Icon content
- `actions`: Primary actions
- `secondary`: Secondary actions

### Data Display Components

#### `<pf-table>`
Table with sortable columns.

```html
<pf-table 
  .columns=${[
    {key: 'name', label: 'Name', sortable: true},
    {key: 'status', label: 'Status'}
  ]}
  @sort=${handleSort}>
  <tr class="pf-v6-c-table__tr">
    <td class="pf-v6-c-table__td">Item 1</td>
    <td class="pf-v6-c-table__td">Active</td>
  </tr>
</pf-table>
```

**Attributes:**
- `columns`: `TableColumn[]`
- `compact`: boolean
- `borders`: boolean
- `striped`: boolean
- `caption`: string

**Events:**
- `sort`: Dispatched when sortable column clicked

#### `<pf-pagination>`
Pagination controls.

```html
<pf-pagination 
  page="1"
  perPage="20"
  total="100"
  @page-change=${handlePageChange}>
</pf-pagination>
```

**Attributes:**
- `page`: number (1-based)
- `perPage`: number
- `total`: number
- `compact`: boolean

**Events:**
- `page-change`: Dispatched when page changes

## Usage

### Import Components

```typescript
// Import individual components
import './components/pf/pf-button';
import './components/pf/pf-card';

// Or import from index
import './components/pf';
```

### Use in Templates

```typescript
import { html } from 'lit';

const template = html`
  <pf-card title="Example">
    <pf-text-input placeholder="Search"></pf-text-input>
    <pf-button variant="primary" @click=${handleClick}>
      Submit
    </pf-button>
  </pf-card>
`;
```

## Development Guidelines

1. **Use PatternFly CSS classes** - Don't reimplement styles
2. **Minimal custom CSS** - Only for component structure (display, host)
3. **Follow PatternFly patterns** - Match official component structure
4. **Provide TypeScript types** - Export interfaces and types
5. **Document thoroughly** - JSDoc comments and examples
6. **Test with PatternFly CSS** - Ensure compatibility with PF v6

## Resources

- [PatternFly v6 Documentation](https://www.patternfly.org/)
- [PatternFly CSS Variables](https://www.patternfly.org/developer-resources/global-css-variables)
- [Lit Documentation](https://lit.dev/)