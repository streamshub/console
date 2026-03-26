import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type EmptyStateVariant = 'xs' | 'sm' | 'lg' | 'xl' | 'full';

/**
 * PatternFly Empty State Component
 * A lightweight wrapper around PatternFly v6 empty state classes
 * 
 * @element pf-empty-state
 * 
 * @attr {EmptyStateVariant} variant - Size variant
 * @attr {string} title - Empty state title
 * @attr {string} icon - Icon class or content
 * 
 * @slot - Body content
 * @slot icon - Icon slot
 * @slot actions - Action buttons
 * @slot secondary - Secondary actions
 * 
 * @example
 * <pf-empty-state title="No results found">
 *   <p>Try adjusting your search criteria</p>
 *   <pf-button slot="actions" variant="primary">Clear filters</pf-button>
 * </pf-empty-state>
 */
@customElement('pf-empty-state')
export class PfEmptyState extends LitElement {
  @property({ type: String }) variant: EmptyStateVariant = 'lg';
  @property({ type: String }) title = '';
  @property({ type: String }) icon = '';

  static styles = css`
    :host {
      display: block;
    }
  `;

  render() {
    const classes = [
      'pf-v6-c-empty-state',
      this.variant && `pf-m-${this.variant}`,
    ].filter(Boolean).join(' ');

    return html`
      <div class=${classes}>
        <div class="pf-v6-c-empty-state__content">
          ${this.icon || this.querySelector('[slot="icon"]') ? html`
            <div class="pf-v6-c-empty-state__icon">
              <slot name="icon">${this.icon}</slot>
            </div>
          ` : ''}
          
          ${this.title ? html`
            <h2 class="pf-v6-c-empty-state__title">
              <span class="pf-v6-c-empty-state__title-text">${this.title}</span>
            </h2>
          ` : ''}
          
          <div class="pf-v6-c-empty-state__body">
            <slot></slot>
          </div>

          ${this.querySelector('[slot="actions"]') ? html`
            <div class="pf-v6-c-empty-state__actions">
              <slot name="actions"></slot>
            </div>
          ` : ''}

          ${this.querySelector('[slot="secondary"]') ? html`
            <div class="pf-v6-c-empty-state__secondary">
              <slot name="secondary"></slot>
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-empty-state': PfEmptyState;
  }
}