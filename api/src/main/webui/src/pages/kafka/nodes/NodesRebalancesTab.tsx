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
import { Rebalance } from '@/api/types';

export function NodesRebalancesTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const { data: clusterData, isLoading: isClusterLoading } = useKafkaCluster(kafkaId, { fields: 'cruiseControlEnabled' });
  // While the cluster data is loading we don't yet know whether CC is enabled,
  // so default to undefined (not true) to avoid a premature rebalances fetch or
  // a false-positive "not enabled" flash.
  const cruiseControlEnabled = isClusterLoading
    ? undefined
    : (clusterData?.data?.attributes?.cruiseControlEnabled ?? false);

  const [dataParams, setDataParams] = useState<ResourceListParams>({});
  // Disable the rebalances query until we know CC is enabled, avoiding a
  // wasted network request on clusters where it is not configured.
  const rebalanceResult = useRebalances(kafkaId, { ...dataParams, enabled: cruiseControlEnabled === true });

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams(params);
  }, []);

  // Confirmation modal state
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<'approve' | 'stop' | 'refresh'>('approve');
  const [pendingRebalance, setPendingRebalance] = useState<Rebalance | null>(null);

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

  const handleConfirmAction = useCallback(() => {
    if (pendingRebalance) {
      patchRebalance({ rebalanceId: pendingRebalance.id, action: pendingAction });
    }
    setIsConfirmModalOpen(false);
    setPendingRebalance(null);
  }, [pendingRebalance, patchRebalance, pendingAction]);

  const handleCancelAction = useCallback(() => {
    setIsConfirmModalOpen(false);
    setPendingRebalance(null);
  }, []);

  if (cruiseControlEnabled === false) {
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
      />

      <RebalanceConfirmationModal
        isOpen={isConfirmModalOpen}
        action={pendingAction}
        onConfirm={handleConfirmAction}
        onCancel={handleCancelAction}
      />
    </PageSection>
  );
}
