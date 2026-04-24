/**
 * Group State Configuration
 * 
 * Centralized configuration for consumer group state labels and icons.
 * Used by GroupsTable and GroupsPage components.
 */

import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InfoCircleIcon,
  OffIcon,
  PendingIcon,
  HistoryIcon,
  SyncAltIcon,
  InProgressIcon,
} from '@patternfly/react-icons';
import type { StatusConfig } from '../StatusLabel';
import type { GroupState } from '@/api/types';

export const GROUP_STATE_CONFIG: Record<GroupState, StatusConfig> = {
  STABLE: {
    icon: CheckCircleIcon,
    iconStatus: 'success',
    label: 'Stable',
  },
  EMPTY: {
    icon: InfoCircleIcon,
    iconStatus: 'info',
    label: 'Empty',
  },
  UNKNOWN: {
    icon: ExclamationTriangleIcon,
    iconStatus: 'warning',
    label: 'Unknown',
  },
  PREPARING_REBALANCE: {
    icon: PendingIcon,
    label: 'Preparing Rebalance',
  },
  ASSIGNING: {
    icon: HistoryIcon,
    label: 'Assigning',
  },
  DEAD: {
    icon: OffIcon,
    label: 'Dead',
  },
  COMPLETING_REBALANCE: {
    icon: SyncAltIcon,
    label: 'Completing Rebalance',
  },
  RECONCILING: {
    icon: InProgressIcon,
    label: 'Reconciling',
  },
};