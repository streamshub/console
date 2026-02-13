/**
 * Node Status Label Components
 * Displays broker and controller status with icons and popovers
 */

import { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Icon,
  Popover,
  Flex,
  FlexItem,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  InProgressIcon,
  PendingIcon,
} from '@patternfly/react-icons';
import type { BrokerStatus, ControllerStatus, NodeRoles, Statuses } from '@/api/types';

// Icon component for new process (recovery status)
const NewProcessIcon = () => (
  <svg
    fill="currentColor"
    height="1em"
    width="1em"
    viewBox="0 0 512 512"
    aria-hidden="true"
    role="img"
    style={{ verticalAlign: '-0.125em' }}
  >
    <path d="M464 32H48C21.5 32 0 53.5 0 80v352c0 26.5 21.5 48 48 48h416c26.5 0 48-21.5 48-48V80c0-26.5-21.5-48-48-48zM128 384H64v-64h64v64zm0-128H64v-64h64v64zm0-128H64V64h64v64zm160 256h-64v-64h64v64zm0-128h-64v-64h64v64zm0-128h-64V64h64v64zm160 256h-64v-64h64v64zm0-128h-64v-64h64v64zm0-128h-64V64h64v64z" />
  </svg>
);

/**
 * Role labels with counts
 */
export const useRoleLabels = (
  statuses?: Statuses
): Record<NodeRoles, { label: ReactNode; labelWithCount: ReactNode }> => {
  const { t } = useTranslation();

  const brokerCount = statuses?.brokers
    ? Object.values(statuses.brokers).reduce((total, count) => total + count, 0)
    : 0;

  const controllerCount = statuses?.controllers
    ? Object.values(statuses.controllers).reduce((total, count) => total + count, 0)
    : 0;

  return {
    broker: {
      label: <>{t('nodes.nodeRoles.broker')}</>,
      labelWithCount: (
        <Flex>
          <FlexItem>{t('nodes.nodeRoles.broker')}</FlexItem>
          <FlexItem
            align={{ default: 'alignRight' }}
            style={{ color: 'var(--pf-t--global--text--color--subtle)' }}
          >
            {brokerCount}
          </FlexItem>
        </Flex>
      ),
    },
    controller: {
      label: <>{t('nodes.nodeRoles.controller')}</>,
      labelWithCount: (
        <Flex>
          <FlexItem>{t('nodes.nodeRoles.controller')}</FlexItem>
          <FlexItem
            align={{ default: 'alignRight' }}
            style={{ color: 'var(--pf-t--global--text--color--subtle)' }}
          >
            {controllerCount}
          </FlexItem>
        </Flex>
      ),
    },
  };
};

/**
 * Broker status labels with icons and popovers
 */
export const useBrokerStatusLabels = (): Record<BrokerStatus, ReactNode> => {
  const { t } = useTranslation();

  return {
    Running: (
      <Popover
        aria-label={t('nodes.brokerStatus.running.label')}
        headerContent={<div>{t('nodes.brokerStatus.running.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.running.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="success">
            <CheckCircleIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.running.label')}
        </span>
      </Popover>
    ),
    Starting: (
      <Popover
        aria-label={t('nodes.brokerStatus.starting.label')}
        headerContent={<div>{t('nodes.brokerStatus.starting.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.starting.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon>
            <InProgressIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.starting.label')}
        </span>
      </Popover>
    ),
    ShuttingDown: (
      <Popover
        aria-label={t('nodes.brokerStatus.shuttingDown.label')}
        headerContent={<div>{t('nodes.brokerStatus.shuttingDown.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.shuttingDown.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon>
            <PendingIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.shuttingDown.label')}
        </span>
      </Popover>
    ),
    PendingControlledShutdown: (
      <Popover
        aria-label={t('nodes.brokerStatus.pendingControlledShutdown.label')}
        headerContent={<div>{t('nodes.brokerStatus.pendingControlledShutdown.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.pendingControlledShutdown.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="warning">
            <ExclamationTriangleIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.pendingControlledShutdown.label')}
        </span>
      </Popover>
    ),
    Recovery: (
      <Popover
        aria-label={t('nodes.brokerStatus.recovery.label')}
        headerContent={<div>{t('nodes.brokerStatus.recovery.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.recovery.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon>
            <NewProcessIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.recovery.label')}
        </span>
      </Popover>
    ),
    NotRunning: (
      <Popover
        aria-label={t('nodes.brokerStatus.notRunning.label')}
        headerContent={<div>{t('nodes.brokerStatus.notRunning.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.notRunning.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="danger">
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.notRunning.label')}
        </span>
      </Popover>
    ),
    Unknown: (
      <Popover
        aria-label={t('nodes.brokerStatus.unknown.label')}
        headerContent={<div>{t('nodes.brokerStatus.unknown.label')}</div>}
        bodyContent={<div>{t('nodes.brokerStatus.unknown.popoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="danger">
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t('nodes.brokerStatus.unknown.label')}
        </span>
      </Popover>
    ),
  };
};

/**
 * Controller status labels with icons and popovers
 */
export const useControllerStatusLabels = (): Record<ControllerStatus, ReactNode> => {
  const { t } = useTranslation();

  return {
    QuorumLeader: (
      <Popover
        aria-label={t('nodes.controllerStatus.quorumLeader')}
        headerContent={<div>{t('nodes.controllerStatus.quorumLeader')}</div>}
        bodyContent={<div>{t('nodes.controllerStatus.quorumLeaderPopoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="success">
            <CheckCircleIcon />
          </Icon>
          &nbsp;{t('nodes.controllerStatus.quorumLeader')}
        </span>
      </Popover>
    ),
    QuorumFollower: (
      <Popover
        aria-label={t('nodes.controllerStatus.quorumFollower')}
        headerContent={<div>{t('nodes.controllerStatus.quorumFollower')}</div>}
        bodyContent={<div>{t('nodes.controllerStatus.quorumFollowerPopoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="success">
            <CheckCircleIcon />
          </Icon>
          &nbsp;{t('nodes.controllerStatus.quorumFollower')}
        </span>
      </Popover>
    ),
    QuorumFollowerLagged: (
      <Popover
        aria-label={t('nodes.controllerStatus.quorumFollowerLagged')}
        headerContent={<div>{t('nodes.controllerStatus.quorumFollowerLagged')}</div>}
        bodyContent={<div>{t('nodes.controllerStatus.quorumFollowerLaggedPopoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="warning">
            <ExclamationTriangleIcon />
          </Icon>
          &nbsp;{t('nodes.controllerStatus.quorumFollowerLagged')}
        </span>
      </Popover>
    ),
    Unknown: (
      <Popover
        aria-label={t('nodes.controllerStatus.unknown')}
        headerContent={<div>{t('nodes.controllerStatus.unknown')}</div>}
        bodyContent={<div>{t('nodes.controllerStatus.unknownPopoverText')}</div>}
      >
        <span className="pf-v6-u-active-color-100">
          <Icon status="danger">
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t('nodes.controllerStatus.unknown')}
        </span>
      </Popover>
    ),
  };
};

/**
 * Generate status labels with counts for filters
 */
const generateStatusLabelsWithCount = <T extends string>(
  labels: Record<T, ReactNode>,
  statuses: Record<T, number> = {} as Record<T, number>
): Record<T, ReactNode> => {
  return Object.entries(labels).reduce((acc, [key, label]) => {
    const typedKey = key as T;
    const count = statuses[typedKey] ?? 0;

    acc[typedKey] = (
      <Flex>
        <FlexItem>{label as ReactNode}</FlexItem>
        <FlexItem
          align={{ default: 'alignRight' }}
          style={{ color: 'var(--pf-t--global--text--color--subtle)' }}
        >
          {count}
        </FlexItem>
      </Flex>
    );
    return acc;
  }, {} as Record<T, ReactNode>);
};

/**
 * Get broker status labels with counts for filter options
 */
export const useBrokerStatusLabelsWithCount = (
  statuses?: Record<string, number>
): Record<BrokerStatus, ReactNode> => {
  const labels = useBrokerStatusLabels();
  return generateStatusLabelsWithCount(labels, statuses);
};

/**
 * Get controller status labels with counts for filter options
 */
export const useControllerStatusLabelsWithCount = (
  statuses?: Record<string, number>
): Record<ControllerStatus, ReactNode> => {
  const labels = useControllerStatusLabels();
  return generateStatusLabelsWithCount(labels, statuses);
};