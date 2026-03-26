import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export type BadgeVariant = 'read' | 'unread';

/**
 * PatternFly Badge Component
 * A lightweight wrapper around PatternFly v6 badge classes
 * 
 * @element pf-badge
 * 
 * @attr {BadgeVariant} variant - Badge variant (read or unread)
 * @attr {string} count - Badge count/text
 * 
 * @slot - Badge content (alternative to count attribute)
 * 
 * @example
 * <pf-badge count="5"></pf-badge>
 * <pf-badge variant="unread">New</pf-badge>
 */
@customElement('pf-badge')
export class PfBadge extends LitElement {
  @property({ type: String }) variant: BadgeVariant = 'read';
  @property({ type: String }) count = '';

  static styles = css`
    :host {
      display: inline-block;
    }
  `;

  render() {
    const classes = [
      'pf-v6-c-badge',
      this.variant === 'unread' && 'pf-m-unread',
    ].filter(Boolean).join(' ');

    return html`
      <span class=${classes}>
        ${this.count || html`<slot></slot>`}
      </span>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-badge': PfBadge;
  }
}