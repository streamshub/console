/**
 * ManagedConnectorLabel - Badge component for managed connectors
 *
 * Displays a badge indicating that a connector is managed by the Strimzi Connector Operator
 * through a KafkaConnector custom resource.
 */

import { Label, Tooltip } from '@patternfly/react-core';
import { ServicesIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';

export function ManagedConnectorLabel() {
  const { t } = useTranslation();
  
  return (
    <Tooltip content={t('kafka.connect.managedTooltip')}>
      <Label
        isCompact={true}
        color="yellow"
        icon={<ServicesIcon />}
        style={{ marginLeft: 'var(--pf-t--global--spacer--sm)' }}
      >
        {t('common.managed')}
      </Label>
    </Tooltip>
  );
}