import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  ExpandableSection,
  Flex,
  FlexItem,
  Tooltip,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { Rebalance } from '@/api/types';

interface ProposalDetailCardProps {
  rebalance: Rebalance;
}

interface StatTileProps {
  value: string | number;
  label: string | React.ReactNode;
  ouiaId: string;
}

function StatTile({ value, label, ouiaId }: StatTileProps) {
  return (
    <Card isCompact style={{ flex: '1 1 0', background: 'var(--pf-t--global--background--color--secondary--default)' }}>
      <CardBody style={{ textAlign: 'center' }}>
        <p data-ouia-component-id={ouiaId} style={{
          fontSize: 'var(--pf-t--global--font--size--2xl)',
          fontWeight: 'var(--pf-t--global--font--weight--body--bold)',
          color: 'var(--pf-t--global--color--brand--default)',
          margin: '0 0 var(--pf-t--global--spacer--xs)',
        }}>
          {value}
        </p>
        <small style={{ color: 'var(--pf-t--global--text--color--subtle)' }}>
          {label}
        </small>
      </CardBody>
    </Card>
  );
}

export function ProposalDetailCard({ rebalance }: ProposalDetailCardProps) {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);

  const opt = rebalance.attributes.optimizationResult;
  const proposalDetails = opt ? {
    sessionId: rebalance.attributes.sessionId ?? '–',
    recentWindows: opt.recentWindows ?? '-',
    onDemandBalancednessScoreBefore: opt.onDemandBalancednessScoreBefore ?? '-',
    onDemandBalancednessScoreAfter: opt.onDemandBalancednessScoreAfter ?? '-',
    numIntraBrokerReplicaMovements: opt.numIntraBrokerReplicaMovements ?? '-',
    intraBrokerDataToMoveMB: opt.intraBrokerDataToMoveMB ? opt.intraBrokerDataToMoveMB + ' MB' : '-',
    excludedBrokersForReplicaMove: opt.excludedBrokersForReplicaMove?.length ? opt.excludedBrokersForReplicaMove.join(', ') : '–',
    excludedBrokersForLeadership: opt.excludedBrokersForLeadership?.length ? opt.excludedBrokersForLeadership.join(', ') : '–',
    excludedTopics: opt.excludedTopics?.length ? opt.excludedTopics.join(', ') : '–',
    monitoredPartitionsPercentage: opt.monitoredPartitionsPercentage ?? '-',
  } : undefined;

  return (
    <Card isPlain style={{ border: '1px solid var(--pf-t--global--border--color--default)' }} ouiaId={"proposal-detail"}>
      <CardBody style={{ padding: 'var(--pf-t--global--spacer--md)' }}>
        <ExpandableSection
          toggleText={t('rebalancing.proposalDetail.title')}
          toggleId='rebalance-proposal-detail-toggle'
          isExpanded={isExpanded}
          onToggle={(_e, expanded) => setIsExpanded(expanded)}
        >
          {/* Summary stat tiles */}
          <Flex gap={{ default: 'gapMd' }} style={{ margin: 'var(--pf-t--global--spacer--md) 0 var(--pf-t--global--spacer--lg)' }}>
            <FlexItem flex={{ default: 'flex_1' }}>
              <StatTile
                ouiaId="numReplicaMovements"
                value={opt?.numReplicaMovements ?? '–'}
                label={
                  <>
                    {t('rebalancing.optimizationProposal.numReplicaMovements')}{' '}
                    <Tooltip content={t('rebalancing.optimizationProposal.numReplicaMovementsTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </>
                }
              />
            </FlexItem>
            <FlexItem flex={{ default: 'flex_1' }}>
              <StatTile
                ouiaId="numLeaderMovements"
                value={opt?.numLeaderMovements ?? '–'}
                label={
                  <>
                    {t('rebalancing.optimizationProposal.numLeaderMovements')}{' '}
                    <Tooltip content={t('rebalancing.optimizationProposal.numLeaderMovementsTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </>
                }
              />
            </FlexItem>
            <FlexItem flex={{ default: 'flex_1' }}>
              <StatTile
                ouiaId="dataToMoveMB"
                value={opt?.dataToMoveMB != null ? `${opt.dataToMoveMB} MB` : '–'}
                label={
                  <>
                    {t('rebalancing.optimizationProposal.dataToMove')}{' '}
                    <Tooltip content={t('rebalancing.optimizationProposal.dataToMoveTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </>
                }
              />
            </FlexItem>
          </Flex>

          {/* Detailed description list */}
          {proposalDetails ? (
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
              {Object.entries(proposalDetails).map(([key, value]) => (
                <DescriptionListGroup data-ouia-component-id={key}>
                  <DescriptionListTerm>
                    {t(`rebalancing.optimizationProposal.${key}`)}{' '}
                    <Tooltip content={t(`rebalancing.optimizationProposal.${key}Tooltip`)}>
                      <HelpIcon />
                    </Tooltip>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {value}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              ))}
            </DescriptionList>
          ) : (
            <p style={{ color: 'var(--pf-t--global--text--color--subtle)' }}>
              {t('rebalancing.proposalDetail.noProposalData')}
            </p>
          )}
        </ExpandableSection>
      </CardBody>
    </Card>
  );
}
