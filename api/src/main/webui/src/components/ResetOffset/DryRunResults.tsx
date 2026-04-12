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
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from '@patternfly/react-table';
import { OffsetResetResult } from '../../api/types';

interface DryRunResultsProps {
  isOpen: boolean;
  results: OffsetResetResult[];
  onClose: () => void;
  onApply: () => void;
  isApplying?: boolean;
}

export function DryRunResults({
  isOpen,
  results,
  onClose,
  onApply,
  isApplying = false,
}: DryRunResultsProps) {
  const { t } = useTranslation();

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
      variant={ModalVariant.large}
      title={t('groups.resetOffset.dryRunResults.title')}
      isOpen={isOpen}
      onClose={onClose}
    >
      <div style={{ padding: '1.5rem' }}>
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
                    <Th>{t('groups.resetOffset.dryRunResults.newOffset')}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {topicResults.map((result) => (
                    <Tr key={`${result.topicName}-${result.partition}`}>
                      <Td dataLabel={t('groups.resetOffset.dryRunResults.partition')}>
                        {result.partition}
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
      </div>
      <div style={{ padding: '1.5rem', paddingTop: '0', display: 'flex', gap: '0.5rem' }}>
        <Button
          variant="primary"
          onClick={onApply}
          isDisabled={isApplying || results.length === 0}
          isLoading={isApplying}
        >
          {t('groups.resetOffset.dryRunResults.apply')}
        </Button>
        <Button
          variant="link"
          onClick={onClose}
          isDisabled={isApplying}
        >
          {t('groups.resetOffset.dryRunResults.cancel')}
        </Button>
      </div>
    </Modal>
  );
}