/**
 * Users Page - List all Kafka users in a cluster
 */

import { useCallback, useState } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { PageSection, Title } from '@patternfly/react-core';
import { useUsers } from '@/api/hooks/useUsers';
import { UsersDataView } from '@/components/kafka/users/UsersDataView';
import { ResourceListParams } from '@/api/hooks/useResourceList';

export function UsersPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const [dataParams, setDataParams] = useState<ResourceListParams>({});

  const usersResult = useUsers(kafkaId, dataParams);

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams(params);
  }, []);

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {t('users.title')}
        </Title>
      </PageSection>
      <PageSection>
        <UsersDataView
          usersResult={usersResult}
          onDataViewChange={handleDataViewChange}
        />
      </PageSection>
    </>
  );
}
