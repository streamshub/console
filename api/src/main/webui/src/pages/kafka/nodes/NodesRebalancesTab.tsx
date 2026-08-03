/**
 * Nodes Rebalances Tab - Shows Kafka rebalances
 */

import { useState, useCallback } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Button,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
} from '@patternfly/react-core';
import { BalanceScaleIcon } from '@patternfly/react-icons';
import { useRebalances, usePatchRebalance } from '@/api/hooks/useRebalances';
import { useKafkaCluster } from '@/api/hooks/useKafkaClusters';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import { RebalancesDataView } from '@/components/kafka/nodes/RebalancesDataView';
import { RebalanceConfirmationModal } from '@/components/kafka/nodes/RebalanceConfirmationModal';
import { RebalanceModal } from '@/components/kafka/nodes/RebalanceModal';
import { Rebalance } from '@/api/types';

export function NodesRebalancesTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const { data: clusterData } = useKafkaCluster(kafkaId, { fields: 'cruiseControlEnabled' });
  const cruiseControlEnabled = clusterData?.data?.attributes?.cruiseControlEnabled ?? false;

  // Table params driven by RebalancesDataView
  const [dataParams, setDataParams] = useState<ResourceListParams>({});
  const rebalanceResult = useRebalances(kafkaId, dataParams);

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams(params);
  }, []);

  // Confirmation modal state
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<'approve' | 'stop' | 'refresh'>('approve');
  const [pendingRebalance, setPendingRebalance] = useState<Rebalance | null>(null);

  // Detail modal state
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedRebalance, setSelectedRebalance] = useState<Rebalance | null>(null);

  const { mutate: patchRebalance } = usePatchRebalance(kafkaId!);

  const handleApprove = useCallback((rebalance: Rebalance) => {
    setPendingRebalance(rebalance);
    setPendingAction('approve');
    setIsConfirmModalOpen(true);
  }, []);

  const handleStop = useCallback((rebalance: Rebalance) => {
    setPendingRebalance(rebalance);
    setPendingAction('stop');
    setIsConfirmModalOpen(true);
  }, []);

  const handleRefresh = useCallback((rebalance: Rebalance) => {
    setPendingRebalance(rebalance);
    setPendingAction('refresh');
    setIsConfirmModalOpen(true);
  }, []);

  const handleViewDetails = useCallback((rebalance: Rebalance) => {
    setSelectedRebalance(rebalance);
    setIsDetailModalOpen(true);
  }, []);

  const handleConfirmAction = () => {
    if (pendingRebalance) {
      patchRebalance({ rebalanceId: pendingRebalance.id, action: pendingAction });
    }
    setIsConfirmModalOpen(false);
    setPendingRebalance(null);
  };

  const handleCancelAction = () => {
    setIsConfirmModalOpen(false);
    setPendingRebalance(null);
  };

  if (!cruiseControlEnabled) {
    return (
      <PageSection isFilled>
        <EmptyState headingLevel="h2" icon={BalanceScaleIcon} titleText={t('rebalancing.cruiseControlNotEnabled')}>
          <EmptyStateBody>{t('rebalancing.cruiseControlNotEnabledDescription')}</EmptyStateBody>
          <EmptyStateFooter>
            <EmptyStateActions>
              <Button
                variant="link"
                component="a"
                href={t('rebalancing.cruiseControlLink')}
                target="_blank"
                rel="noopener noreferrer"
              >
                {t('rebalancing.cruiseControlGetStarted')}
              </Button>
            </EmptyStateActions>
          </EmptyStateFooter>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <PageSection isFilled>
      <RebalancesDataView
        kafkaId={kafkaId!}
        rebalanceResult={rebalanceResult}
        onDataViewChange={handleDataViewChange}
        onApprove={handleApprove}
        onStop={handleStop}
        onRefresh={handleRefresh}
        onViewDetails={handleViewDetails}
      />

      <RebalanceConfirmationModal
        isOpen={isConfirmModalOpen}
        action={pendingAction}
        onConfirm={handleConfirmAction}
        onCancel={handleCancelAction}
      />

      <RebalanceModal
        rebalance={selectedRebalance}
        isOpen={isDetailModalOpen}
        onClose={() => { setIsDetailModalOpen(false); setSelectedRebalance(null); }}
      />
    </PageSection>
  );
}
