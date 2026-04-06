/**
 * Lag Table Component - Shows partition lag details for a group member
 */

import { useTranslation } from 'react-i18next';
import { useParams, Link } from 'react-router-dom';
import {
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
} from '@patternfly/react-table';
import { HelpIcon, InfoCircleIcon } from '@patternfly/react-icons';
import { OffsetAndMetadata } from '../api/types';

interface LagTableProps {
  offsets: OffsetAndMetadata[];
}

export function LagTable({ offsets }: LagTableProps) {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  const formatNumber = (value: number | undefined | null): string => {
    if (value === undefined || value === null || isNaN(value)) {
      return t('common.notAvailable');
    }
    return value.toLocaleString();
  };

  if (!offsets || offsets.length === 0) {
    return (
      <EmptyState>
        <InfoCircleIcon />
        <Title headingLevel="h4" size="md">
          No topics found
        </Title>
        <EmptyStateBody>
          No offsets are available for this member.
        </EmptyStateBody>
      </EmptyState>
    );
  }

  return (
    <Table aria-label="Group lag" variant="compact">
      <Thead>
        <Tr>
          <Th width={30}>Topic</Th>
          <Th width={15} modifier="fitContent" style={{ textAlign: 'right' }}>
            Partition
          </Th>
          <Th width={15} modifier="fitContent" style={{ textAlign: 'right' }}>
            Lag
          </Th>
          <Th width={20} modifier="fitContent" style={{ textAlign: 'right' }}>
            Committed offset{' '}
            <Tooltip content="The offset of the last message that the consumer group has successfully processed and committed.">
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={20} modifier="fitContent" style={{ textAlign: 'right' }}>
            End offset{' '}
            <Tooltip content="The offset of the last message in the partition.">
              <HelpIcon />
            </Tooltip>
          </Th>
        </Tr>
      </Thead>
      <Tbody>
        {offsets.map((offset, index) => (
          <Tr key={`${offset.topicName}-${offset.partition}-${index}`}>
            <Td dataLabel="Topic">
              {offset.topicId ? (
                <Link to={`/kafka/${kafkaId}/topics/${offset.topicId}`}>
                  {offset.topicName}
                </Link>
              ) : (
                offset.topicName
              )}
            </Td>
            <Td dataLabel="Partition" modifier="fitContent" style={{ textAlign: 'right' }}>
              {formatNumber(offset.partition)}
            </Td>
            <Td dataLabel="Lag" modifier="fitContent" style={{ textAlign: 'right' }}>
              {formatNumber(offset.lag)}
            </Td>
            <Td dataLabel="Committed offset" modifier="fitContent" style={{ textAlign: 'right' }}>
              {formatNumber(offset.offset)}
            </Td>
            <Td dataLabel="End offset" modifier="fitContent" style={{ textAlign: 'right' }}>
              {formatNumber(offset.logEndOffset)}
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  );
}