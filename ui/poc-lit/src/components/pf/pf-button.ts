import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'danger' | 'warning' | 'link' | 'plain' | 'control';

/**
 * PatternFly Button Component
 * A lightweight wrapper around PatternFly v6 button classes
 * 
 * @element pf-button
 * 
 * @attr {ButtonVariant} variant - Button style variant (default: 'primary')
 * @attr {boolean} disabled - Whether the button is disabled
 * @attr {boolean} loading - Show loading spinner
 * @attr {string} type - Button type attribute (button, submit, reset)
 * @attr {boolean} small - Use small size
 * @attr {boolean} block - Full width button
 * 
 * @fires click - Dispatched when button is clicked (not when disabled)
 * 
 * @slot - Button content
 * @slot icon - Icon slot (appears before text)
 * 
 * @example
 * <pf-button variant="primary">Click me</pf-button>
 * <pf-button variant="danger" disabled>Delete</pf-button>
 * <pf-button variant="secondary" loading>Loading...</pf-button>
 */
@customElement('pf-button')
export class PfButton extends LitElement {
  @property({ type: String }) variant: ButtonVariant = 'primary';
  @property({ type: Boolean }) disabled = false;
  @property({ type: Boolean }) loading = false;
  @property({ type: String }) type: 'button' | 'submit' | 'reset' = 'button';
  @property({ type: Boolean }) small = false;
  @property({ type: Boolean }) block = false;

  static styles = css`
    :host {
      display: inline-block;
    }

    :host([block]) {
      display: block;
    }

    :host([block]) button {
      width: 100%;
    }
  `;

  private handleClick(e: MouseEvent) {
    if (this.disabled || this.loading) {
      e.preventDefault();
      e.stopPropagation();
      return;
    }
  }

  render() {
    const classes = [
      'pf-v6-c-button',
      `pf-m-${this.variant}`,
      this.small && 'pf-m-small',
      this.block && 'pf-m-block',
    ].filter(Boolean).join(' ');

    return html`
      <button
        class=${classes}
        type=${this.type}
        ?disabled=${this.disabled || this.loading}
        @click=${this.handleClick}
      >
        ${this.loading ? html`
          <span class="pf-v6-c-spinner pf-m-sm" role="progressbar">
            <span class="pf-v6-c-spinner__clipper"></span>
            <span class="pf-v6-c-spinner__lead-ball"></span>
            <span class="pf-v6-c-spinner__tail-ball"></span>
          </span>
        ` : html`<slot name="icon"></slot>`}
        <slot></slot>
      </button>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-button': PfButton;
  }
}