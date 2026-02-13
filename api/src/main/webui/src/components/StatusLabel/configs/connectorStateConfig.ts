/**
 * Connector State Configuration
 * 
 * Centralized configuration for Kafka Connect connector state labels and icons.
 * Used by ConnectorsTable component.
 */

import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  HistoryIcon,
} from '@patternfly/react-icons';
import type { StatusConfig } from '../StatusLabel';
import type { ConnectorState } from '@/api/types';

export const CONNECTOR_STATE_CONFIG: Record<ConnectorState, StatusConfig> = {
  UNASSIGNED: {
    icon: PendingIcon,
    label: 'Unassigned',
  },
  RUNNING: {
    icon: CheckCircleIcon,
    iconStatus: 'success',
    label: 'Running',
  },
  PAUSED: {
    icon: PauseCircleIcon,
    label: 'Paused',
  },
  STOPPED: {
    icon: PauseCircleIcon, // Will be replaced with custom stop icon in component
    label: 'Stopped',
  },
  FAILED: {
    icon: ExclamationCircleIcon,
    iconStatus: 'danger',
    label: 'Failed',
  },
  RESTARTING: {
    icon: HistoryIcon,
    label: 'Restarting',
  },
};