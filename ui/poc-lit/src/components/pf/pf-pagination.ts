import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

/**
 * PatternFly Pagination Component
 * A lightweight wrapper around PatternFly v6 pagination classes
 * 
 * @element pf-pagination
 * 
 * @attr {number} page - Current page (1-based)
 * @attr {number} perPage - Items per page
 * @attr {number} total - Total number of items
 * @attr {boolean} compact - Use compact variant
 * 
 * @fires page-change - Dispatched when page changes
 * @fires per-page-change - Dispatched when items per page changes
 * 
 * @example
 * <pf-pagination 
 *   page="1" 
 *   perPage="20" 
 *   total="100"
 *   @page-change=${handlePageChange}>
 * </pf-pagination>
 */
@customElement('pf-pagination')
export class PfPagination extends LitElement {
  @property({ type: Number }) page = 1;
  @property({ type: Number }) perPage = 20;
  @property({ type: Number }) total = 0;
  @property({ type: Boolean }) compact = false;

  static styles = css`
    :host {
      display: block;
    }
  `;

  private get totalPages(): number {
    return Math.ceil(this.total / this.perPage);
  }

  private get startItem(): number {
    return (this.page - 1) * this.perPage + 1;
  }

  private get endItem(): number {
    return Math.min(this.page * this.perPage, this.total);
  }

  private handleFirstPage() {
    if (this.page > 1) {
      this.page = 1;
      this.dispatchPageChange();
    }
  }

  private handlePreviousPage() {
    if (this.page > 1) {
      this.page--;
      this.dispatchPageChange();
    }
  }

  private handleNextPage() {
    if (this.page < this.totalPages) {
      this.page++;
      this.dispatchPageChange();
    }
  }

  private handleLastPage() {
    if (this.page < this.totalPages) {
      this.page = this.totalPages;
      this.dispatchPageChange();
    }
  }

  private dispatchPageChange() {
    this.dispatchEvent(new CustomEvent('page-change', {
      detail: { page: this.page },
      bubbles: true,
      composed: true,
    }));
  }

  render() {
    const classes = [
      'pf-v6-c-pagination',
      this.compact && 'pf-m-compact',
    ].filter(Boolean).join(' ');

    return html`
      <div class=${classes}>
        <div class="pf-v6-c-pagination__total-items">
          ${this.startItem} - ${this.endItem} of ${this.total}
        </div>
        <nav class="pf-v6-c-pagination__nav" aria-label="Pagination">
          <button 
            class="pf-v6-c-button pf-m-plain" 
            type="button"
            ?disabled=${this.page === 1}
            @click=${this.handleFirstPage}
            aria-label="Go to first page"
          >
            <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 256 512" aria-hidden="true" role="img">
              <path d="M224 480c0 17.7-14.3 32-32 32s-32-14.3-32-32V32c0-17.7 14.3-32 32-32s32 14.3 32 32v448zm-96-32c0 17.7-14.3 32-32 32s-32-14.3-32-32V32c0-17.7 14.3-32 32-32s32 14.3 32 32v416z"></path>
            </svg>
          </button>
          <button 
            class="pf-v6-c-button pf-m-plain" 
            type="button"
            ?disabled=${this.page === 1}
            @click=${this.handlePreviousPage}
            aria-label="Go to previous page"
          >
            <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 256 512" aria-hidden="true" role="img">
              <path d="M192 448c-8.188 0-16.38-3.125-22.62-9.375l-160-160c-12.5-12.5-12.5-32.75 0-45.25l160-160c12.5-12.5 32.75-12.5 45.25 0s12.5 32.75 0 45.25L77.25 256l137.4 137.4c12.5 12.5 12.5 32.75 0 45.25C208.4 444.9 200.2 448 192 448z"></path>
            </svg>
          </button>
          <div class="pf-v6-c-pagination__nav-page-select">
            Page ${this.page} of ${this.totalPages}
          </div>
          <button 
            class="pf-v6-c-button pf-m-plain" 
            type="button"
            ?disabled=${this.page === this.totalPages}
            @click=${this.handleNextPage}
            aria-label="Go to next page"
          >
            <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 256 512" aria-hidden="true" role="img">
              <path d="M64 448c-8.188 0-16.38-3.125-22.62-9.375c-12.5-12.5-12.5-32.75 0-45.25L178.8 256L41.38 118.6c-12.5-12.5-12.5-32.75 0-45.25s32.75-12.5 45.25 0l160 160c12.5 12.5 12.5 32.75 0 45.25l-160 160C80.38 444.9 72.19 448 64 448z"></path>
            </svg>
          </button>
          <button 
            class="pf-v6-c-button pf-m-plain" 
            type="button"
            ?disabled=${this.page === this.totalPages}
            @click=${this.handleLastPage}
            aria-label="Go to last page"
          >
            <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 256 512" aria-hidden="true" role="img">
              <path d="M32 32c0-17.7 14.3-32 32-32s32 14.3 32 32v448c0 17.7-14.3 32-32 32s-32-14.3-32-32V32zm96 416c0 17.7-14.3 32-32 32s-32-14.3-32-32V32c0-17.7 14.3-32 32-32s32 14.3 32 32v416z"></path>
            </svg>
          </button>
        </nav>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'pf-pagination': PfPagination;
  }
}