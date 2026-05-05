/**
 * Dry Run Results Component
 * Displays preview of offset changes before applying them
 */

import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  Button,
  Alert,
  AlertVariant,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from '@patternfly/react-table';
import { OffsetAndMetadata, OffsetResetResult } from '@/api/types';
import { CliCommandDisplay } from './CliCommandDisplay';

interface DryRunResultsProps {
  isOpen: boolean;
  results: OffsetResetResult[];
  currentOffsets?: OffsetAndMetadata[] | null;
  command: string;
  onClose: () => void;
}

export function DryRunResults({
  isOpen,
  results,
  currentOffsets,
  command,
  onClose,
}: DryRunResultsProps) {
  const { t } = useTranslation();

  const currentOffsetsByPartition = (currentOffsets || []).reduce((acc, offset) => {
    acc[`${offset.topicName}-${offset.partition}`] = offset.offset;
    return acc;
  }, {} as Record<string, number | null>);

  // Group results by topic
  const resultsByTopic = results.reduce((acc, result) => {
    const topicName = result.topicName;
    if (!acc[topicName]) {
      acc[topicName] = [];
    }
    acc[topicName].push(result);
    return acc;
  }, {} as Record<string, OffsetResetResult[]>);

  return (
    <Modal
      variant={ModalVariant.medium}
      isOpen={isOpen}
      onClose={onClose}
    >
      <ModalHeader title={t('groups.resetOffset.dryRunResults.title')} />
      <ModalBody>
        <Alert
          variant={AlertVariant.info}
          isInline
          title={t('groups.resetOffset.dryRunResults.description')}
          style={{ marginBottom: '1rem' }}
        />

        {results.length === 0 ? (
          <Alert
            variant={AlertVariant.warning}
            isInline
            title={t('groups.resetOffset.dryRunResults.noResults')}
          />
        ) : (
          Object.entries(resultsByTopic).map(([topicName, topicResults]) => (
            <div key={topicName} style={{ marginBottom: '1.5rem' }}>
              <h3 style={{ marginBottom: '0.5rem' }}>
                {t('groups.resetOffset.dryRunResults.topic')}: {topicName}
              </h3>
              <Table aria-label={`Dry run results for ${topicName}`} variant="compact">
                <Thead>
                  <Tr>
                    <Th>{t('groups.resetOffset.dryRunResults.partition')}</Th>
                    <Th>{t('groups.resetOffset.dryRunResults.currentOffset')}</Th>
                    <Th>{t('groups.resetOffset.dryRunResults.newOffset')}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {topicResults.map((result) => (
                    <Tr key={`${result.topicName}-${result.partition}`}>
                      <Td dataLabel={t('groups.resetOffset.dryRunResults.partition')}>
                        {result.partition}
                      </Td>
                      <Td dataLabel={t('groups.resetOffset.dryRunResults.currentOffset')}>
                        {currentOffsetsByPartition[`${result.topicName}-${result.partition}`] ?? t('common.notAvailable')}
                      </Td>
                      <Td dataLabel={t('groups.resetOffset.dryRunResults.newOffset')}>
                        {result.offset !== null
                          ? result.offset
                          : t('groups.resetOffset.dryRunResults.offsetDeleted')}
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            </div>
          ))
        )}
        <CliCommandDisplay command={command} />
      </ModalBody>
      <ModalFooter>
        <Button
          variant="link"
          onClick={onClose}
        >
          {t('groups.resetOffset.dryRunResults.cancel')}
        </Button>
      </ModalFooter>
    </Modal>
  );
}