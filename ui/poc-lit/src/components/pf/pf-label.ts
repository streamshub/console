import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type LabelColor = 'blue' | 'cyan' | 'green' | 'orange' | 'purple' | 'red' | 'grey' | 'gold';

/**
 * PatternFly Label Component
 * A lightweight wrapper around PatternFly v6 label classes
 * 
 * @element pf-label
 * 
 * @attr {LabelColor} color - Label color variant
 * @attr {boolean} compact - Use compact size
 * @attr {boolean} outline - Use outline style
 * @attr {boolean} closable - Show close button
 * 
 * @fires close - Dispatched when close button is clicked
 * 
 * @slot - Label text content
 * @slot icon - Icon slot
 * 
 * @example
 * <pf-label color="blue">Status: Active</pf-label>
 * <pf-label color="red" closable>Error</pf-label>
 */
@customElement('pf-label')
export class PfLabel extends LitElement {
  @property({ type: String }) color: LabelColor = 'grey';
  @property({ type: Boolean }) compact = false;
  @property({ type: Boolean }) outline = false;
  @property({ type: Boolean }) closable = false;

  static styles = css`
    :host {
      display: inline-block;
    }
  `;

  private handleClose(e: Event) {
    e.stopPropagation();
    this.dispatchEvent(new CustomEvent('close', {
      bubbles: true,
      composed: true,
    }));
  }

  render() {
    const classes = [
      'pf-v6-c-label',
      `pf-m-${this.color}`,
      this.compact && 'pf-m-compact',
      this.outline && 'pf-m-outline',
    ].filter(Boolean).join(' ');

    return html`
      <span class=${classes}>
        <span class="pf-v6-c-label__content">
          <slot name="icon"></slot>
          <span class="pf-v6-c-label__text">
            <slot></slot>
          </span>
        </span>
        ${this.closable ? html`
          <button 
            class="pf-v6-c-button pf-m-plain" 
            type="button"
            @click=${this.handleClose}
            aria-label="Close"
          >
            <span class="pf-v6-c-label__icon">
              <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 352 512" aria-hidden="true" role="img">
                <path d="M242.72 256l100.07-100.07c12.28-12.28 12.28-32.19 0-44.48l-22.24-22.24c-12.28-12.28-32.19-12.28-44.48 0L176 189.28 75.93 89.21c-12.28-12.28-32.19-12.28-44.48 0L9.21 111.45c-12.28 12.28-12.28 32.19 0 44.48L109.28 256 9.21 356.07c-12.28 12.28-12.28 32.19 0 44.48l22.24 22.24c12.28 12.28 32.2 12.28 44.48 0L176 322.72l100.07 100.07c12.28 12.28 32.2 12.28 44.48 0l22.24-22.24c12.28-12.28 12.28-32.19 0-44.48L242.72 256z"></path>
              </svg>
            </span>
          </button>
        ` : ''}
      </span>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-label': PfLabel;
  }
}