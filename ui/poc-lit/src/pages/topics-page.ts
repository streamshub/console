import { LitElement, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getTopics, TopicsResponse } from '../api/topics';
import { pageStyles } from '../styles/shared-styles';
import '../components/topics-table';

@customElement('topics-page')
export class TopicsPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  
  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }
  @state() private page = 1;
  @state() private perPage = 20;
  @state() private sortBy = 'name';
  @state() private sortDirection: 'asc' | 'desc' = 'asc';
  @state() private searchTerm = '';

  private topicsTask = new Task(this, {
    task: async ([kafkaId, page, perPage, sortBy, sortDirection, searchTerm]) => {
      const response = await getTopics(kafkaId as string, {
        page: page as number,
        perPage: perPage as number,
        sort: sortBy as string,
        sortDir: sortDirection as 'asc' | 'desc',
        name: searchTerm as string || undefined,
        fields: [ 'name' , 'status' , 'numPartitions', 'totalLeaderLogBytes' ],
      });

      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to load topics');
      }

      return response.data!;
    },
    args: () => [this.kafkaId, this.page, this.perPage, this.sortBy, this.sortDirection, this.searchTerm],
  });

  static styles = pageStyles;

  private handleSearch(e: Event) {
    const input = e.target as HTMLInputElement;
    this.searchTerm = input.value;
    this.page = 1; // Reset to first page on search
  }

  private handleSortChange(e: CustomEvent) {
    this.sortBy = e.detail.sortBy;
    this.sortDirection = e.detail.sortDirection;
  }

  private handlePreviousPage() {
    if (this.page > 1) {
      this.page--;
    }
  }

  private handleNextPage(hasMore: boolean) {
    if (hasMore) {
      this.page++;
    }
  }

  render() {
    return html`
      <div class="page-header">
        <h1 class="pf-v6-c-title pf-m-2xl">Topics</h1>
      </div>

      <div class="page-content">
        <div class="toolbar">
          <div class="search-input">
            <div class="pf-v6-c-input-group">
              <input
                class="pf-v6-c-form-control"
                type="text"
                placeholder="Search topics..."
                @input=${this.handleSearch}
                .value=${this.searchTerm}
              />
            </div>
          </div>
        </div>

        ${this.topicsTask.render({
          pending: () => html`
            <topics-table loading></topics-table>
          `,
          complete: (data: TopicsResponse) => html`
            <topics-table
              .kafkaId="${this.kafkaId}"
              .topics=${data.data}
              .sortBy=${this.sortBy}
              .sortDirection=${this.sortDirection}
              @sort-change=${this.handleSortChange}
            ></topics-table>

            <div class="pagination">
              <div class="pagination-info">
                Showing ${((this.page - 1) * this.perPage) + 1} - ${Math.min(this.page * this.perPage, data.meta.page.total)} of ${data.meta.page.total} topics
              </div>
              <div class="pagination-controls">
                <button
                  class="pf-v6-c-button pf-m-secondary"
                  ?disabled=${this.page <= 1}
                  @click=${this.handlePreviousPage}
                >
                  Previous
                </button>
                <button
                  class="pf-v6-c-button pf-m-secondary"
                  ?disabled=${this.page * this.perPage >= data.meta.page.total}
                  @click=${() => this.handleNextPage(this.page * this.perPage < data.meta.page.total)}
                >
                  Next
                </button>
              </div>
            </div>
          `,
          error: (error: unknown) => {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            return html`
              <div class="error-state">
                <div class="pf-v6-c-empty-state">
                  <div class="pf-v6-c-empty-state__content">
                    <div class="pf-v6-c-empty-state__icon">
                      <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
                    </div>
                    <h2 class="pf-v6-c-title pf-m-lg">Error loading topics</h2>
                    <div class="pf-v6-c-empty-state__body">
                      ${errorMessage}
                    </div>
                    <button
                      class="pf-v6-c-button pf-m-primary"
                      @click=${() => this.topicsTask.run()}
                    >
                      Retry
                    </button>
                  </div>
                </div>
              </div>
            `;
          },
        })}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'topics-page': TopicsPage;
  }
}

// Made with Bob
