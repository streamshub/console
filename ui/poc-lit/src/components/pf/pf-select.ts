import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

/**
 * PatternFly Select Component
 * A lightweight wrapper around PatternFly v6 form select classes
 * 
 * @element pf-select
 * 
 * @attr {string} value - Selected value
 * @attr {string} placeholder - Placeholder text
 * @attr {boolean} disabled - Whether select is disabled
 * @attr {boolean} required - Whether select is required
 * @attr {string} name - Select name attribute
 * @attr {string} aria-label - Accessibility label
 * @attr {SelectOption[]} options - Select options
 * 
 * @fires change - Dispatched when selection changes
 * 
 * @example
 * <pf-select 
 *   .options=${[{value: '1', label: 'Option 1'}, {value: '2', label: 'Option 2'}]}
 *   @change=${handleChange}>
 * </pf-select>
 */
@customElement('pf-select')
export class PfSelect extends LitElement {
  @property({ type: String }) value = '';
  @property({ type: String }) placeholder = 'Select an option';
  @property({ type: Boolean }) disabled = false;
  @property({ type: Boolean }) required = false;
  @property({ type: String }) name = '';
  @property({ type: String, attribute: 'aria-label' }) ariaLabel = '';
  @property({ type: Array }) options: SelectOption[] = [];

  static styles = css`
    :host {
      display: inline-block;
      width: 100%;
    }
  `;

  private handleChange(e: Event) {
    const select = e.target as HTMLSelectElement;
    this.value = select.value;
    this.dispatchEvent(new CustomEvent('change', {
      detail: { value: this.value },
      bubbles: true,
      composed: true,
    }));
  }

  render() {
    return html`
      <select
        class="pf-v6-c-form-control"
        .value=${this.value}
        ?disabled=${this.disabled}
        ?required=${this.required}
        name=${this.name}
        aria-label=${this.ariaLabel || this.placeholder}
        @change=${this.handleChange}
      >
        ${this.placeholder ? html`
          <option value="" ?disabled=${this.required}>${this.placeholder}</option>
        ` : ''}
        ${this.options.map(option => html`
          <option 
            value=${option.value}
            ?disabled=${option.disabled}
            ?selected=${this.value === option.value}
          >
            ${option.label}
          </option>
        `)}
      </select>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-select': PfSelect;
  }
}