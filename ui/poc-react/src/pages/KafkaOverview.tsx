/**
 * Kafka Overview Page - Cluster dashboard
 */

import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Card,
  CardBody,
} from '@patternfly/react-core';
import { useKafkaCluster } from '../api/hooks/useKafkaClusters';

export function KafkaOverview() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const { data } = useKafkaCluster(kafkaId);

  const cluster = data?.data;

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          {cluster?.attributes.name || t('kafka.overview')}
        </Title>
      </PageSection>
      <PageSection>
        <Card>
          <CardBody>
            <Title headingLevel="h2" size="lg">
              Cluster Information
            </Title>
            {cluster && (
              <dl>
                <dt>Name:</dt>
                <dd>{cluster.attributes.name}</dd>
                {cluster.attributes.namespace && (
                  <>
                    <dt>Namespace:</dt>
                    <dd>{cluster.attributes.namespace}</dd>
                  </>
                )}
                {cluster.attributes.creationTimestamp && (
                  <>
                    <dt>Created:</dt>
                    <dd>{new Date(cluster.attributes.creationTimestamp).toLocaleString()}</dd>
                  </>
                )}
              </dl>
            )}
          </CardBody>
        </Card>
      </PageSection>
    </>
  );
}