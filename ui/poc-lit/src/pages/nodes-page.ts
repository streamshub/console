import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getNodes, NodesResponse } from '../api/nodes';
import { pageStyles } from '../styles/shared-styles';
import '../components/nodes-table';

@customElement('nodes-page')
export class NodesPage extends LitElement {
  static styles = [
    pageStyles,
    css`
      .summary {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: var(--pf-v6-global--spacer--md);
        margin-bottom: var(--pf-v6-global--spacer--md);
      }

      .summary-card {
        padding: var(--pf-v6-global--spacer--md);
        border: var(--pf-v6-global--BorderWidth--sm) solid var(--pf-v6-global--BorderColor--100);
        border-radius: var(--pf-v6-global--BorderRadius--sm);
        background: var(--pf-v6-global--BackgroundColor--100);
      }

      .summary-card h3 {
        margin: 0 0 var(--pf-v6-global--spacer--sm) 0;
        font-size: var(--pf-v6-global--FontSize--lg);
        color: var(--pf-v6-global--Color--200);
      }

      .summary-card .value {
        font-size: var(--pf-v6-global--FontSize--2xl);
        font-weight: var(--pf-v6-global--FontWeight--bold);
      }
    `
  ];

  @property({ type: Object }) location?: RouterLocation;
  
  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }
  @state() private sortColumn: string = 'id';
  @state() private sortDirection: 'asc' | 'desc' = 'asc';
  @state() private pageSize: number = 20;

  private _nodesTask = new Task(this, {
    task: async ([kafkaId, sortColumn, sortDirection, pageSize]) => {
      const response = await getNodes(kafkaId as string, {
        sort: sortColumn as string,
        sortDir: sortDirection as 'asc' | 'desc',
        pageSize: pageSize as number,
      });
      
      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to fetch nodes');
      }
      
      return response.data!;
    },
    args: () => [this.kafkaId, this.sortColumn, this.sortDirection, this.pageSize] as const,
  });


  private handleSort(e: CustomEvent) {
    this.sortColumn = e.detail.column;
    this.sortDirection = e.detail.direction;
  }

  private handlePageSizeChange(e: Event) {
    const select = e.target as HTMLSelectElement;
    this.pageSize = parseInt(select.value);
  }

  render() {
    return html`
      <div class="page-header">
        <h1 class="pf-v6-c-title pf-m-2xl">Kafka Nodes</h1>
      </div>

      <div class="page-content">
        ${this._nodesTask.render({
        pending: () => html`<div class="loading">Loading nodes...</div>`,
        complete: (data: NodesResponse) => html`
          <div class="summary">
            <div class="summary-card">
              <h3>Total Nodes</h3>
              <div class="value">${data.meta.page.total}</div>
            </div>
            <div class="summary-card">
              <h3>Brokers</h3>
              <div class="value">
                ${Object.values(data.meta.summary.statuses.brokers || {}).reduce((sum, count) => sum + count, 0)}
              </div>
            </div>
            <div class="summary-card">
              <h3>Controllers</h3>
              <div class="value">
                ${Object.values(data.meta.summary.statuses.controllers || {}).reduce((sum, count) => sum + count, 0)}
              </div>
            </div>
          </div>

          <div class="toolbar">
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

          <nodes-table
            .nodes=${data.data}
            .sortColumn=${this.sortColumn}
            .sortDirection=${this.sortDirection}
            .kafkaId=${this.kafkaId}
            @sort-change=${this.handleSort}
          ></nodes-table>
          
          <div class="pagination">
            <div class="pagination-info">
              Showing ${data.data.length} of ${data.meta.page.total} nodes
            </div>
            <div class="pagination-controls">
              <button ?disabled=${!data.links.prev}>Previous</button>
              <span>Page ${data.meta.page.pageNumber || 1}</span>
              <button ?disabled=${!data.links.next}>Next</button>
            </div>
          </div>
        `,
        error: (err: unknown) => html`
          <div class="error">
            Error loading nodes: ${err instanceof Error ? err.message : 'Unknown error'}
          </div>
        `,
      })}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'nodes-page': NodesPage;
  }
}

// Made with Bob
