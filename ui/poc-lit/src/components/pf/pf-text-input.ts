import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type TextInputType = 'text' | 'email' | 'password' | 'search' | 'tel' | 'url' | 'number';

/**
 * PatternFly Text Input Component
 * A lightweight wrapper around PatternFly v6 form control classes
 * 
 * @element pf-text-input
 * 
 * @attr {TextInputType} type - Input type
 * @attr {string} value - Input value
 * @attr {string} placeholder - Placeholder text
 * @attr {boolean} disabled - Whether input is disabled
 * @attr {boolean} readonly - Whether input is readonly
 * @attr {boolean} required - Whether input is required
 * @attr {string} name - Input name attribute
 * @attr {string} aria-label - Accessibility label
 * 
 * @fires input - Dispatched when input value changes
 * @fires change - Dispatched when input loses focus
 * 
 * @example
 * <pf-text-input placeholder="Enter text" @input=${handleInput}></pf-text-input>
 * <pf-text-input type="search" placeholder="Search..."></pf-text-input>
 */
@customElement('pf-text-input')
export class PfTextInput extends LitElement {
  @property({ type: String }) type: TextInputType = 'text';
  @property({ type: String }) value = '';
  @property({ type: String }) placeholder = '';
  @property({ type: Boolean }) disabled = false;
  @property({ type: Boolean }) readonly = false;
  @property({ type: Boolean }) required = false;
  @property({ type: String }) name = '';
  @property({ type: String, attribute: 'aria-label' }) ariaLabel = '';

  static styles = css`
    :host {
      display: inline-block;
      width: 100%;
    }
  `;

  private handleInput(e: Event) {
    const input = e.target as HTMLInputElement;
    this.value = input.value;
    this.dispatchEvent(new CustomEvent('input', {
      detail: { value: this.value },
      bubbles: true,
      composed: true,
    }));
  }

  private handleChange(e: Event) {
    this.dispatchEvent(new CustomEvent('change', {
      detail: { value: this.value },
      bubbles: true,
      composed: true,
    }));
  }

  render() {
    return html`
      <input
        class="pf-v6-c-form-control"
        type=${this.type}
        .value=${this.value}
        placeholder=${this.placeholder}
        ?disabled=${this.disabled}
        ?readonly=${this.readonly}
        ?required=${this.required}
        name=${this.name}
        aria-label=${this.ariaLabel || this.placeholder}
        @input=${this.handleInput}
        @change=${this.handleChange}
      />
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-text-input': PfTextInput;
  }
}