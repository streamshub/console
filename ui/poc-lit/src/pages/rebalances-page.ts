import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getRebalances, RebalancesResponse, RebalanceStatus, RebalanceMode } from '../api/rebalances';
import { pageStyles } from '../styles/shared-styles';
import '../components/rebalances-table';

@customElement('rebalances-page')
export class RebalancesPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  @state() private searchTerm = '';
  @state() private pageSize = 20;
  @state() private currentPage = 1;
  @state() private sortColumn = 'name';
  @state() private sortDirection: 'asc' | 'desc' = 'asc';
  @state() private statusFilter: RebalanceStatus[] = [];
  @state() private modeFilter: RebalanceMode[] = [];

  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }

  static styles = [
    pageStyles,
    css`
      .filters {
        display: flex;
        gap: var(--pf-v5-global--spacer--md);
        margin-bottom: var(--pf-v5-global--spacer--md);
        flex-wrap: wrap;
      }

      .filter-group {
        display: flex;
        flex-direction: column;
        gap: var(--pf-v5-global--spacer--xs);
      }

      .filter-group label {
        font-size: var(--pf-v5-global--FontSize--sm);
        font-weight: var(--pf-v5-global--FontWeight--bold);
      }

      .checkbox-group {
        display: flex;
        flex-direction: column;
        gap: var(--pf-v5-global--spacer--xs);
      }

      .checkbox-label {
        display: flex;
        align-items: center;
        gap: var(--pf-v5-global--spacer--sm);
      }
    `
  ];

  private _rebalancesTask = new Task(this, {
    task: async ([kafkaId, searchTerm, pageSize, currentPage, sortColumn, sortDirection, statusFilter, modeFilter]) => {
      if (!kafkaId) {
        throw new Error('No Kafka cluster ID provided');
      }

      const response = await getRebalances(kafkaId as string, {
        name: searchTerm as string || undefined,
        perPage: pageSize as number,
        page: currentPage as number,
        sort: sortColumn as string,
        sortDir: sortDirection as 'asc' | 'desc',
        status: (statusFilter as RebalanceStatus[]).length > 0 ? statusFilter as RebalanceStatus[] : undefined,
        mode: (modeFilter as RebalanceMode[]).length > 0 ? modeFilter as RebalanceMode[] : undefined,
      });

      return response;
    },
    args: () => [
      this.kafkaId,
      this.searchTerm,
      this.pageSize,
      this.currentPage,
      this.sortColumn,
      this.sortDirection,
      this.statusFilter,
      this.modeFilter,
    ] as const,
  });

  private handleSearch(e: Event) {
    const input = e.target as HTMLInputElement;
    this.searchTerm = input.value;
    this.currentPage = 1;
  }

  private handleSort(e: CustomEvent) {
    this.sortColumn = e.detail.column;
    this.sortDirection = e.detail.direction;
  }

  private handlePageSizeChange(e: Event) {
    const select = e.target as HTMLSelectElement;
    this.pageSize = parseInt(select.value);
    this.currentPage = 1;
  }

  private handleStatusFilterChange(e: Event, status: RebalanceStatus) {
    const checkbox = e.target as HTMLInputElement;
    if (checkbox.checked) {
      this.statusFilter = [...this.statusFilter, status];
    } else {
      this.statusFilter = this.statusFilter.filter(s => s !== status);
    }
    this.currentPage = 1;
  }

  private handleModeFilterChange(e: Event, mode: RebalanceMode) {
    const checkbox = e.target as HTMLInputElement;
    if (checkbox.checked) {
      this.modeFilter = [...this.modeFilter, mode];
    } else {
      this.modeFilter = this.modeFilter.filter(m => m !== mode);
    }
    this.currentPage = 1;
  }

  render() {
    return html`
      <div class="page-header">
        <h1 class="pf-v5-c-title pf-m-2xl">Rebalance</h1>
      </div>

      <div class="page-content">
        <div class="toolbar">
          <input
            type="search"
            class="search-input"
            placeholder="Search by name..."
            @input=${this.handleSearch}
            .value=${this.searchTerm}
          />
          
          <div class="filter-group">
            <label>Per page:</label>
            <select @change=${this.handlePageSizeChange}>
              <option value="10" ?selected=${this.pageSize === 10}>10</option>
              <option value="20" ?selected=${this.pageSize === 20}>20</option>
              <option value="50" ?selected=${this.pageSize === 50}>50</option>
              <option value="100" ?selected=${this.pageSize === 100}>100</option>
            </select>
          </div>
        </div>

        <div class="filters">
          <div class="filter-group">
            <label>Status:</label>
            <div class="checkbox-group">
              ${(['New', 'PendingProposal', 'ProposalReady', 'Rebalancing', 'Stopped', 'NotReady', 'Ready', 'ReconciliationPaused'] as RebalanceStatus[]).map(status => html`
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    .checked=${this.statusFilter.includes(status)}
                    @change=${(e: Event) => this.handleStatusFilterChange(e, status)}
                  />
                  ${status}
                </label>
              `)}
            </div>
          </div>

          <div class="filter-group">
            <label>Mode:</label>
            <div class="checkbox-group">
              ${(['full', 'add-brokers', 'remove-brokers'] as RebalanceMode[]).map(mode => html`
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    .checked=${this.modeFilter.includes(mode)}
                    @change=${(e: Event) => this.handleModeFilterChange(e, mode)}
                  />
                  ${mode === 'full' ? 'Full' : mode === 'add-brokers' ? 'Add Brokers' : 'Remove Brokers'}
                </label>
              `)}
            </div>
          </div>
        </div>

        ${this._rebalancesTask.render({
          pending: () => html`<div class="loading">Loading rebalances...</div>`,
          complete: (data: RebalancesResponse) => html`
            <rebalances-table
              .rebalances=${data.data}
              .sortColumn=${this.sortColumn}
              .sortDirection=${this.sortDirection}
              .kafkaId=${this.kafkaId}
              @sort-change=${this.handleSort}
            ></rebalances-table>
            
            <div class="pagination">
              <div class="pagination-info">
                Showing ${data.data.length} of ${data.meta.page.total || 0} rebalances
              </div>
              <div class="pagination-controls">
                <button
                  ?disabled=${!data.links.prev}
                  @click=${() => this.currentPage--}
                >
                  Previous
                </button>
                <span>Page ${data.meta.page.pageNumber || 1}</span>
                <button
                  ?disabled=${!data.links.next}
                  @click=${() => this.currentPage++}
                >
                  Next
                </button>
              </div>
            </div>
          `,
          error: (err: unknown) => html`
            <div class="error">
              Error loading rebalances: ${err instanceof Error ? err.message : 'Unknown error'}
            </div>
          `,
        })}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'rebalances-page': RebalancesPage;
  }
}

// Made with Bob