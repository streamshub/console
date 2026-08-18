/**
 * Connect Clusters Tab
 * 
 * Displays the connect clusters table using ResourceListDataView pattern
 */

import { useCallback, useState } from 'react';
import { useParams } from 'react-router';
import { PageSection } from '@patternfly/react-core';
import { useConnectClusters } from '@/api/hooks/useConnect';
import { ConnectClustersDataView } from '@/components/kafka/connect/ConnectClustersDataView';
import { ResourceListParams } from '@/api/hooks/useResourceList';

export function ConnectClustersTab() {
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const [dataParams, setDataParams] = useState<ResourceListParams>({});

  const connectClustersResult = useConnectClusters(kafkaId, dataParams);

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams(params);
  }, []);

  return (
    <PageSection isFilled>
      <ConnectClustersDataView
        connectClustersResult={connectClustersResult}
        onDataViewChange={handleDataViewChange}
      />
    </PageSection>
  );
}
