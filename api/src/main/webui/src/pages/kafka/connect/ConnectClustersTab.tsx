/**
 * Connect Clusters Tab
 * 
 * Displays the connect clusters table
 */

import { useParams } from 'react-router-dom';
import { PageSection } from '@patternfly/react-core';
import { ConnectClustersTable } from '@/components/kafka/connect/ConnectClustersTable';

export function ConnectClustersTab() {
  const { kafkaId } = useParams<{ kafkaId: string }>();

  return (
    <PageSection isFilled>
      <ConnectClustersTable kafkaId={kafkaId} />
    </PageSection>
  );
}