/**
 * Group Detail Page - Shows details of a specific consumer group with tabs
 *
 * Split in two:
 * - GroupDetailPage      -> router params, data fetching, privileges
 * - GroupDetailPageView  -> pure UI, takes everything as props (used by Storybook)
 */

import { useState } from 'react';
import { useParams, useNavigate, useLocation, Outlet } from 'react-router';
import { useTranslation } from 'react-i18next';
import { usePageTitle } from '@/hooks';
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
  Alert,
  AlertActionCloseButton,
} from '@patternfly/react-core';
import { useGroup } from '@/api/hooks/useGroups';
import { ResetOffsetModal } from '@/components/kafka/groups/ResetOffset';
import { hasPrivilege } from '@/utils/privileges';
import type { Group } from '@/api/types';

/* -------------------------------------------------------------------------- */
/* Container: router + data fetching                                           */
/* -------------------------------------------------------------------------- */

export function GroupDetailPage() {
  const { kafkaId, groupId } = useParams<{
    kafkaId: string;
    groupId: string;
  }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { data: group, isLoading, error } = useGroup(kafkaId, groupId);

  const groupName = group?.attributes.groupId || groupId || '';
  usePageTitle(groupName || undefined);

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/').filter(Boolean);
  const currentTab = pathSegments[pathSegments.length - 1];

  // Default to 'members' if we're at the group root
  const activeTab = currentTab === groupId ? 'members' : currentTab;

  const handleTabSelect = (tabKey: string | number) => {
    navigate(`/kafka/${kafkaId}/groups/${groupId}/${tabKey}`);
  };

  const canResetOffset =
    hasPrivilege('UPDATE', group) &&
    group?.attributes.state === 'EMPTY' &&
    group?.attributes.protocol === 'consumer';

  return (
    <GroupDetailPageView
      kafkaId={kafkaId!}
      groupName={groupName}
      group={group}
      isLoading={isLoading}
      error={error}
      canResetOffset={!!canResetOffset}
      activeTab={activeTab}
      onTabSelect={handleTabSelect}
    >
      <Outlet context={{ group }} />
    </GroupDetailPageView>
  );
}

/* -------------------------------------------------------------------------- */
/* View: pure UI                                                               */
/* -------------------------------------------------------------------------- */

export interface GroupDetailPageViewProps {
  kafkaId: string;
  groupName: string;
  group: Group | undefined;
  isLoading: boolean;
  error: Error | null;
  canResetOffset: boolean;
  activeTab: string;
  onTabSelect: (tabKey: string | number) => void;
  /** Content of the selected tab (the router <Outlet /> in the real page) */
  children?: React.ReactNode;
}

export function GroupDetailPageView({
  kafkaId,
  groupName,
  group,
  isLoading,
  error,
  canResetOffset,
  activeTab,
  onTabSelect,
  children,
}: GroupDetailPageViewProps) {
  const { t } = useTranslation();

  // Reset offset modal state
  const [isResetOffsetModalOpen, setIsResetOffsetModalOpen] = useState(false);
  const [resetOffsetSuccessMessage, setResetOffsetSuccessMessage] =
    useState<string>();

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

  return (
    <>
      <PageSection>
        <Flex
          justifyContent={{ default: 'justifyContentSpaceBetween' }}
          alignItems={{ default: 'alignItemsCenter' }}
        >
          <FlexItem>
            <Title headingLevel="h1" size="2xl">
              {groupName}
            </Title>
          </FlexItem>
          {group && (
            <FlexItem>
              <Button
                variant="primary"
                onClick={() => setIsResetOffsetModalOpen(true)}
                isDisabled={!canResetOffset}
              >
                {t('groups.resetOffsetAction')}
              </Button>
            </FlexItem>
          )}
        </Flex>
      </PageSection>
      {resetOffsetSuccessMessage && (
        <PageSection>
          <Alert
            variant="success"
            isInline
            title={resetOffsetSuccessMessage}
            actionClose={
              <AlertActionCloseButton
                onClose={() => setResetOffsetSuccessMessage(undefined)}
              />
            }
          />
        </PageSection>
      )}

      <PageSection>
        <Tabs
          activeKey={activeTab}
          onSelect={(_event, tabKey) => onTabSelect(tabKey)}
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
            title={
              <TabTitleText>{t('topics.tabs.configuration')}</TabTitleText>
            }
            aria-label={t('topics.tabs.configuration')}
          />
        </Tabs>
      </PageSection>
      {children}

      {group && (
        <ResetOffsetModal
          isOpen={isResetOffsetModalOpen}
          onClose={() => setIsResetOffsetModalOpen(false)}
          onSuccess={(message) => setResetOffsetSuccessMessage(message)}
          kafkaId={kafkaId}
          group={group}
        />
      )}
    </>
  );
}
