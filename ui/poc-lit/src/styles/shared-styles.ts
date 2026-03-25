import { css } from 'lit';

/**
 * Common table styles for all table components
 * Uses PatternFly v5 CSS variables
 */
export const tableStyles = css`
  table {
    width: 100%;
    border-collapse: collapse;
  }

  th, td {
    padding: var(--pf-v5-global--spacer--sm);
    text-align: left;
    border-bottom: var(--pf-v5-global--BorderWidth--sm) solid var(--pf-v5-global--BorderColor--100);
  }

  th {
    font-weight: var(--pf-v5-global--FontWeight--bold);
    background-color: var(--pf-v5-global--BackgroundColor--200);
    cursor: pointer;
    user-select: none;
  }

  th:hover {
    background-color: var(--pf-v5-global--BackgroundColor--dark-400);
  }

  th.sortable::after {
    content: ' ⇅';
    opacity: 0.3;
  }

  th.sorted-asc::after {
    content: ' ↑';
    opacity: 1;
  }

  th.sorted-desc::after {
    content: ' ↓';
    opacity: 1;
  }

  tr:hover td {
    background-color: var(--pf-v5-global--BackgroundColor--200);
  }

  a {
    color: var(--pf-v5-global--link--Color);
    text-decoration: none;
  }

  a:hover {
    color: var(--pf-v5-global--link--Color--hover);
    text-decoration: underline;
  }

  .sort-indicator {
    margin-left: var(--pf-v5-global--spacer--sm);
    font-size: var(--pf-v5-global--FontSize--sm);
  }
`;

/**
 * Common page layout styles
 * Uses PatternFly v5 CSS variables
 */
export const pageStyles = css`
  :host {
    display: block;
  }

  .page-header {
    padding: var(--pf-v5-global--spacer--lg);
    background-color: var(--pf-v5-global--BackgroundColor--100);
    border-bottom: var(--pf-v5-global--BorderWidth--sm) solid var(--pf-v5-global--BorderColor--100);
  }

  .page-content {
    padding: var(--pf-v5-global--spacer--lg);
  }

  h1 {
    font-size: var(--pf-v5-global--FontSize--2xl);
    margin-bottom: var(--pf-v5-global--spacer--md);
    font-weight: var(--pf-v5-global--FontWeight--normal);
  }

  .toolbar {
    display: flex;
    gap: var(--pf-v5-global--spacer--md);
    margin-bottom: var(--pf-v5-global--spacer--md);
    align-items: center;
  }

  .search-input {
    flex: 1;
    max-width: 400px;
  }

  .filter-group {
    display: flex;
    gap: var(--pf-v5-global--spacer--sm);
    align-items: center;
  }

  .pagination {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: var(--pf-v5-global--spacer--md);
    padding: var(--pf-v5-global--spacer--md);
    border-top: var(--pf-v5-global--BorderWidth--sm) solid var(--pf-v5-global--BorderColor--100);
  }

  .pagination-info {
    color: var(--pf-v5-global--Color--200);
  }

  .pagination-controls {
    display: flex;
    gap: var(--pf-v5-global--spacer--sm);
  }

  .loading, .error {
    padding: var(--pf-v5-global--spacer--lg);
    text-align: center;
  }

  .error-state {
    padding: var(--pf-v5-global--spacer--xl);
    text-align: center;
    color: var(--pf-v5-global--danger-color--100);
  }
`;

/**
 * Common badge/label styles
 * Uses PatternFly v5 CSS variables
 */
export const badgeStyles = css`
  .badge {
    display: inline-block;
    padding: var(--pf-v5-global--spacer--xs) var(--pf-v5-global--spacer--sm);
    background-color: var(--pf-v5-global--BackgroundColor--200);
    border-radius: var(--pf-v5-global--BorderRadius--sm);
    font-size: var(--pf-v5-global--FontSize--sm);
    margin-right: var(--pf-v5-global--spacer--xs);
  }

  .badge-primary {
    background-color: var(--pf-v5-global--primary-color--100);
    color: var(--pf-v5-global--Color--light-100);
  }

  .badge-info {
    background-color: var(--pf-v5-global--info-color--100);
    color: var(--pf-v5-global--Color--light-100);
  }

  .badge-success {
    background-color: var(--pf-v5-global--success-color--100);
    color: var(--pf-v5-global--Color--light-100);
  }

  .badge-warning {
    background-color: var(--pf-v5-global--warning-color--100);
    color: var(--pf-v5-global--Color--dark-100);
  }

  .badge-danger {
    background-color: var(--pf-v5-global--danger-color--100);
    color: var(--pf-v5-global--Color--light-100);
  }

  .label-group {
    display: flex;
    flex-wrap: wrap;
    gap: var(--pf-v5-global--spacer--xs);
  }

  .label {
    display: inline-block;
    padding: var(--pf-v5-global--spacer--xs) var(--pf-v5-global--spacer--sm);
    background-color: var(--pf-v5-global--BackgroundColor--200);
    border-radius: var(--pf-v5-global--BorderRadius--sm);
    font-size: var(--pf-v5-global--FontSize--sm);
  }
`;

/**
 * Common status indicator styles
 * Uses PatternFly v5 CSS variables
 */
export const statusStyles = css`
  .status-icon {
    margin-right: var(--pf-v5-global--spacer--sm);
  }

  .status-success {
    color: var(--pf-v5-global--success-color--100);
  }

  .status-info {
    color: var(--pf-v5-global--info-color--100);
  }

  .status-warning {
    color: var(--pf-v5-global--warning-color--100);
  }

  .status-danger {
    color: var(--pf-v5-global--danger-color--100);
  }

  .status-running { color: var(--pf-v5-global--success-color--100); }
  .status-stable { color: var(--pf-v5-global--success-color--100); }
  .status-empty { color: var(--pf-v5-global--info-color--100); }
  .status-unknown { color: var(--pf-v5-global--warning-color--100); }
  .status-dead { color: var(--pf-v5-global--danger-color--100); }
  .status-error { color: var(--pf-v5-global--danger-color--100); }
`;

/**
 * Utility styles
 */
export const utilityStyles = css`
  .truncate {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

// Made with Bob
