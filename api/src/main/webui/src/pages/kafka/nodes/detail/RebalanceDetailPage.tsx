import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  AlertActionCloseButton,
  Button,
  CodeBlock,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Divider,
  EmptyState,
  EmptyStateBody,
  Flex,
  FlexItem,
  Label,
  LabelGroup,
  PageSection,
  Spinner,
  Title,
} from '@patternfly/react-core';
import { SyncAltIcon } from '@patternfly/react-icons';
import { useRebalance, usePatchRebalance } from '@/api/hooks/useRebalances';
import { usePageTitle } from '@/hooks';
import { StatusLabel } from '@/components/StatusLabel';
import { createRebalanceStatusConfig } from '@/components/StatusLabel/configs';
import { RebalanceConfirmationModal } from '@/components/kafka/nodes/RebalanceConfirmationModal';
import { BrokerImpactTable } from '@/components/kafka/nodes/BrokerImpactTable';
import { ProposalDetailCard } from '@/components/kafka/nodes/ProposalDetailCard';
import { hasPrivilege } from '@/utils/privileges';
import { formatDateTime } from '@/utils/dateTime';
import { Rebalance } from '@/api/types';

function getLastUpdated(rebalance: Rebalance): string {
  const statusCondition = rebalance.attributes.conditions?.find(
    (c) => c.type === rebalance.attributes.status,
  );
  return statusCondition?.lastTransitionTime || rebalance.attributes.creationTimestamp || '';
}

export function RebalanceDetailPage() {
  const { t } = useTranslation();
  const { kafkaId, rebalanceId } = useParams<{ kafkaId: string; rebalanceId: string }>();

  const { data, isLoading, error, refetch } = useRebalance(kafkaId, rebalanceId);
  const rebalance = data?.data;

  const statusConfig = useMemo(() => createRebalanceStatusConfig(t), [t]);

  usePageTitle(rebalance?.attributes.name);

  // Action confirmation state
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<'approve' | 'stop' | 'refresh'>('approve');

  // Alert dismiss state
  const [isAlertDismissed, setIsAlertDismissed] = useState(false);

  const { mutate: patchRebalance } = usePatchRebalance(kafkaId!);

  const handleAction = useCallback((action: 'approve' | 'stop' | 'refresh') => {
    setPendingAction(action);
    setIsConfirmModalOpen(true);
  }, []);

  const handleConfirmAction = useCallback(() => {
    if (rebalance) {
      patchRebalance(
        { rebalanceId: rebalance.id, action: pendingAction },
        { onSuccess: () => { void refetch(); } },
      );
    }
    setIsConfirmModalOpen(false);
  }, [rebalance, patchRebalance, pendingAction, refetch]);

  const handleCancelAction = useCallback(() => {
    setIsConfirmModalOpen(false);
  }, []);

  if (isLoading) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error || !rebalance) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error?.message ?? t('rebalancing.rebalanceNotFound')}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  const canUpdate = hasPrivilege('UPDATE', rebalance);
  const allowedActions = rebalance.meta?.allowedActions ?? [];
  const status = rebalance.attributes.status;
  const lastUpdated = getLastUpdated(rebalance);

  const modeLabel =
    rebalance.attributes.mode === 'full'
      ? t('rebalancing.fullMode')
      : rebalance.attributes.mode === 'add-brokers'
        ? t('rebalancing.addBrokersMode')
        : t('rebalancing.removeBrokersMode');

  return (
    <>
      {/* Header */}
      <PageSection>
        <Flex justifyContent={{ default: 'justifyContentSpaceBetween' }} alignItems={{ default: 'alignItemsCenter' }}>
          <FlexItem>
            <Title headingLevel="h1" size="2xl" ouiaId={"title"}>
              {rebalance.attributes.name}
            </Title>
          </FlexItem>
        </Flex>
      </PageSection>

      {/* Proposal-ready alert */}
      {status === 'ProposalReady' && !isAlertDismissed && (
        <PageSection>
          <Alert
            variant="info"
            isInline
            title={t('rebalancing.proposalReadyAlert.title')}
            actionClose={<AlertActionCloseButton onClose={() => setIsAlertDismissed(true)} />}
          >
            {t('rebalancing.proposalReadyAlert.description')}
          </Alert>
        </PageSection>
      )}

      {/* Action buttons */}
      <PageSection>
        <Flex gap={{ default: 'gapSm' }}>
          <FlexItem>
            <Button
              variant="primary"
              isDisabled={!canUpdate || !allowedActions.includes('approve')}
              onClick={() => handleAction('approve')}
            >
              {t('rebalancing.approve')}
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              variant="secondary"
              icon={<SyncAltIcon />}
              isDisabled={!canUpdate || !allowedActions.includes('refresh')}
              onClick={() => handleAction('refresh')}
            >
              {t('rebalancing.refreshProposal')}
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              variant="secondary"
              isDisabled={!canUpdate || !allowedActions.includes('stop')}
              onClick={() => handleAction('stop')}
            >
              {t('rebalancing.stop')}
            </Button>
          </FlexItem>
        </Flex>
      </PageSection>

      {/* Metadata */}
      <PageSection>
        <DescriptionList
          columnModifier={{ default: '2Col' }}
        >
          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.rebalanceName')}</DescriptionListTerm>
            <DescriptionListDescription>{rebalance.attributes.name}</DescriptionListDescription>
          </DescriptionListGroup>

          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.namespace')}</DescriptionListTerm>
            <DescriptionListDescription>{rebalance.attributes.namespace ?? '–'}</DescriptionListDescription>
          </DescriptionListGroup>

          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.created')}</DescriptionListTerm>
            <DescriptionListDescription>
              {formatDateTime({ value: rebalance.attributes.creationTimestamp })}
            </DescriptionListDescription>
          </DescriptionListGroup>

          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.lastUpdated')}</DescriptionListTerm>
            <DescriptionListDescription>
              {formatDateTime({ value: lastUpdated })}
            </DescriptionListDescription>
          </DescriptionListGroup>

          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.mode')}</DescriptionListTerm>
            <DescriptionListDescription>{modeLabel}</DescriptionListDescription>
          </DescriptionListGroup>

          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.autoApprovalEnabled')}</DescriptionListTerm>
            <DescriptionListDescription>
              {String(rebalance.meta?.autoApproval === true)}
            </DescriptionListDescription>
          </DescriptionListGroup>

          {rebalance.attributes.goals && rebalance.attributes.goals.length > 0 && (
            <DescriptionListGroup>
              <DescriptionListTerm>{t('rebalancing.goals')}</DescriptionListTerm>
              <DescriptionListDescription>
                <LabelGroup>
                  {rebalance.attributes.goals.map((goal) => (
                    <Label key={goal}>{goal}</Label>
                  ))}
                </LabelGroup>
              </DescriptionListDescription>
            </DescriptionListGroup>
          )}

          <DescriptionListGroup>
            <DescriptionListTerm>{t('rebalancing.status')}</DescriptionListTerm>
            <DescriptionListDescription>
              {status ?
                <>
                  <Label variant='outline' status={statusConfig[status].iconStatus} icon={<></>}>
                    <StatusLabel status={status} config={statusConfig} />
                  </Label>
                  {rebalance.attributes.conditions
                    ?.filter(c => c.type === status)
                    .filter(c => c.message?.length ?? 0 > 0) 
                    .map(c => {
                      return <>
                        <Divider style={{padding: '1em' }} />
                        <CodeBlock>{c.message}</CodeBlock>
                      </>;
                    })
                  }
                </>
                : '–'}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </PageSection>

      <PageSection>
        <Divider />
      </PageSection>

      {/* Broker impact table */}
      <PageSection>
        <Title headingLevel="h2" size="lg" style={{ marginBottom: 'var(--pf-t--global--spacer--md)' }}>
          {t('rebalancing.brokerImpact.title')}
        </Title>
        <BrokerImpactTable
          brokerCapacity={rebalance.attributes.brokerCapacity}
          brokerImpact={rebalance.attributes.optimizationProposal?.brokerImpact} />
      </PageSection>

      {/* Proposal detail expandable card */}
      <PageSection>
        <ProposalDetailCard rebalance={rebalance} />
      </PageSection>

      <RebalanceConfirmationModal
        isOpen={isConfirmModalOpen}
        action={pendingAction}
        onConfirm={handleConfirmAction}
        onCancel={handleCancelAction}
      />
    </>
  );
}
