import { LitElement, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getKafkaUsers, KafkaUsersResponse } from '../api/kafka-users';
import { pageStyles } from '../styles/shared-styles';
import '../components/kafka-users-table';

@customElement('kafka-users-page')
export class KafkaUsersPage extends LitElement {
  static styles = pageStyles;

  @property({ type: Object }) location?: RouterLocation;
  
  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }
  @state() private searchTerm: string = '';
  @state() private sortColumn: string = 'name';
  @state() private sortDirection: 'asc' | 'desc' = 'asc';
  @state() private pageSize: number = 20;

  private _usersTask = new Task(this, {
    task: async ([kafkaId, searchTerm, sortColumn, sortDirection, pageSize]) => {
      const response = await getKafkaUsers(kafkaId as string, {
        username: searchTerm as string || undefined,
        sort: sortColumn as string,
        sortDir: sortDirection as 'asc' | 'desc',
        pageSize: pageSize as number,
      });
      
      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to fetch Kafka users');
      }
      
      return response.data!;
    },
    args: () => [this.kafkaId, this.searchTerm, this.sortColumn, this.sortDirection, this.pageSize] as const,
  });


  private handleSearch(e: Event) {
    const input = e.target as HTMLInputElement;
    this.searchTerm = input.value;
  }

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
        <h1 class="pf-v5-c-title pf-m-2xl">Kafka Users</h1>
      </div>

      <div class="page-content">
        <div class="toolbar">
        <input
          type="search"
          class="search-input"
          placeholder="Search by username..."
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

      ${this._usersTask.render({
        pending: () => html`<div class="loading">Loading Kafka users...</div>`,
        complete: (data: KafkaUsersResponse) => html`
          <kafka-users-table
            .users=${data.data}
            .sortColumn=${this.sortColumn}
            .sortDirection=${this.sortDirection}
            .kafkaId=${this.kafkaId}
            @sort-change=${this.handleSort}
          ></kafka-users-table>
          
          <div class="pagination">
            <div class="pagination-info">
              Showing ${data.data.length} of ${data.meta.page.total || 0} users
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
            Error loading Kafka users: ${err instanceof Error ? err.message : 'Unknown error'}
          </div>
        `,
      })}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'kafka-users-page': KafkaUsersPage;
  }
}

// Made with Bob
