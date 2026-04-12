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

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  Flex,
  FlexItem,
  Title,
  Icon,
  Content,
  Divider,
  Grid,
  GridItem,
  Skeleton,
  Tooltip,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  HelpIcon,
} from '@patternfly/react-icons';
import { KafkaCluster } from '../../api/types';

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

  const status = cluster?.attributes.status ?? 
    (brokersOnline === brokersTotal ? 'Ready' : 'Not Available');

  const isManaged = cluster?.meta?.kind === 'kafkas.kafka.strimzi.io';

  return (
    <Card component="div">
      <CardBody>
        <Flex>
          <FlexItem>
            <Title headingLevel="h2">
              {t('ClusterCard.Kafka_cluster_details')}
            </Title>
          </FlexItem>
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
        </Flex>
      </CardBody>
    </Card>
  );
}