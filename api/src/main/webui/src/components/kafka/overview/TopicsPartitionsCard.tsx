/**
 * Topics Partitions Card Component
 *
 * Displays topic and partition statistics:
 * - Total topics count
 * - Total partitions count
 * - Status breakdown (fully replicated, under-replicated, offline)
 */

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Content,
  Divider,
  Flex,
  FlexItem,
  Icon,
  Skeleton,
  Tooltip,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
} from '@patternfly/react-icons';

export interface TopicsPartitionsCardProps {
  totalTopics: number | undefined;
  totalPartitions: number | undefined;
  fullyReplicated: number | undefined;
  underReplicated: number | undefined;
  offline: number | undefined;
  isLoading: boolean;
}

export function TopicsPartitionsCard({
  totalTopics,
  totalPartitions,
  fullyReplicated,
  underReplicated,
  offline,
  isLoading,
}: TopicsPartitionsCardProps) {
  const { t } = useTranslation();

  return (
    <Card component="div">
      <CardHeader
        actions={{
          actions: (
            <Link to="../topics" style={{ textDecoration: 'none' }}>
              {t('ClusterOverview.view_all_topics')}
            </Link>
          ),
        }}
      >
        <CardTitle>{t('ClusterOverview.topic_header')}</CardTitle>
      </CardHeader>
      <CardBody>
        <Flex gap={{ default: 'gapLg' }}>
          {/* Left side: Topics and Partitions counts */}
          <Flex
            flex={{ default: 'flex_1' }}
            direction={{ default: 'column' }}
            alignSelf={{ default: 'alignSelfCenter' }}
          >
            <FlexItem>
              {isLoading ? (
                <Skeleton />
              ) : (
                <Flex gap={{ default: 'gapMd' }}>
                  <FlexItem>
                    <Content>
                      <Content component="small">
                        <Link to="../topics" style={{ textDecoration: 'none' }}>
                          {totalTopics ?? 0} {t('ClusterOverview.total_topics')}
                        </Link>
                      </Content>
                    </Content>
                  </FlexItem>
                  <Divider orientation={{ default: 'vertical' }} />
                  <FlexItem>
                    <Content>
                      <Content component="small">
                        {totalPartitions ?? 0} {t('ClusterOverview.total_partitions')}
                      </Content>
                    </Content>
                  </FlexItem>
                </Flex>
              )}
            </FlexItem>
          </Flex>

          <Divider />

          {/* Right side: Status breakdown */}
          <Flex
            flex={{ default: 'flex_1' }}
            style={{ textAlign: 'center' }}
            justifyContent={{ default: 'justifyContentSpaceAround' }}
            direction={{ default: 'column', '2xl': 'row' }}
            flexWrap={{ default: 'nowrap' }}
          >
            {/* Fully Replicated */}
            <FlexItem>
              <Link to="../topics?status=FullyReplicated" style={{ textDecoration: 'none' }}>
                {isLoading ? (
                  <Skeleton shape="circle" width="1rem" style={{ display: 'inline-block' }} />
                ) : (
                  fullyReplicated ?? 0
                )}
              </Link>
              <Content className="pf-v6-u-text-nowrap">
                <Content component="small">
                  <Icon status="success">
                    <CheckCircleIcon />
                  </Icon>
                  &nbsp;{t('ClusterOverview.fully_replicated_partition')}&nbsp;
                  <Tooltip content={t('ClusterOverview.fully_replicated_partition_tooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </Content>
              </Content>
            </FlexItem>

            {/* Under-replicated */}
            <FlexItem>
              <Link to="../topics?status=UnderReplicated" style={{ textDecoration: 'none' }}>
                {isLoading ? (
                  <Skeleton shape="circle" width="1rem" style={{ display: 'inline-block' }} />
                ) : (
                  underReplicated ?? 0
                )}
              </Link>
              <Content className="pf-v6-u-text-nowrap">
                <Content component="small">
                  <Icon status="warning">
                    <ExclamationTriangleIcon />
                  </Icon>
                  &nbsp;{t('ClusterOverview.under_replicated_partition')}&nbsp;
                  <Tooltip content={t('ClusterOverview.under_replicated_partition_tooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </Content>
              </Content>
            </FlexItem>

            {/* Offline */}
            <FlexItem>
              <Link to="../topics?status=Offline" style={{ textDecoration: 'none' }}>
                {isLoading ? (
                  <Skeleton shape="circle" width="1rem" style={{ display: 'inline-block' }} />
                ) : (
                  offline ?? 0
                )}
              </Link>
              <Content className="pf-v6-u-text-nowrap">
                <Content component="small">
                  <Icon status="danger">
                    <ExclamationCircleIcon />
                  </Icon>
                  &nbsp;{t('ClusterOverview.unavailable_partition')}&nbsp;
                  <Tooltip content={t('ClusterOverview.unavailable_partition_tooltip')}>
                    <HelpIcon />
                  </Tooltip>
                </Content>
              </Content>
            </FlexItem>
          </Flex>
        </Flex>
      </CardBody>
    </Card>
  );
}