/**
 * Connect Connectors Tab
 * 
 * Displays the connectors table
 */

import { useParams } from 'react-router-dom';
import { PageSection } from '@patternfly/react-core';
import { ConnectorsTable } from '@/components/kafka/connect/ConnectorsTable';

export function ConnectConnectorsTab() {
  const { kafkaId } = useParams<{ kafkaId: string }>();

  return (
    <PageSection isFilled>
      <ConnectorsTable kafkaId={kafkaId} />
    </PageSection>
  );
}