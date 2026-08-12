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
}

function StatTile({ value, label }: StatTileProps) {
  return (
    <Card isCompact style={{ flex: '1 1 0', background: 'var(--pf-t--global--background--color--secondary--default)' }}>
      <CardBody style={{ textAlign: 'center' }}>
        <p style={{
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
  const sessionId = rebalance.attributes.sessionId;

  return (
    <Card isPlain style={{ border: '1px solid var(--pf-t--global--border--color--default)' }}>
      <CardBody style={{ padding: 'var(--pf-t--global--spacer--md)' }}>
        <ExpandableSection
          toggleText={t('rebalancing.proposalDetail.title')}
          isExpanded={isExpanded}
          onToggle={(_e, expanded) => setIsExpanded(expanded)}
        >
          {/* Summary stat tiles */}
          <Flex gap={{ default: 'gapMd' }} style={{ margin: 'var(--pf-t--global--spacer--md) 0 var(--pf-t--global--spacer--lg)' }}>
            <FlexItem flex={{ default: 'flex_1' }}>
              <StatTile
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
          {opt ? (
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
                  {t('rebalancing.optimizationProposal.sessionId')}{' '}
                  <Tooltip content={t('rebalancing.optimizationProposal.sessionIdTooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </DescriptionListTerm>
                <DescriptionListDescription>
                  {sessionId ?? '–'}
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
                  {opt.recentWindows ?? '-'}
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
                  {opt.onDemandBalancednessScoreBefore ?? '-'}
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
                  {opt.onDemandBalancednessScoreAfter ?? '-'}
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
                  {opt.numIntraBrokerReplicaMovements ?? '-'}
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
                  {opt.intraBrokerDataToMoveMB ? opt.intraBrokerDataToMoveMB + ' MB' : '-'}
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
                  {opt.excludedBrokersForReplicaMove?.length
                    ? opt.excludedBrokersForReplicaMove.join(', ')
                    : '–'}
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
                  {opt.excludedBrokersForLeadership?.length
                    ? opt.excludedBrokersForLeadership.join(', ')
                    : '–'}
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
                  {opt.excludedTopics?.length ? opt.excludedTopics.join(', ') : '–'}
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
                  {opt.monitoredPartitionsPercentage ?? '-'}
                </DescriptionListDescription>
              </DescriptionListGroup>

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
