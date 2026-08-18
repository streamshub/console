/**
 * Connect Connectors Tab
 * 
 * Displays the connectors table using ResourceListDataView pattern
 */

import { useCallback, useState } from 'react';
import { useParams } from 'react-router';
import { PageSection } from '@patternfly/react-core';
import { useConnectors } from '@/api/hooks/useConnect';
import { ConnectorsDataView } from '@/components/kafka/connect/ConnectorsDataView';
import { ResourceListParams } from '@/api/hooks/useResourceList';

export function ConnectConnectorsTab() {
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const [dataParams, setDataParams] = useState<ResourceListParams>({});

  const connectorsResult = useConnectors(kafkaId, dataParams);

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams(params);
  }, []);

  return (
    <PageSection isFilled>
      <ConnectorsDataView
        connectorsResult={connectorsResult}
        onDataViewChange={handleDataViewChange}
      />
    </PageSection>
  );
}
