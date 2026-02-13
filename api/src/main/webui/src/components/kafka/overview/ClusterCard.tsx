/**
 * Cluster Card Component
 *
 * Displays cluster overview information including:
 * - Cluster name and status
 * - Broker counts (online/total)
 * - Consumer group count
 * - Kafka version
 * - Errors and warnings
 */

import * as React from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  Flex,
  FlexItem,
  Title,
  Button,
  Icon,
  Content,
  Divider,
  Grid,
  GridItem,
  Skeleton,
  Tooltip,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListItemCells,
  DataListCell,
  Truncate,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  HelpIcon,
  PauseCircleIcon,
  PlayIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
} from '@patternfly/react-icons';
import { KafkaCluster } from '@/api/types';
import { usePatchKafkaCluster } from '@/api/hooks/useKafkaClusters';
import { hasPrivilege } from '@/utils/privileges';
import { ReconciliationModal } from './ReconciliationModal';
import { ErrorsAndWarnings } from './ErrorsAndWarnings';
import { formatDistanceToNow } from 'date-fns';

export interface ClusterCardProps {
  cluster: KafkaCluster | undefined;
  groupsCount: number | undefined;
  brokersOnline: number;
  brokersTotal: number;
  isLoading: boolean;
}

export function ClusterCard({
  cluster,
  groupsCount,
  brokersOnline,
  brokersTotal,
  isLoading,
}: ClusterCardProps) {
  const { t } = useTranslation();
  const [isModalOpen, setIsModalOpen] = React.useState(false);

  const status = cluster?.attributes.status ??
    (brokersOnline === brokersTotal ? 'Ready' : 'Not Available');

  const isManaged = cluster?.meta?.managed;
  const isReconciliationPaused = cluster?.meta?.reconciliationPaused ?? false;
  const canUpdate = hasPrivilege('UPDATE', cluster);
  const { mutate: patchKafkaCluster, isPending } = usePatchKafkaCluster(cluster?.id);

  const onConfirmReconciliation = () => {
    patchKafkaCluster(!isReconciliationPaused, {
      onSuccess: () => {
        setIsModalOpen(false);
        // The mutation already invalidates the query cache, which should trigger a refetch
      },
    });
  };

  // Process conditions to extract errors and warnings
  const messages = React.useMemo(() => {
    const conditions = cluster?.attributes.conditions || [];
    return conditions
      .filter((c) => c.type !== 'Ready')
      .map((c) => ({
        variant: c.type === 'Error' ? ('danger' as const) : ('warning' as const),
        subject: {
          type: c.type || '',
          name: cluster?.attributes.name || '',
          id: cluster?.id || '',
        },
        message: c.message || '',
        date: c.lastTransitionTime || '',
      }));
  }, [cluster?.attributes.conditions, cluster?.attributes.name, cluster?.id]);

  // Calculate warning and danger counts
  const warnings = messages.filter((m) => m.variant === 'warning').length;
  const dangers = messages.filter((m) => m.variant === 'danger').length;

  return (
    <Card component="div">
      <CardBody>
        <Flex
          justifyContent={{ default: 'justifyContentSpaceBetween' }}
          alignItems={{ default: 'alignItemsCenter' }}
          flexWrap={{ default: 'wrap' }}
          spaceItems={{ default: 'spaceItemsMd' }}
        >
          <FlexItem>
            <Title headingLevel="h2">
              {t('ClusterCard.Kafka_cluster_details')}
            </Title>
          </FlexItem>
          {isManaged && canUpdate && (
            <FlexItem>
              <Button
                variant="link"
                icon={isReconciliationPaused ? <PlayIcon /> : <PauseCircleIcon />}
                onClick={() => setIsModalOpen(true)}
                isDisabled={isPending}
              >
                {isReconciliationPaused
                  ? t('reconciliation.resume_reconciliation')
                  : t('reconciliation.pause_reconciliation_button')}
              </Button>
            </FlexItem>
          )}
        </Flex>
        
        <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsMd' }}>
          <Flex
            flexWrap={{ default: 'wrap', sm: 'nowrap' }}
            spaceItems={{ default: 'spaceItemsMd' }}
            justifyContent={{ default: 'justifyContentCenter' }}
            style={{ textAlign: 'center' }}
          >
            {/* Cluster Status Section */}
            <Flex
              direction={{ default: 'column' }}
              alignSelf={{ default: 'alignSelfCenter' }}
              style={{ minWidth: '200px' }}
            >
              <FlexItem className="pf-v6-u-py-md">
                {isLoading ? (
                  <>
                    <Icon status="success">
                      <Skeleton shape="circle" width="1rem" />
                    </Icon>
                    <Skeleton width="100%" />
                    <Skeleton
                      width="50%"
                      height="0.7rem"
                      style={{ display: 'inline-block' }}
                    />
                  </>
                ) : (
                  <>
                    <Icon status="success">
                      <CheckCircleIcon />
                    </Icon>
                    <Title headingLevel="h2">
                      {cluster?.attributes.name ?? 'n/a'}
                    </Title>
                    <Content>
                      <Content component="small">{status}</Content>
                    </Content>
                  </>
                )}
              </FlexItem>
            </Flex>

            <Divider
              orientation={{ default: 'horizontal', sm: 'vertical' }}
            />

            {/* Metrics Section */}
            <Flex
              direction={{ default: 'column' }}
              alignSelf={{ default: 'alignSelfCenter' }}
              flex={{ default: 'flex_1' }}
              style={{ minWidth: '200px', textAlign: 'center' }}
            >
              <Grid>
                {/* Online Brokers */}
                <GridItem span={12} xl={4}>
                  <Link
                    to="../nodes"
                    className="pf-v6-u-font-size-xl"
                    style={{ textDecoration: 'none' }}
                  >
                    {isLoading ? (
                      <Skeleton
                        shape="circle"
                        width="1.5rem"
                        style={{ display: 'inline-block' }}
                      />
                    ) : (
                      <>
                        {brokersOnline}/{brokersTotal}
                      </>
                    )}
                  </Link>
                  <Content>
                    <Content component="small">
                      {t('ClusterCard.online_brokers')}
                    </Content>
                  </Content>
                </GridItem>

                {/* Consumer Groups */}
                <GridItem span={12} xl={4}>
                  <Link
                    to="../groups"
                    className="pf-v6-u-font-size-xl"
                    style={{ textDecoration: 'none' }}
                  >
                    {isLoading ? (
                      <Skeleton
                        shape="circle"
                        width="1.5rem"
                        style={{ display: 'inline-block' }}
                      />
                    ) : (
                      groupsCount ?? 0
                    )}
                  </Link>
                  <Content>
                    <Content component="small">
                      {t('ClusterCard.consumer_groups')}
                    </Content>
                  </Content>
                </GridItem>

                {/* Kafka Version */}
                <GridItem span={12} xl={4}>
                  <div className="pf-v6-u-font-size-xl">
                    {isLoading ? (
                      <Skeleton />
                    ) : (
                      <>
                        {cluster?.attributes.kafkaVersion ? (
                          <>
                            {cluster.attributes.kafkaVersion}
                            {!isManaged && (
                              <>
                                {' '}
                                <Tooltip content={t('ClustersTable.version_derived')}>
                                  <HelpIcon />
                                </Tooltip>
                              </>
                            )}
                          </>
                        ) : (
                          t('ClustersTable.not_available')
                        )}
                      </>
                    )}
                  </div>
                  <Content>
                    <Content component="small">
                      {t('ClusterCard.kafka_version')}
                    </Content>
                  </Content>
                </GridItem>
              </Grid>
            </Flex>
          </Flex>

          {/* Errors and Warnings Section */}
          <Divider />
          <FlexItem>
            <ErrorsAndWarnings warnings={warnings} dangers={dangers}>
              <DataList
                aria-label={t('ClusterCard.cluster_errors_and_warnings')}
                isCompact
              >
                {isLoading ? (
                  Array.from({ length: 5 }).map((_, i) => (
                    <DataListItem key={i}>
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="name">
                              <Skeleton />
                            </DataListCell>,
                            <DataListCell key="message">
                              <Skeleton />
                            </DataListCell>,
                            <DataListCell key="date">
                              <Skeleton />
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  ))
                ) : (
                  <>
                    {messages.length === 0 && (
                      <DataListItem aria-labelledby="no-messages">
                        <DataListItemRow>
                          <DataListItemCells
                            dataListCells={[
                              <DataListCell key="name">
                                <span id="no-messages">
                                  {t('ClusterCard.no_messages')}
                                </span>
                              </DataListCell>,
                            ]}
                          />
                        </DataListItemRow>
                      </DataListItem>
                    )}
                    {messages
                      .sort((a, b) => b.date.localeCompare(a.date))
                      .map((m, i) => (
                        <DataListItem aria-labelledby={`message-${i}`} key={i}>
                          <DataListItemRow>
                            <DataListItemCells
                              dataListCells={[
                                <DataListCell
                                  key="name"
                                  className="pf-v6-u-text-nowrap"
                                  width={1}
                                >
                                  <Icon status={m.variant}>
                                    {m.variant === 'danger' && (
                                      <ExclamationCircleIcon />
                                    )}
                                    {m.variant === 'warning' && (
                                      <ExclamationTriangleIcon />
                                    )}
                                  </Icon>
                                  &nbsp;
                                  {m.subject.type}
                                </DataListCell>,
                                <DataListCell key="message" width={5}>
                                  <div className="pf-v6-u-display-none pf-v6-u-display-block-on-md">
                                    <Truncate
                                      content={
                                        m.subject.type === 'ReconciliationPaused'
                                          ? t('reconciliation.reconciliation_paused_warning')
                                          : m.message
                                      }
                                    />
                                    {isReconciliationPaused && m.subject.type === 'ReconciliationPaused' && (
                                      <>
                                        &nbsp;
                                        <Button
                                          variant="link"
                                          isInline
                                          onClick={() => setIsModalOpen(true)}
                                        >
                                          {t('reconciliation.resume')}
                                        </Button>
                                      </>
                                    )}
                                  </div>
                                  <div className="pf-v6-u-display-block pf-v6-u-display-none-on-md">
                                    {m.message}
                                  </div>
                                </DataListCell>,
                                <DataListCell
                                  key="date"
                                  width={1}
                                  className="pf-v6-u-text-nowrap"
                                >
                                  {m.date
                                    ? formatDistanceToNow(new Date(m.date), {
                                        addSuffix: true,
                                      })
                                    : ''}
                                </DataListCell>,
                              ]}
                            />
                          </DataListItemRow>
                        </DataListItem>
                      ))}
                  </>
                )}
              </DataList>
            </ErrorsAndWarnings>
          </FlexItem>
        </Flex>
      </CardBody>
      {isModalOpen && (
        <ReconciliationModal
          isOpen={isModalOpen}
          isReconciliationPaused={isReconciliationPaused}
          isPending={isPending}
          onClose={() => setIsModalOpen(false)}
          onConfirm={onConfirmReconciliation}
        />
      )}
    </Card>
  );
}