import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * PatternFly Checkbox Component
 * A lightweight wrapper around PatternFly v6 checkbox classes
 * 
 * @element pf-checkbox
 * 
 * @attr {boolean} checked - Whether checkbox is checked
 * @attr {boolean} disabled - Whether checkbox is disabled
 * @attr {boolean} required - Whether checkbox is required
 * @attr {string} name - Checkbox name attribute
 * @attr {string} value - Checkbox value attribute
 * @attr {string} label - Checkbox label text
 * @attr {string} description - Optional description text
 * 
 * @fires change - Dispatched when checkbox state changes
 * 
 * @slot - Label content (alternative to label attribute)
 * @slot description - Description content
 * 
 * @example
 * <pf-checkbox label="Accept terms" @change=${handleChange}></pf-checkbox>
 * <pf-checkbox checked disabled label="Read only"></pf-checkbox>
 */
@customElement('pf-checkbox')
export class PfCheckbox extends LitElement {
  @property({ type: Boolean }) checked = false;
  @property({ type: Boolean }) disabled = false;
  @property({ type: Boolean }) required = false;
  @property({ type: String }) name = '';
  @property({ type: String }) value = '';
  @property({ type: String }) label = '';
  @property({ type: String }) description = '';

  static styles = css`
    :host {
      display: block;
    }
  `;

  private handleChange(e: Event) {
    const input = e.target as HTMLInputElement;
    this.checked = input.checked;
    this.dispatchEvent(new CustomEvent('change', {
      detail: { checked: this.checked, value: this.value },
      bubbles: true,
      composed: true,
    }));
  }

  render() {
    const id = `checkbox-${Math.random().toString(36).substr(2, 9)}`;

    return html`
      <div class="pf-v6-c-check">
        <input
          class="pf-v6-c-check__input"
          type="checkbox"
          id=${id}
          .checked=${this.checked}
          ?disabled=${this.disabled}
          ?required=${this.required}
          name=${this.name}
          value=${this.value}
          @change=${this.handleChange}
        />
        ${this.label || this.querySelector('[slot]') ? html`
          <label class="pf-v6-c-check__label" for=${id}>
            ${this.label || html`<slot></slot>`}
          </label>
        ` : ''}
        ${this.description || this.querySelector('[slot="description"]') ? html`
          <span class="pf-v6-c-check__description">
            ${this.description || html`<slot name="description"></slot>`}
          </span>
        ` : ''}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-checkbox': PfCheckbox;
  }
}