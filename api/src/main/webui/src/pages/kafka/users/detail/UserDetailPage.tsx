/**
 * User Detail Page - Display details for a single Kafka user
 */

import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  EmptyState,
  EmptyStateBody,
  Spinner,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  Tab,
  TabContentBody,
  Tabs,
  TabTitleText,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  TableVariant,
} from '@patternfly/react-table';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import { useUser } from '@/api/hooks/useUsers';
import { formatDateTime } from '@/utils/dateTime';

export function UserDetailPage() {
  const { t } = useTranslation();
  const { kafkaId, userId } = useParams<{ kafkaId: string; userId: string }>();
  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);

  const { data, isLoading, error } = useUser(kafkaId, userId);

  const handleTabClick = (
    _event: React.MouseEvent<unknown> | React.KeyboardEvent,
    tabIndex: string | number
  ) => {
    setActiveTabKey(tabIndex);
  };

  if (isLoading) {
    return (
      <PageSection>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h1" size="lg">
            {t('common.loading')}
          </Title>
          <EmptyStateBody>{t('users.loading')}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection>
        <EmptyState>
          <ExclamationTriangleIcon color="var(--pf-v5-global--danger-color--100)" />
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>
            {error instanceof Error ? error.message : t('users.errorLoading')}
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  if (!data?.data) {
    return (
      <PageSection>
        <EmptyState>
          <Title headingLevel="h1" size="lg">
            {t('users.details.title')}
          </Title>
          <EmptyStateBody>No user data available</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  const user = data.data;
  const {
    name,
    namespace,
    creationTimestamp,
    username,
    authenticationType,
    authorization,
  } = user.attributes;
  const authorizationData = authorization?.accessControls ?? [];
  
  // Use username for display (not the resource name or userId)
  const displayName = username || '';

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {displayName}
        </Title>
      </PageSection>
      <PageSection>
        <Flex direction={{ default: 'column' }} gap={{ default: 'gap2xl' }}>
          <FlexItem>
            <DescriptionList isHorizontal columnModifier={{ default: '2Col' }}>
              <DescriptionListGroup>
                <DescriptionListTerm>{t('users.details.name')}</DescriptionListTerm>
                <DescriptionListDescription>{name}</DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>{t('users.details.username')}</DescriptionListTerm>
                <DescriptionListDescription>{username}</DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>{t('users.details.authentication')}</DescriptionListTerm>
                <DescriptionListDescription>
                  {authenticationType}
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>{t('users.details.namespace')}</DescriptionListTerm>
                <DescriptionListDescription>
                  {namespace ?? '-'}
                </DescriptionListDescription>
              </DescriptionListGroup>
              <DescriptionListGroup>
                <DescriptionListTerm>{t('users.details.creationTime')}</DescriptionListTerm>
                <DescriptionListDescription>
                  {creationTimestamp ? formatDateTime({ value: creationTimestamp }) : '-'}
                </DescriptionListDescription>
              </DescriptionListGroup>
            </DescriptionList>
          </FlexItem>

          <FlexItem>
            <Tabs
              activeKey={activeTabKey}
              onSelect={handleTabClick}
              aria-label="Kafka User Details Tabs"
              role="region"
            >
              <Tab
                eventKey={0}
                title={<TabTitleText>{t('users.details.authorization')}</TabTitleText>}
                aria-label={t('users.details.authorization')}
              >
                <TabContentBody>
                  {authorizationData.length === 0 ? (
                    <EmptyState>
                      <Title headingLevel="h2" size="lg">
                        {t('users.details.noAuthorization')}
                      </Title>
                    </EmptyState>
                  ) : (
                    <Table
                      aria-label={t('users.details.authorization')}
                      variant={TableVariant.compact}
                    >
                      <Thead>
                        <Tr>
                          <Th>{t('users.details.authColumns.type')}</Th>
                          <Th>{t('users.details.authColumns.resourceName')}</Th>
                          <Th>{t('users.details.authColumns.patternType')}</Th>
                          <Th>{t('users.details.authColumns.host')}</Th>
                          <Th>{t('users.details.authColumns.operations')}</Th>
                          <Th>{t('users.details.authColumns.permissionType')}</Th>
                        </Tr>
                      </Thead>
                      <Tbody>
                        {authorizationData.map((rule, index) => (
                          <Tr key={index}>
                            <Td dataLabel={t('users.details.authColumns.type')}>
                              {rule.type}
                            </Td>
                            <Td dataLabel={t('users.details.authColumns.resourceName')}>
                              {rule.resourceName ?? '-'}
                            </Td>
                            <Td dataLabel={t('users.details.authColumns.patternType')}>
                              {rule.patternType ?? '-'}
                            </Td>
                            <Td dataLabel={t('users.details.authColumns.host')}>
                              {rule.host ?? '-'}
                            </Td>
                            <Td dataLabel={t('users.details.authColumns.operations')}>
                              {rule.operations?.join(', ')}
                            </Td>
                            <Td dataLabel={t('users.details.authColumns.permissionType')}>
                              {rule.permissionType}
                            </Td>
                          </Tr>
                        ))}
                      </Tbody>
                    </Table>
                  )}
                </TabContentBody>
              </Tab>
            </Tabs>
          </FlexItem>
        </Flex>
      </PageSection>
    </>
  );
}