/**
 * Group Detail Page - Shows details of a specific consumer group with tabs
 */

import { useState } from 'react';
import { useParams, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Tabs,
  Tab,
  TabTitleText,
  EmptyState,
  EmptyStateBody,
  Spinner,
  Button,
  Flex,
  FlexItem,
} from '@patternfly/react-core';
import { useGroup } from '@/api/hooks/useGroups';
import { ResetOffsetModal } from '@/components/kafka/groups/ResetOffset';
import { hasPrivilege } from '@/utils/privileges';

export function GroupDetailPage() {
  const { t } = useTranslation();
  const { kafkaId, groupId } = useParams<{ kafkaId: string; groupId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { data: group, isLoading, error } = useGroup(kafkaId, groupId);

  // Reset offset modal state
  const [isResetOffsetModalOpen, setIsResetOffsetModalOpen] = useState(false);

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const currentTab = pathSegments[pathSegments.length - 1];
  
  // Default to 'members' if we're at the group root
  const activeTab = currentTab === groupId ? 'members' : currentTab;

  const handleTabSelect = (_event: React.MouseEvent, tabKey: string | number) => {
    navigate(`/kafka/${kafkaId}/groups/${groupId}/${tabKey}`);
  };

  if (isLoading) {
    return (
      <PageSection>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h1" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection>
        <EmptyState>
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  const groupName = group?.attributes.groupId || groupId || '';
  const canResetOffset =
    hasPrivilege('UPDATE', group) &&
    group?.attributes.state === 'EMPTY' &&
    group?.attributes.protocol === 'consumer';

  const handleResetOffset = () => {
    setIsResetOffsetModalOpen(true);
  };

  const handleCloseResetOffsetModal = () => {
    setIsResetOffsetModalOpen(false);
  };

  return (
    <>
      <PageSection>
        <Flex justifyContent={{ default: 'justifyContentSpaceBetween' }} alignItems={{ default: 'alignItemsCenter' }}>
          <FlexItem>
            <Title headingLevel="h1" size="2xl">
              {groupName}
            </Title>
          </FlexItem>
          {group && (
            <FlexItem>
              <Button
                variant="secondary"
                onClick={handleResetOffset}
                isDisabled={!canResetOffset}
              >
                {t('groups.resetOffsetAction')}
              </Button>
            </FlexItem>
          )}
        </Flex>
      </PageSection>
      <PageSection>
        <Tabs
          activeKey={activeTab}
          onSelect={handleTabSelect}
          aria-label="Group detail tabs"
          role="region"
        >
          <Tab
            eventKey="members"
            title={<TabTitleText>{t('groups.members')}</TabTitleText>}
            aria-label={t('groups.members')}
          />
          <Tab
            eventKey="configuration"
            title={<TabTitleText>{t('topics.tabs.configuration')}</TabTitleText>}
            aria-label={t('topics.tabs.configuration')}
          />
        </Tabs>
      </PageSection>
      <Outlet context={{ group }} />

      {group && (
        <ResetOffsetModal
          isOpen={isResetOffsetModalOpen}
          onClose={handleCloseResetOffsetModal}
          kafkaId={kafkaId!}
          group={group}
        />
      )}
    </>
  );
}