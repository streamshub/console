/**
 * Rebalance Modal Component
 * Displays optimization proposal details for a Kafka rebalance
 */

import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  Button,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Tooltip,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { Rebalance } from '../api/types';

interface RebalanceModalProps {
  rebalance: Rebalance | null;
  isOpen: boolean;
  onClose: () => void;
}

export function RebalanceModal({ rebalance, isOpen, onClose }: RebalanceModalProps) {
  const { t } = useTranslation();

  if (!rebalance) {
    return null;
  }

  const optimizationResult = rebalance.attributes.optimizationResult;
  const sessionId = rebalance.attributes.sessionId;

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t('rebalancing.optimizationProposal.title')}
      isOpen={isOpen}
      onClose={onClose}
    >
      <div style={{ padding: '1.5rem' }}>
        <p style={{ marginBottom: '1rem' }}>{t('rebalancing.optimizationProposal.description')}</p>
      <DescriptionList
        isHorizontal
        horizontalTermWidthModifier={{
          default: '12ch',
          sm: '15ch',
          md: '20ch',
          lg: '28ch',
          xl: '30ch',
          '2xl': '35ch',
        }}
      >
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.dataToMove')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.dataToMoveTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.dataToMoveMB || 0} MB
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.excludedBrokersForLeadership')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.excludedBrokersForLeadershipTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.excludedBrokersForLeadership &&
            optimizationResult.excludedBrokersForLeadership.length > 0
              ? optimizationResult.excludedBrokersForLeadership.join(', ')
              : '-'}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.excludedBrokersForReplicaMove')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.excludedBrokersForReplicaMoveTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.excludedBrokersForReplicaMove &&
            optimizationResult.excludedBrokersForReplicaMove.length > 0
              ? optimizationResult.excludedBrokersForReplicaMove.join(', ')
              : '-'}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.excludedTopics')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.excludedTopicsTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.excludedTopics && optimizationResult.excludedTopics.length > 0
              ? optimizationResult.excludedTopics.join(', ')
              : '-'}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.intraBrokerDataToMove')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.intraBrokerDataToMoveTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.intraBrokerDataToMoveMB || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.monitoredPartitionsPercentage')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.monitoredPartitionsPercentageTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.monitoredPartitionsPercentage || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.numIntraBrokerReplicaMovements')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.numIntraBrokerReplicaMovementsTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.numIntraBrokerReplicaMovements || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.numLeaderMovements')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.numLeaderMovementsTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.numLeaderMovements || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.numReplicaMovements')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.numReplicaMovementsTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.numReplicaMovements || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.onDemandBalancednessScoreAfter')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.onDemandBalancednessScoreAfterTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.onDemandBalancednessScoreAfter || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.onDemandBalancednessScoreBefore')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.onDemandBalancednessScoreBeforeTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.onDemandBalancednessScoreBefore || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.recentWindows')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.recentWindowsTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>
            {optimizationResult?.recentWindows || 0}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('rebalancing.optimizationProposal.sessionId')}{' '}
            <Tooltip content={t('rebalancing.optimizationProposal.sessionIdTooltip')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
          <DescriptionListDescription>{sessionId ?? '-'}</DescriptionListDescription>
        </DescriptionListGroup>
      </DescriptionList>
      </div>
      <div style={{ padding: '1.5rem', paddingTop: '0' }}>
        <Button variant="primary" onClick={onClose}>
          {t('common.close')}
        </Button>
      </div>
    </Modal>
  );
}