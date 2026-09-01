/**
 * Nodes Overview Tab - Shows cluster node summary and nodes table
 */

import { useState, useCallback } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Icon,
  PageSection,
  Tooltip,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
} from '@patternfly/react-icons';
import { useNodes } from '@/api/hooks/useNodes';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import { formatNumber } from '@/utils/format';
import { NodesDataView } from '@/components/kafka/nodes/NodesDataView';
import { NodeChartsCard } from '@/components/kafka/nodes/NodeChartsCard';

export function NodesOverviewTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  // Table data driven by NodesDataView
  const [tableParams, setTableParams] = useState<ResourceListParams>({});
  const nodeResult = useNodes(kafkaId, tableParams);
  const nodeChartsResult = useNodes(kafkaId, {
    fields: 'storageCapacity,storageUsed,broker',
    page: {
      sort: 'id',
      size: 1000,
    },
  });

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setTableParams(params);
  }, []);

  const summary = nodeResult.data?.meta?.summary;

  const leadControllerId = summary?.leaderId ?? '';

  const totalNodes = Object.values(summary?.statuses?.combined ?? {}).reduce<number>(
    (sum, count) => sum + Number(count),
    0,
  );

  const brokersTotal = Object.values(summary?.statuses?.brokers ?? {}).reduce<number>(
    (sum, count) => sum + Number(count),
    0,
  );
  const brokersWarning = Object.keys(summary?.statuses?.brokers ?? {}).some(
    (key) => key !== 'Running',
  );

  const controllersTotal = Object.values(summary?.statuses?.controllers ?? {}).reduce<number>(
    (sum, count) => sum + Number(count),
    0,
  );
  const controllersWarning = Object.keys(summary?.statuses?.controllers ?? {}).some(
    (key) => key !== 'QuorumLeader' && key !== 'QuorumFollower',
  );

  return (
    <PageSection isFilled>
      <Grid hasGutter>
        <GridItem>
          <Card style={{ background: 'var(--pf-t--global--background--color--secondary--default)' }} ouiaId={"summary"}>
            <CardBody>
              <DescriptionList
                isCompact
                isHorizontal
                columnModifier={{ default: '2Col' }}
              >
                <DescriptionListGroup data-ouia-component-id={"total-node-count"}>
                  <DescriptionListTerm style={{ whiteSpace: 'nowrap' }}>
                    {t('nodes.distribution.totalNodes')}{' '}
                    <Tooltip content={t('nodes.distribution.totalNodesTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {formatNumber(totalNodes)}
                  </DescriptionListDescription>
                </DescriptionListGroup>

                <DescriptionListGroup data-ouia-component-id={"controller-node-count"}>
                  <DescriptionListTerm style={{ whiteSpace: 'nowrap' }}>{t('nodes.distribution.controllerRole')}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {controllersWarning ? (
                      <Icon status="warning"><ExclamationTriangleIcon /></Icon>
                    ) : (
                      <Icon status="success"><CheckCircleIcon /></Icon>
                    )}
                    {' '}{formatNumber(controllersTotal)}
                  </DescriptionListDescription>
                </DescriptionListGroup>

                <DescriptionListGroup data-ouia-component-id={"lead-controller"}>
                  <DescriptionListTerm style={{ whiteSpace: 'nowrap' }}>
                    {t('nodes.distribution.leadController')}{' '}
                    <Tooltip content={t('nodes.distribution.leadControllerTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {t('nodes.distribution.leadControllerValue', { leadController: leadControllerId })}
                  </DescriptionListDescription>
                </DescriptionListGroup>

                <DescriptionListGroup data-ouia-component-id={"broker-node-count"}>
                  <DescriptionListTerm style={{ whiteSpace: 'nowrap' }}>{t('nodes.distribution.brokerRole')}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {brokersWarning ? (
                      <Icon status="warning"><ExclamationTriangleIcon /></Icon>
                    ) : (
                      <Icon status="success"><CheckCircleIcon /></Icon>
                    )}
                    {' '}{formatNumber(brokersTotal)}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </DescriptionList>
            </CardBody>
          </Card>
        </GridItem>

        <GridItem>
          <NodesDataView
            kafkaId={kafkaId!}
            nodeResult={nodeResult}
            onDataViewChange={handleDataViewChange}
          />
        </GridItem>

        <GridItem>
          <NodeChartsCard nodeResult={nodeChartsResult} />
        </GridItem>
      </Grid>
    </PageSection>
  );
}
