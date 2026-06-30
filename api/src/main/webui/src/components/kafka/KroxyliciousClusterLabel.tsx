/**
 * Kroxylicious Cluster Label Component
 *
 * Displays a badge indicating that a cluster is a Kroxylicious virtual cluster.
 * Shows a blue label with an info icon and tooltip explaining what it means.
 */

import { KafkaCluster } from '@/api/types';
import { Label, Tooltip } from '@patternfly/react-core';
import { InfoCircleIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';

export function KroxyliciousClusterLabel({
  cluster,
  clusterKind,
} : {
  cluster?: KafkaCluster;
  clusterKind?: string;
}) {
  const { t } = useTranslation();
  const kind = clusterKind || cluster?.meta?.kind;

  if (kind !== 'virtualkafkaclusters.kroxylicious.io') {
    return null;
  }

  return (
    <Tooltip content={t('KroxyliciousCluster.tooltip')}>
      <Label
        isCompact
        color="blue"
        icon={<InfoCircleIcon />}
        className="pf-v6-u-ml-sm"
      >
        {t('KroxyliciousCluster.label')}
      </Label>
    </Tooltip>
  );
}