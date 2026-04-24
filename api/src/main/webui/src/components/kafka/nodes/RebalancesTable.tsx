/**
 * Rebalances Table Component
 * Displays Kafka rebalances with actions (approve, stop, refresh)
 */

import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { formatDateTime } from '@/utils/dateTime';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ActionsColumn,
  ExpandableRowContent,
  ThProps,
} from '@patternfly/react-table';
import {
  Button,
  Flex,
  FlexItem,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Badge,
  Popover,
  List,
  ListItem,
} from '@patternfly/react-core';
import {
  HelpIcon,
} from '@patternfly/react-icons';
import { Rebalance } from '@/api/types';
import { RebalanceModal } from './RebalanceModal';
import { hasPrivilege } from '@/utils/privileges';
import { StatusLabel } from '@/components/StatusLabel';
import { createRebalanceStatusConfig } from '@/components/StatusLabel/configs';

interface RebalancesTableProps {
  rebalances: Rebalance[] | undefined;
  sortBy: string;
  sortDirection: 'asc' | 'desc';
  onSort: (column: string) => void;
  onApprove: (rebalance: Rebalance) => void;
  onStop: (rebalance: Rebalance) => void;
  onRefresh: (rebalance: Rebalance) => void;
  kafkaId: string;
}

export function RebalancesTable({
  rebalances,
  sortBy,
  sortDirection,
  onSort,
  onApprove,
  onStop,
  onRefresh,
  kafkaId,
}: RebalancesTableProps) {
  const { t } = useTranslation();
  
  // Create status config with i18n translations
  const statusConfig = useMemo(() => createRebalanceStatusConfig(t), [t]);

  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const [selectedRebalance, setSelectedRebalance] = useState<Rebalance | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleRebalanceClick = (rebalance: Rebalance) => {
    setSelectedRebalance(rebalance);
    setIsModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedRebalance(null);
  };

  const toggleRowExpanded = (id: string) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const getSortParams = (columnName: string): ThProps['sort'] => ({
    sortBy: {
      index: sortBy === columnName ? 0 : undefined,
      direction: sortDirection,
    },
    onSort: () => onSort(columnName),
    columnIndex: 0,
  });

  // Get last updated timestamp
  const getLastUpdated = (rebalance: Rebalance): string => {
    const statusCondition = rebalance.attributes.conditions?.find(
      (c) => c.type === rebalance.attributes.status
    );
    return statusCondition?.lastTransitionTime || rebalance.attributes.creationTimestamp || '';
  };

  return (
    <>
      <Table aria-label={t('rebalancing.title')} variant="compact">
        <Thead>
          <Tr>
            <Th />
            <Th width={30} sort={getSortParams('name')}>
              {t('rebalancing.rebalanceName')}
            </Th>
            <Th sort={getSortParams('status')}>{t('rebalancing.status')}</Th>
            <Th sort={getSortParams('lastUpdated')}>{t('rebalancing.lastUpdated')}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {rebalances?.map((rebalance) => {
            const isExpanded = expandedRows.has(rebalance.id);
            const lastUpdated = getLastUpdated(rebalance);

            const canUpdate = hasPrivilege('UPDATE', rebalance);

            return (
              <>
                <Tr key={rebalance.id} style={{ verticalAlign: 'middle' }}>
                  <Td
                    expand={{
                      rowIndex: 0,
                      isExpanded,
                      onToggle: () => toggleRowExpanded(rebalance.id),
                    }}
                  />
                  <Td dataLabel={t('rebalancing.rebalanceName')}>
                    <Button
                      variant="link"
                      isInline
                      onClick={() => handleRebalanceClick(rebalance)}
                    >
                      <Badge>{t('rebalancing.crBadge')}</Badge> {rebalance.attributes.name}
                    </Button>
                  </Td>
                  <Td dataLabel={t('rebalancing.status')}>
                    <StatusLabel
                      status={rebalance.attributes.status || 'New'}
                      config={statusConfig}
                    />
                  </Td>
                  <Td dataLabel={t('rebalancing.lastUpdated')}>
                    {formatDateTime({ value: lastUpdated })}
                  </Td>
                  <Td isActionCell>
                    <ActionsColumn
                      isDisabled={!canUpdate}
                      items={[
                        {
                          title: t('rebalancing.approve'),
                          onClick: () => onApprove(rebalance),
                          isDisabled: !canUpdate || !rebalance.meta?.allowedActions?.includes('approve'),
                        },
                        {
                          title: t('rebalancing.refresh'),
                          onClick: () => onRefresh(rebalance),
                          isDisabled: !canUpdate || !rebalance.meta?.allowedActions?.includes('refresh'),
                        },
                        {
                          title: t('rebalancing.stop'),
                          onClick: () => onStop(rebalance),
                          isDisabled: !canUpdate || !rebalance.meta?.allowedActions?.includes('stop'),
                        },
                      ]}
                    />
                  </Td>
                </Tr>
                {isExpanded && (
                  <Tr isExpanded={isExpanded}>
                    <Td colSpan={5}>
                      <ExpandableRowContent>
                        <DescriptionList className="pf-v6-u-mt-md pf-v6-u-mb-lg">
                          <Flex justifyContent={{ default: 'justifyContentSpaceEvenly' }}>
                            <FlexItem style={{ width: '25%' }}>
                              <DescriptionListGroup>
                                <DescriptionListTerm>{t('rebalancing.autoApprovalEnabled')}</DescriptionListTerm>
                                <DescriptionListDescription>
                                  {rebalance.meta?.autoApproval === true ? 'true' : 'false'}
                                </DescriptionListDescription>
                              </DescriptionListGroup>
                            </FlexItem>
                            <FlexItem style={{ width: '50%', paddingRight: '5rem' }}>
                              <DescriptionListGroup>
                                <DescriptionListTerm>
                                  {t('rebalancing.mode')}{' '}
                                  <Popover
                                    aria-label={t('rebalancing.mode')}
                                    headerContent={<div>{t('rebalancing.rebalanceMode')}</div>}
                                    bodyContent={
                                      <div>
                                        <List>
                                          <ListItem>
                                            <strong>{t('rebalancing.fullMode')}</strong>{' '}
                                            {t('rebalancing.fullModeDescription')}
                                          </ListItem>
                                          <ListItem>
                                            <strong>{t('rebalancing.addBrokersMode')}</strong>{' '}
                                            {t('rebalancing.addBrokersModeDescription')}
                                          </ListItem>
                                          <ListItem>
                                            <strong>{t('rebalancing.removeBrokersMode')}</strong>{' '}
                                            {t('rebalancing.removeBrokersModeDescription')}
                                          </ListItem>
                                        </List>
                                      </div>
                                    }
                                  >
                                    <HelpIcon />
                                  </Popover>
                                </DescriptionListTerm>
                                <DescriptionListDescription>
                                  {rebalance.attributes.mode === 'full' ? (
                                    t('rebalancing.fullMode')
                                  ) : (
                                    <>
                                      {rebalance.attributes.mode === 'add-brokers'
                                        ? t('rebalancing.addBrokersMode')
                                        : t('rebalancing.removeBrokersMode')}{' '}
                                      {rebalance.attributes.brokers?.length
                                        ? rebalance.attributes.brokers.map((b, index) => (
                                            <span key={b}>
                                              <Link to={`/kafka/${kafkaId}/nodes/${b}`}>
                                                {t('rebalancing.broker', { b })}
                                              </Link>
                                              {index < (rebalance.attributes.brokers?.length || 0) - 1 && ', '}
                                            </span>
                                          ))
                                        : ''}
                                    </>
                                  )}
                                </DescriptionListDescription>
                              </DescriptionListGroup>
                            </FlexItem>
                          </Flex>
                        </DescriptionList>
                      </ExpandableRowContent>
                    </Td>
                  </Tr>
                )}
              </>
            );
          })}
        </Tbody>
      </Table>

      <RebalanceModal
        rebalance={selectedRebalance}
        isOpen={isModalOpen}
        onClose={handleModalClose}
      />
    </>
  );
}