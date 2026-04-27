/**
 * Group Members Tab - Shows members of a consumer group
 */

import { useOutletContext } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  EmptyState,
  EmptyStateBody,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ExpandableRowContent,
} from '@patternfly/react-table';
import { HelpIcon, InfoCircleIcon } from '@patternfly/react-icons';
import { Group, MemberDescription, OffsetAndMetadata } from '@/api/types';
import { useState } from 'react';
import { LagTable } from '@/components/kafka/groups/LagTable';

interface GroupOutletContext {
  group: Group;
}

export function GroupMembersTab() {
  const { t } = useTranslation();
  const { group } = useOutletContext<GroupOutletContext>();
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const formatNumber = (value: number | undefined | null): string => {
    if (value === undefined || value === null || isNaN(value)) {
      return t('common.notAvailable');
    }
    return value.toLocaleString();
  };

  // Handle empty group - show offsets with unknown member
  const members: MemberDescription[] =
    group.attributes.members?.length === 0 && group.attributes.offsets
      ? [
          {
            memberId: 'unknown',
            host: 'N/A',
            clientId: 'unknown',
            assignments: group.attributes.offsets.map((o) => ({
              topicId: o.topicId,
              topicName: o.topicName,
              partition: o.partition,
            })),
          },
        ]
      : (group.attributes.members ?? []);

  const toggleRow = (memberId: string) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(memberId)) {
        newSet.delete(memberId);
      } else {
        newSet.add(memberId);
      }
      return newSet;
    });
  };

  const getOffsetsForMember = (member: MemberDescription): OffsetAndMetadata[] => {
    const partitions = member.assignments?.map((a) => ({
      topicName: a.topicName,
      partition: a.partition,
    })) ?? [];

    const offsets: OffsetAndMetadata[] = group.attributes.offsets
      ?.filter((o) =>
        partitions.some((p) => p.topicName === o.topicName && p.partition === o.partition)
      )
      .map((o) => ({
        ...o,
      })) ?? [];

    offsets.sort((a, b) => a.topicName.localeCompare(b.topicName));
    return offsets;
  };

  const calculateMemberLag = (member: MemberDescription): number | null => {
    const offsets = getOffsetsForMember(member);
    const totalLag = offsets.reduce((acc, offset) => {
      const lag = offset.lag ?? NaN;
      return (acc ?? 0) + (isNaN(lag) ? 0 : lag);
    }, 0 as number | null);
    return totalLag;
  };

  if (members.length === 0) {
    return (
      <PageSection>
        <EmptyState>
          <InfoCircleIcon />
          <Title headingLevel="h2" size="lg">
            {t('groups.noGroups')}
          </Title>
          <EmptyStateBody>
            This group has no members.
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <PageSection>
      <Table aria-label="Group members" variant="compact">
      <Thead>
        <Tr>
          <Th />
          <Th width={30}>
            Member ID
          </Th>
          <Th width={20}>
            Client ID{' '}
            <Tooltip content="The client ID is a user-specified string sent in each request to help trace calls.">
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={15} modifier="fitContent" style={{ textAlign: 'right' }}>
            Overall lag{' '}
            <Tooltip content="The cumulative lag across all partitions assigned to this member. Lag indicates the delay between the last message in the partition and the message being currently picked up by the member.">
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={15} modifier="fitContent" style={{ textAlign: 'right' }}>
            Assigned partitions
          </Th>
        </Tr>
      </Thead>
      {members.map((member, rowIndex) => {
        const isExpanded = expandedRows.has(member.memberId);
        const offsets = getOffsetsForMember(member);
        const lag = calculateMemberLag(member);

        return (
          <Tbody key={member.memberId} isExpanded={isExpanded}>
            <Tr>
              <Td
                expand={{
                  rowIndex,
                  isExpanded,
                  onToggle: () => toggleRow(member.memberId),
                }}
              />
              <Td dataLabel="Member ID">{member.memberId}</Td>
              <Td dataLabel="Client ID">{member.clientId}</Td>
              <Td dataLabel="Overall lag" modifier="fitContent" style={{ textAlign: 'right' }}>
                {formatNumber(lag)}
              </Td>
              <Td dataLabel="Assigned partitions" modifier="fitContent" style={{ textAlign: 'right' }}>
                {formatNumber(member.assignments?.length)}
              </Td>
            </Tr>
            <Tr isExpanded={isExpanded}>
              <Td colSpan={5}>
                <ExpandableRowContent>
                  <div style={{ padding: '1rem' }}>
                    <LagTable offsets={offsets} />
                  </div>
                </ExpandableRowContent>
              </Td>
            </Tr>
          </Tbody>
        );
      })}
      </Table>
    </PageSection>
  );
}