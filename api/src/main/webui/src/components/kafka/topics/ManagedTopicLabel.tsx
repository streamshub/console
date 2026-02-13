/**
 * ManagedTopicLabel - Badge component for managed topics
 *
 * Displays a badge indicating that a topic is managed by the Strimzi Topic Operator
 * through a KafkaTopic custom resource.
 */

import { Label, Tooltip } from '@patternfly/react-core';
import { ServicesIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';

export function ManagedTopicLabel() {
  const { t } = useTranslation();
  
  return (
    <Tooltip content={t('topics.managedTooltip')}>
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