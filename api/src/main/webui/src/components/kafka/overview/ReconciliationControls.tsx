import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Banner,
  Bullseye,
  Button,
  Flex,
  FlexItem,
} from '@patternfly/react-core';
import { KafkaCluster } from '@/api/types';
import { usePatchKafkaCluster } from '@/api/hooks/useKafkaClusters';
import { hasPrivilege } from '@/utils/privileges';
import { ReconciliationModal } from './ReconciliationModal';

interface ReconciliationControlsProps {
  cluster: KafkaCluster | undefined;
}

export function ReconciliationControls({
  cluster,
}: ReconciliationControlsProps) {
  const { t } = useTranslation();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const isManaged = cluster?.meta?.managed === true;
  const isReconciliationPaused = cluster?.meta?.reconciliationPaused ?? false;
  const canUpdate = hasPrivilege('UPDATE', cluster);

  const { mutate: patchKafkaCluster, isPending } = usePatchKafkaCluster(cluster?.id);

  if (!cluster || !isManaged) {
    return null;
  }

  const onConfirm = () => {
    patchKafkaCluster(!isReconciliationPaused, {
      onSuccess: () => {
        setIsModalOpen(false);
      },
    });
  };

  return (
    <>
      {isReconciliationPaused && (
        <Banner color="yellow">
          <Bullseye>
            <Flex spaceItems={{ default: 'spaceItemsMd' }}>
              <FlexItem spacer={{ default: 'spacerNone' }}>
                {t('reconciliation.reconciliation_paused_warning')}
              </FlexItem>
              {canUpdate && (
                <FlexItem
                  spacer={{ default: 'spacerLg' }}
                  style={{ marginLeft: '0.75rem' }}
                >
                  <Button
                    variant="link"
                    isInline
                    onClick={() => setIsModalOpen(true)}
                  >
                    {t('reconciliation.resume')}
                  </Button>
                </FlexItem>
              )}
            </Flex>
          </Bullseye>
        </Banner>
      )}
      {isModalOpen && (
        <ReconciliationModal
          isOpen={isModalOpen}
          isReconciliationPaused={isReconciliationPaused}
          isPending={isPending}
          onClose={() => setIsModalOpen(false)}
          onConfirm={onConfirm}
        />
      )}
    </>
  );
}