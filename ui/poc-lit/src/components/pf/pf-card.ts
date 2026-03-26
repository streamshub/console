import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * PatternFly Card Component
 * A lightweight wrapper around PatternFly v6 card classes
 * 
 * @element pf-card
 * 
 * @attr {string} title - Card title
 * @attr {boolean} compact - Use compact spacing
 * @attr {boolean} flat - Remove border and shadow
 * @attr {boolean} rounded - Use rounded corners
 * @attr {boolean} hoverable - Add hover effect
 * @attr {boolean} selectable - Make card selectable/clickable
 * @attr {boolean} selected - Whether card is selected
 * 
 * @fires card-click - Dispatched when selectable card is clicked
 * 
 * @slot - Card body content
 * @slot header - Card header content (overrides title)
 * @slot footer - Card footer content
 * @slot actions - Card actions (top right)
 * 
 * @example
 * <pf-card title="My Card">
 *   <p>Card content goes here</p>
 * </pf-card>
 */
@customElement('pf-card')
export class PfCard extends LitElement {
  @property({ type: String }) title = '';
  @property({ type: Boolean }) compact = false;
  @property({ type: Boolean }) flat = false;
  @property({ type: Boolean }) rounded = false;
  @property({ type: Boolean }) hoverable = false;
  @property({ type: Boolean }) selectable = false;
  @property({ type: Boolean }) selected = false;

  static styles = css`
    :host {
      display: block;
    }
  `;

  private handleClick() {
    if (this.selectable) {
      this.selected = !this.selected;
      this.dispatchEvent(new CustomEvent('card-click', {
        detail: { selected: this.selected },
        bubbles: true,
        composed: true,
      }));
    }
  }

  render() {
    const cardClasses = [
      'pf-v6-c-card',
      this.compact && 'pf-m-compact',
      this.flat && 'pf-m-flat',
      this.rounded && 'pf-m-rounded',
      this.hoverable && 'pf-m-hoverable',
      this.selectable && 'pf-m-selectable',
      this.selected && 'pf-m-selected',
    ].filter(Boolean).join(' ');

    const hasHeader = this.title || this.querySelector('[slot="header"]') || this.querySelector('[slot="actions"]');
    const hasFooter = this.querySelector('[slot="footer"]');

    return html`
      <div class=${cardClasses} @click=${this.handleClick}>
        ${hasHeader ? html`
          <div class="pf-v6-c-card__header">
            <div class="pf-v6-c-card__header-main">
              <slot name="header">
                ${this.title ? html`<h3 class="pf-v6-c-card__title">${this.title}</h3>` : ''}
              </slot>
            </div>
            <div class="pf-v6-c-card__actions">
              <slot name="actions"></slot>
            </div>
          </div>
        ` : ''}
        
        <div class="pf-v6-c-card__body">
          <slot></slot>
        </div>

        ${hasFooter ? html`
          <div class="pf-v6-c-card__footer">
            <slot name="footer"></slot>
          </div>
        ` : ''}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-card': PfCard;
  }
}