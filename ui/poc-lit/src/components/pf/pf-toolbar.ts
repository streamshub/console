import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * PatternFly Toolbar Component
 * A lightweight wrapper around PatternFly v6 toolbar classes
 * 
 * @element pf-toolbar
 * 
 * @attr {boolean} sticky - Make toolbar sticky at top
 * 
 * @slot - Toolbar content items
 * @slot filter - Filter section
 * @slot actions - Action buttons section
 * @slot pagination - Pagination controls
 * 
 * @example
 * <pf-toolbar>
 *   <div slot="filter">
 *     <pf-text-input placeholder="Search..."></pf-text-input>
 *   </div>
 *   <div slot="actions">
 *     <pf-button>Create</pf-button>
 *   </div>
 * </pf-toolbar>
 */
@customElement('pf-toolbar')
export class PfToolbar extends LitElement {
  @property({ type: Boolean }) sticky = false;

  static styles = css`
    :host {
      display: block;
    }

    :host([sticky]) .pf-v6-c-toolbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background-color: var(--pf-v6-global--BackgroundColor--100);
    }
  `;

  render() {
    return html`
      <div class="pf-v6-c-toolbar">
        <div class="pf-v6-c-toolbar__content">
          <div class="pf-v6-c-toolbar__content-section">
            <div class="pf-v6-c-toolbar__group pf-m-filter-group">
              <div class="pf-v6-c-toolbar__item pf-m-search-filter">
                <slot name="filter"></slot>
              </div>
            </div>
            <div class="pf-v6-c-toolbar__group">
              <div class="pf-v6-c-toolbar__item">
                <slot name="actions"></slot>
              </div>
            </div>
            <div class="pf-v6-c-toolbar__item pf-m-pagination">
              <slot name="pagination"></slot>
            </div>
          </div>
          <slot></slot>
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-toolbar': PfToolbar;
  }
}