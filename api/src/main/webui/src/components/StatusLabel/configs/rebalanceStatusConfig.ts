/**
 * Rebalance Status Configuration
 * 
 * Centralized configuration for Kafka rebalance status labels, icons, and tooltips.
 * Used by RebalancesTable component.
 * 
 * Note: This config uses a factory function to support i18n translations.
 */

import {
  CheckIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  OutlinedClockIcon,
} from '@patternfly/react-icons';
import type { StatusConfig } from '../StatusLabel';
import type { RebalanceStatus } from '@/api/types';
import type { TFunction } from 'i18next';

/**
 * Creates rebalance status configuration with i18n support
 * 
 * @param t - Translation function from useTranslation hook
 * @returns Configuration object for rebalance statuses
 */
export function createRebalanceStatusConfig(
  t: TFunction
): Record<RebalanceStatus, StatusConfig> {
  return {
    New: {
      icon: ExclamationCircleIcon,
      label: t('rebalancing.statuses.new.label'),
      tooltip: t('rebalancing.statuses.new.tooltip'),
    },
    PendingProposal: {
      icon: PendingIcon,
      label: t('rebalancing.statuses.pendingProposal.label'),
      tooltip: t('rebalancing.statuses.pendingProposal.tooltip'),
    },
    ProposalReady: {
      icon: CheckIcon,
      label: t('rebalancing.statuses.proposalReady.label'),
      tooltip: t('rebalancing.statuses.proposalReady.tooltip'),
    },
    Stopped: {
      icon: PauseCircleIcon, // Note: Component may replace with custom stop icon
      label: t('rebalancing.statuses.stopped.label'),
      tooltip: t('rebalancing.statuses.stopped.tooltip'),
    },
    Rebalancing: {
      icon: PendingIcon,
      label: t('rebalancing.statuses.rebalancing.label'),
      tooltip: t('rebalancing.statuses.rebalancing.tooltip'),
    },
    NotReady: {
      icon: OutlinedClockIcon,
      label: t('rebalancing.statuses.notReady.label'),
      tooltip: t('rebalancing.statuses.notReady.tooltip'),
    },
    Ready: {
      icon: CheckIcon,
      label: t('rebalancing.statuses.ready.label'),
      tooltip: t('rebalancing.statuses.ready.tooltip'),
    },
    ReconciliationPaused: {
      icon: PauseCircleIcon,
      label: t('rebalancing.statuses.reconciliationPaused.label'),
      tooltip: t('rebalancing.statuses.reconciliationPaused.tooltip'),
    },
  };
}