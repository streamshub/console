/**
 * StatusLabel Component
 *
 * A reusable component for rendering status labels with icons.
 * Provides consistent status display across the application.
 * Supports optional tooltips for additional context.
 */

import { Icon, Tooltip } from '@patternfly/react-core';

export type StatusIconStatus = 'success' | 'danger' | 'warning' | 'info' | 'custom';

export interface StatusConfig {
  icon: React.ComponentType;
  iconStatus?: StatusIconStatus;
  label: string;
  tooltip?: string;
}

export interface StatusLabelProps<T extends string> {
  status: T;
  config: Record<T, StatusConfig>;
}

/**
 * Renders a status label with an icon and optional tooltip
 *
 * @example
 * ```tsx
 * const config: Record<TopicStatus, StatusConfig<TopicStatus>> = {
 *   FullyReplicated: {
 *     icon: CheckCircleIcon,
 *     iconStatus: 'success',
 *     label: 'Fully replicated',
 *     tooltip: 'All replicas are in sync',
 *   },
 * };
 *
 * <StatusLabel status="FullyReplicated" config={config} />
 * ```
 */
export function StatusLabel<T extends string>({
  status,
  config,
}: StatusLabelProps<T>) {
  const statusConfig = config[status];
  
  if (!statusConfig) {
    return <>{status}</>;
  }

  const { icon: IconComponent, iconStatus, label, tooltip } = statusConfig;

  const content = (
    <>
      <Icon status={iconStatus}>
        <IconComponent />
      </Icon>
      &nbsp;{label}
    </>
  );

  if (tooltip) {
    return (
      <Tooltip content={tooltip}>
        <span>{content}</span>
      </Tooltip>
    );
  }

  return content;
}