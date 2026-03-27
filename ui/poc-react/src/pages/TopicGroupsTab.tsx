/**
 * Topic Groups Tab - Shows groups for a topic
 */

import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMemo } from 'react';
import {
  PageSection,
  EmptyState,
  EmptyStateBody,
  Title,
  Spinner,
  Icon,
  Label,
  LabelGroup,
  Tooltip,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Pagination,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from '@patternfly/react-table';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InfoCircleIcon,
  HelpIcon,
  HistoryIcon,
  OffIcon,
  PendingIcon,
  SyncAltIcon,
  InProgressIcon,
} from '@patternfly/react-icons';
import { useTopicGroups } from '../api/hooks/useGroups';
import { GroupState } from '../api/types';

const StateLabel: Record<GroupState, { label: React.ReactNode }> = {
  STABLE: {
    label: (
      <>
        <Icon status="success">
          <CheckCircleIcon />
        </Icon>
        &nbsp;Stable
      </>
    ),
  },
  EMPTY: {
    label: (
      <>
        <Icon status="info">
          <InfoCircleIcon />
        </Icon>
        &nbsp;Empty
      </>
    ),
  },
  UNKNOWN: {
    label: (
      <>
        <Icon status="warning">
          <ExclamationTriangleIcon />
        </Icon>
        &nbsp;Unknown
      </>
    ),
  },
  PREPARING_REBALANCE: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp;Preparing Rebalance
      </>
    ),
  },
  ASSIGNING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Assigning
      </>
    ),
  },
  DEAD: {
    label: (
      <>
        <Icon>
          <OffIcon />
        </Icon>
        &nbsp;Dead
      </>
    ),
  },
  COMPLETING_REBALANCE: {
    label: (
      <>
        <Icon>
          <SyncAltIcon />
        </Icon>
        &nbsp;Completing Rebalance
      </>
    ),
  },
  RECONCILING: {
    label: (
      <>
        <Icon>
          <InProgressIcon />
        </Icon>
        &nbsp;Reconciling
      </>
    ),
  },
};

function formatNumber(value: number | undefined | null): string {
  if (value === undefined || value === null || isNaN(value)) {
    return 'N/A';
  }
  return value.toLocaleString();
}

function describeEnabled(group: { meta?: { describeAvailable?: boolean } }): boolean {
  return group.meta?.describeAvailable !== false;
}

export function TopicGroupsTab() {
  const { t } = useTranslation();
  const { kafkaId, topicId } = useParams<{ kafkaId: string; topicId: string }>();

  const { data, isLoading, error } = useTopicGroups(kafkaId, topicId, {
    pageSize: 100,
  });

  const groups = data?.data || [];
  const total = data?.meta?.page?.total || 0;

  // Calculate overall lag for each group
  const groupsWithLag = useMemo(() => {
    return groups.map((group) => {
      const overallLag = group.attributes.offsets
        ?.map((o) => o.lag)
        .reduce((acc, v) => (acc ?? NaN) + (v ?? NaN), 0);
      return { ...group, overallLag };
    });
  }, [groups]);

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

  if (error) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  if (groups.length === 0) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('groups.noGroups')}
          </Title>
          <EmptyStateBody>
            No groups are currently consuming from this topic.
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <PageSection isFilled>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={total}
              perPage={100}
              page={1}
              variant="top"
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      <Table aria-label="Groups table" variant="compact">
        <Thead>
          <Tr>
            <Th width={20}>{t('groups.groupId')}</Th>
            <Th width={10}>Type</Th>
            <Th width={10}>Protocol</Th>
            <Th width={15}>
              {t('groups.state')}{' '}
              <Tooltip
                content={
                  <div>
                    <p>Reflects the current operational state of the group.</p>
                    <p>
                      Possible states include 'Stable,' 'Rebalancing,' or 'Empty.' 'Stable'
                      indicates normal functioning, 'Rebalancing' means ongoing adjustments to
                      the group's members, and 'Empty' suggests no active members.
                    </p>
                    <p>If in the 'Empty' state, consider adding members to the group.</p>
                  </div>
                }
              >
                <HelpIcon />
              </Tooltip>
            </Th>
            <Th width={10}>
              Overall lag{' '}
              <Tooltip
                content={
                  <div>
                    <p>The cumulative lag across all partitions assigned to the group.</p>
                    <p>
                      Lag is the difference in the rate of production and consumption of
                      messages.
                    </p>
                    <p>
                      Specifically, lag for a given member in a group indicates the delay
                      between the last message in the partition and the message being currently
                      picked up by the member.
                    </p>
                  </div>
                }
              >
                <HelpIcon />
              </Tooltip>
            </Th>
            <Th width={10}>
              {t('groups.members')}{' '}
              <Tooltip
                content="Represents an individual member within the group. Monitor the lag of each member for insights into the health of the group."
              >
                <HelpIcon />
              </Tooltip>
            </Th>
            <Th width={25}>Topics</Th>
          </Tr>
        </Thead>
        <Tbody>
          {groupsWithLag.map((group) => {
            // Collect all topics from members and offsets
            const allTopics: Record<string, string | undefined> = {};
            group.attributes.members
              ?.flatMap((m) => m.assignments ?? [])
              .forEach((a) => (allTopics[a.topicName] = a.topicId));
            group.attributes.offsets?.forEach(
              (a) => (allTopics[a.topicName] = a.topicId)
            );

            return (
              <Tr key={group.id}>
                <Td dataLabel={t('groups.groupId')}>
                  {describeEnabled(group) && group.meta?.describeAvailable ? (
                    <Link to={`/kafka/${kafkaId}/groups/${group.id}`}>
                      {group.attributes.groupId}
                    </Link>
                  ) : (
                    <>{group.attributes.groupId}</>
                  )}
                </Td>
                <Td dataLabel="Type">{group.attributes.type || '-'}</Td>
                <Td dataLabel="Protocol">{group.attributes.protocol ?? '-'}</Td>
                <Td dataLabel={t('groups.state')}>
                  {StateLabel[group.attributes.state]?.label}
                </Td>
                <Td dataLabel="Overall lag">{formatNumber(group.overallLag)}</Td>
                <Td dataLabel={t('groups.members')}>
                  {formatNumber(group.attributes.members?.length)}
                </Td>
                <Td dataLabel="Topics">
                  <LabelGroup>
                    {Object.entries(allTopics).map(([topicName, topicId]) => (
                      <Label
                        key={topicName}
                        color="blue"
                        isCompact
                        render={({ className, content }) =>
                          topicId ? (
                            <Link
                              to={`/kafka/${kafkaId}/topics/${topicId}`}
                              className={className}
                            >
                              {content}
                            </Link>
                          ) : (
                            <span className={className}>{content}</span>
                          )
                        }
                      >
                        {topicName}
                      </Label>
                    ))}
                  </LabelGroup>
                </Td>
              </Tr>
            );
          })}
        </Tbody>
      </Table>

      <Toolbar>
        <ToolbarContent>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={total}
              perPage={100}
              page={1}
              variant="bottom"
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    </PageSection>
  );
}