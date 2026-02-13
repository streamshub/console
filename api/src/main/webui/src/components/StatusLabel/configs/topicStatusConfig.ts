/**
 * Topic Status Configuration
 * 
 * Centralized configuration for topic status labels and icons.
 * Used by TopicsPage and TopicsFilterToolbar components.
 */

import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ExclamationCircleIcon,
} from '@patternfly/react-icons';
import type { StatusConfig } from '../StatusLabel';

export type TopicStatus = 
  | 'FullyReplicated'
  | 'UnderReplicated'
  | 'PartiallyOffline'
  | 'Unknown'
  | 'Offline';

export const TOPIC_STATUS_CONFIG: Record<TopicStatus, StatusConfig> = {
  FullyReplicated: {
    icon: CheckCircleIcon,
    iconStatus: 'success',
    label: 'Fully replicated',
  },
  UnderReplicated: {
    icon: ExclamationTriangleIcon,
    iconStatus: 'warning',
    label: 'Under replicated',
  },
  PartiallyOffline: {
    icon: ExclamationTriangleIcon,
    iconStatus: 'warning',
    label: 'Partially offline',
  },
  Unknown: {
    icon: ExclamationTriangleIcon,
    iconStatus: 'warning',
    label: 'Unknown',
  },
  Offline: {
    icon: ExclamationCircleIcon,
    iconStatus: 'danger',
    label: 'Offline',
  },
};