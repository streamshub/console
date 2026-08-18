import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Divider,
  Stack,
  StackItem,
  Tooltip,
} from '@patternfly/react-core';
import { ChartLineIcon, HelpIcon } from '@patternfly/react-icons';
import { UseQueryResult } from '@tanstack/react-query';
import { ListResponse, Node, NodeListMeta } from '@/api/types';
import { ChartSkeletonLoader } from '@/components/kafka/overview/ChartSkeletonLoader';
import { ChartNodeStorageUsage } from './charts/ChartNodeStorageUsage';
import { ChartPartitionDistribution } from './charts/ChartPartitionDistribution';

export interface NodeChartsCardProps {
  nodeResult: UseQueryResult<ListResponse<Node, NodeListMeta>, Error>;
}

export function NodeChartsCard({ nodeResult }: NodeChartsCardProps) {
  const { t } = useTranslation();
  const nodes = useMemo(() => nodeResult.data?.data ?? [], [nodeResult.data]);

  return (
    <Card component="div">
      <CardHeader>
        <CardTitle>
          <ChartLineIcon style={{ marginRight: 'var(--pf-t--global--spacer--sm)' }} />
          {t('nodes.charts.title')}
        </CardTitle>
      </CardHeader>
      <CardBody>
        <Stack hasGutter>
          {nodeResult.isLoading ? (
            <>
              <StackItem>
                <ChartSkeletonLoader />
              </StackItem>
              <StackItem>
                <Divider />
              </StackItem>
              <StackItem>
                <ChartSkeletonLoader />
              </StackItem>
            </>
          ) : (
            <>
              <StackItem>
                <div>
                  <strong>{t('nodes.charts.storageUsage')}</strong>
                </div>
                <div style={{ color: 'var(--pf-t--global--text--color--subtle)', fontSize: 'var(--pf-t--global--font--size--sm)' }}>
                  {t('nodes.charts.storageUsageSubtitle')}{' '}
                  <Tooltip content={t('nodes.charts.storageUsageTooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </div>
              </StackItem>
              <StackItem>
                <ChartNodeStorageUsage nodes={nodes} />
              </StackItem>

              <StackItem>
                <Divider />
              </StackItem>

              <StackItem>
                <div>
                  <strong>{t('nodes.charts.partitionDistribution')}</strong>
                </div>
                <div style={{ color: 'var(--pf-t--global--text--color--subtle)', fontSize: 'var(--pf-t--global--font--size--sm)' }}>
                  {t('nodes.charts.partitionDistributionSubtitle')}{' '}
                  <Tooltip content={t('nodes.charts.partitionDistributionTooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </div>
              </StackItem>
              <StackItem>
                <ChartPartitionDistribution nodes={nodes} />
              </StackItem>
            </>
          )}
        </Stack>
      </CardBody>
    </Card>
  );
}
