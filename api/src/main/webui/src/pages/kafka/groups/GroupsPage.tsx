/**
 * Groups Page - List all groups in a Kafka cluster using ResourceListDataView pattern
 */

import { useCallback, useState } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Alert,
  AlertActionLink,
  AlertActionCloseButton,
  Grid,
  GridItem,
} from '@patternfly/react-core';
import { useGroups } from '@/api/hooks/useGroups';
import { GroupsDataView } from '@/components/kafka/groups/GroupsDataView';
import { ResetOffsetModal } from '@/components/kafka/groups/ResetOffset';
import { Group } from '@/api/types';
import { useShowLearning } from '@/hooks/useShowLearning';
import { ResourceListParams } from '@/api/hooks/useResourceList';

export function GroupsPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const showLearning = useShowLearning();

  const [dataParams, setDataParams] = useState<ResourceListParams>({});

  // Alert state
  const [isAlertVisible, setIsAlertVisible] = useState(true);

  // Reset offset modal state
  const [isResetOffsetModalOpen, setIsResetOffsetModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [resetOffsetSuccessMessage, setResetOffsetSuccessMessage] = useState<string>();

  const groupsResult = useGroups(kafkaId, dataParams);

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams(params);
  }, []);

  const handleResetOffset = (group: Group) => {
    setSelectedGroup(group);
    setIsResetOffsetModalOpen(true);
  };

  const handleCloseResetOffsetModal = () => {
    setIsResetOffsetModalOpen(false);
    setSelectedGroup(null);
  };

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl" ouiaId={"title"}>
          {t('groups.title')}
        </Title>
      </PageSection>
      <PageSection>
        <Grid hasGutter>
          {resetOffsetSuccessMessage && (
            <Alert
              variant="success"
              isInline
              title={resetOffsetSuccessMessage}
              actionClose={<AlertActionCloseButton onClose={() => setResetOffsetSuccessMessage(undefined)} />}
              style={{ marginBottom: '1rem' }}
            />
          )}
          {showLearning && isAlertVisible && (
            <GridItem>
              <Alert
                variant="info"
                isInline
                title={t('groups.alert')}
                actionClose={
                  <AlertActionCloseButton onClose={() => setIsAlertVisible(false)} />
                }
                actionLinks={
                  <AlertActionLink
                    component="a"
                    href={t('groups.learnMoreLink')}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {t('groups.learnMore')}
                  </AlertActionLink>
                }
              />
            </GridItem>
          )}
          <GridItem>
            <GroupsDataView
              groupsResult={groupsResult}
              onDataViewChange={handleDataViewChange}
              onResetOffset={handleResetOffset}
            />
          </GridItem>
        </Grid>
      </PageSection>

      {selectedGroup && (
        <ResetOffsetModal
          isOpen={isResetOffsetModalOpen}
          onClose={handleCloseResetOffsetModal}
          onSuccess={(message) => setResetOffsetSuccessMessage(message)}
          kafkaId={kafkaId!}
          group={selectedGroup}
        />
      )}
    </>
  );
}
