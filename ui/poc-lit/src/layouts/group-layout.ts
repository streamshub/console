import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getGroup } from '../api/groups';
import type { Group } from '../api/groups';
import { pageStyles } from '../styles/shared-styles';

@customElement('group-layout')
export class GroupLayout extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  @state() private groupName?: string;

  static styles = [
    pageStyles,
    css`
      :host {
        display: block;
      }

      .group-tabs {
        border-bottom: 1px solid var(--pf-v5-global--BorderColor--100);
        margin-bottom: var(--pf-v5-global--spacer--lg);
      }

      .group-tabs ul {
        display: flex;
        list-style: none;
        margin: 0;
        padding: 0;
        gap: var(--pf-v5-global--spacer--md);
      }

      .group-tabs li {
        margin: 0;
      }

      .group-tabs a {
        display: block;
        padding: var(--pf-v5-global--spacer--md) var(--pf-v5-global--spacer--lg);
        color: var(--pf-v5-global--Color--200);
        text-decoration: none;
        border-bottom: 3px solid transparent;
        transition: all 0.2s;
      }

      .group-tabs a:hover {
        color: var(--pf-v5-global--Color--100);
        border-bottom-color: var(--pf-v5-global--BorderColor--100);
      }

      .group-tabs a.active {
        color: var(--pf-v5-global--primary-color--100);
        border-bottom-color: var(--pf-v5-global--primary-color--100);
      }

      .group-content {
        padding: var(--pf-v5-global--spacer--md);
      }
    `
  ];

  private groupTask = new Task(this, {
    task: async ([kafkaId, groupId]) => {
      const response = await getGroup(
        kafkaId as string,
        groupId as string,
        { fields: ['groupId'] }
      );
      
      if (response.errors) {
        return null;
      }
      
      return response.data!.data;
    },
    args: () => [
      this.location?.params?.kafkaId,
      this.location?.params?.groupId
    ],
    onComplete: (group: Group | null) => {
      if (group) {
        this.groupName = group.attributes.groupId;
      }
    }
  });

  private isActiveRoute(path: string): boolean {
    return this.location?.pathname?.includes(path) || false;
  }

  render() {
    const kafkaId = this.location?.params?.kafkaId as string;
    const groupId = this.location?.params?.groupId as string;

    if (!kafkaId || !groupId) {
      return html`<div>Loading...</div>`;
    }

    const basePath = `/kafka/${kafkaId}/groups/${groupId}`;
    const displayName = this.groupName || groupId;

    return html`
      <div class="group-layout">
        <div class="page-header">
          <h1>Consumer Group: ${displayName}</h1>
        </div>

        <nav class="group-tabs">
          <ul>
            <li>
              <a 
                href="${basePath}/members" 
                class="${this.isActiveRoute('/members') ? 'active' : ''}"
              >
                Members
              </a>
            </li>
            <li>
              <a 
                href="${basePath}/configuration" 
                class="${this.isActiveRoute('/configuration') ? 'active' : ''}"
              >
                Configuration
              </a>
            </li>
            <li>
              <a 
                href="${basePath}/reset-offset" 
                class="${this.isActiveRoute('/reset-offset') ? 'active' : ''}"
              >
                Reset Offset
              </a>
            </li>
          </ul>
        </nav>

        <div class="group-content">
          <slot></slot>
        </div>
      </div>
    `;
  }
}

// Made with Bob
