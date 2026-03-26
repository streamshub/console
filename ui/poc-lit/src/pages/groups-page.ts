import { LitElement, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getGroups, Group, GroupsResponse } from '../api/groups';
import { pageStyles } from '../styles/shared-styles';
import '../components/groups-table';

@customElement('groups-page')
export class GroupsPage extends LitElement {
  static styles = pageStyles;

  @property({ type: Object }) location?: RouterLocation;
  
  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }
  @state() private searchTerm: string = '';
  @state() private sortColumn: string = 'groupId';
  @state() private sortDirection: 'asc' | 'desc' = 'asc';
  @state() private pageSize: number = 20;
  @state() private currentPage: number = 1;
  @state() private typeFilter: string[] = [];
  @state() private stateFilter: string[] = [];

  private _groupsTask = new Task(this, {
    task: async ([kafkaId, searchTerm, sortColumn, sortDirection, pageSize, typeFilter, stateFilter]) => {
      const response = await getGroups(kafkaId as string, {
        id: searchTerm as string || undefined,
        fields: ['groupId', 'type', 'protocol', 'state', 'members', 'offsets' ],
        sort: sortColumn as string,
        sortDir: sortDirection as 'asc' | 'desc',
        pageSize: pageSize as number,
        type: (typeFilter as string[]).length > 0 ? typeFilter as string[] : undefined,
        consumerGroupState: (stateFilter as string[]).length > 0 ? stateFilter as string[] : undefined,
      });
      
      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to fetch groups');
      }
      
      return response.data!;
    },
    args: () => [this.kafkaId, this.searchTerm, this.sortColumn, this.sortDirection, this.pageSize, this.typeFilter, this.stateFilter] as const,
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
    this.currentPage = 1;
  }

  render() {
    return html`
      <div class="page-header">
        <h1 class="pf-v6-c-title pf-m-2xl">Groups</h1>
      </div>

      <div class="page-content">
        <div class="toolbar">
        <input
          type="search"
          class="search-input"
          placeholder="Search by group ID..."
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

      ${this._groupsTask.render({
        pending: () => html`<div class="loading">Loading groups...</div>`,
        complete: (data: GroupsResponse) => html`
          <groups-table
            .groups=${data.data}
            .sortColumn=${this.sortColumn}
            .sortDirection=${this.sortDirection}
            .kafkaId=${this.kafkaId}
            @sort-change=${this.handleSort}
          ></groups-table>
          
          <div class="pagination">
            <div class="pagination-info">
              Showing ${data.data.length} of ${data.meta.page.total || 0} groups
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
            Error loading groups: ${err instanceof Error ? err.message : 'Unknown error'}
          </div>
        `,
      })}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'groups-page': GroupsPage;
  }
}

// Made with Bob
