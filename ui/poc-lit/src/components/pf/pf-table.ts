import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
}

/**
 * PatternFly Table Component
 * A lightweight wrapper around PatternFly v6 table classes
 * 
 * @element pf-table
 * 
 * @attr {TableColumn[]} columns - Table column definitions
 * @attr {boolean} compact - Use compact spacing
 * @attr {boolean} borders - Show borders
 * @attr {boolean} striped - Use striped rows
 * @attr {string} caption - Table caption for accessibility
 * 
 * @fires sort - Dispatched when sortable column header is clicked
 * 
 * @slot - Table body rows (tr elements)
 * 
 * @example
 * <pf-table .columns=${columns}>
 *   <tr>
 *     <td>Cell 1</td>
 *     <td>Cell 2</td>
 *   </tr>
 * </pf-table>
 */
@customElement('pf-table')
export class PfTable extends LitElement {
  @property({ type: Array }) columns: TableColumn[] = [];
  @property({ type: Boolean }) compact = false;
  @property({ type: Boolean }) borders = true;
  @property({ type: Boolean }) striped = false;
  @property({ type: String }) caption = '';

  // Disable shadow DOM to allow proper table structure
  protected createRenderRoot() {
    return this;
  }

  private handleSort(column: TableColumn) {
    if (column.sortable) {
      this.dispatchEvent(new CustomEvent('sort', {
        detail: { column: column.key },
        bubbles: true,
        composed: true,
      }));
    }
  }

  render() {
    const classes = [
      'pf-v6-c-table',
      this.compact && 'pf-m-compact',
      !this.borders && 'pf-m-no-border-rows',
      this.striped && 'pf-m-striped',
    ].filter(Boolean).join(' ');

    return html`
      <table class=${classes} role="grid">
        ${this.caption ? html`<caption>${this.caption}</caption>` : ''}
        <thead class="pf-v6-c-table__thead">
          <tr class="pf-v6-c-table__tr" role="row">
            ${this.columns.map(column => html`
              <th 
                class="pf-v6-c-table__th ${column.sortable ? 'pf-v6-c-table__sort' : ''}"
                role="columnheader" 
                scope="col"
                @click=${() => this.handleSort(column)}
              >
                ${column.sortable ? html`
                  <button class="pf-v6-c-table__button">
                    <div class="pf-v6-c-table__button-content">
                      <span class="pf-v6-c-table__text">${column.label}</span>
                      <span class="pf-v6-c-table__sort-indicator">
                        <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 256 512" aria-hidden="true" role="img">
                          <path d="M119.5 326.9L3.5 209.1c-4.7-4.7-4.7-12.3 0-17l7.1-7.1c4.7-4.7 12.3-4.7 17 0L128 287.3l100.4-102.2c4.7-4.7 12.3-4.7 17 0l7.1 7.1c4.7 4.7 4.7 12.3 0 17L136.5 327c-4.7 4.6-12.3 4.6-17-.1z"></path>
                        </svg>
                      </span>
                    </div>
                  </button>
                ` : html`${column.label}`}
              </th>
            `)}
          </tr>
        </thead>
        <tbody class="pf-v6-c-table__tbody" role="rowgroup">
          <slot></slot>
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-table': PfTable;
  }
}