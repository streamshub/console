/**
 * Cluster Connection Details Component
 *
 * Displays bootstrap server information for connecting to a Kafka cluster.
 * Shows external and internal listeners with authentication types.
 */

import * as React from 'react';
import { useTranslation } from 'react-i18next';
import {
  Stack,
  StackItem,
  Content,
  List,
  ListItem,
  ClipboardCopy,
  Badge,
  Divider,
  ExpandableSection,
} from '@patternfly/react-core';
import { ExternalLinkAltIcon } from '@patternfly/react-icons';
import { KafkaCluster } from '@/api/types';

export interface ClusterConnectionDetailsProps {
  cluster: KafkaCluster | undefined;
  showLearning?: boolean;
}

export function ClusterConnectionDetails({
  cluster,
  showLearning = false,
}: ClusterConnectionDetailsProps) {
  const { t } = useTranslation();
  const [isExternalExpanded, setIsExternalExpanded] = React.useState(true);
  const [isInternalExpanded, setIsInternalExpanded] = React.useState(true);

  const listeners = cluster?.attributes.listeners || [];
  const external = listeners.filter((l) => l.type !== 'internal');
  const internal = listeners.filter((l) => l.type === 'internal');

  return (
    <Stack hasGutter>
      <StackItem isFilled>
        <Content className="pf-v6-u-p-lg">
          <Content>{t('ClusterConnectionDetails.description')}</Content>

          <ExpandableSection
            displaySize="lg"
            isExpanded={isExternalExpanded}
            onToggle={(_event, isExpanded) => setIsExternalExpanded(isExpanded)}
            toggleContent={
              <div>
                {t('ClusterConnectionDetails.externalServersBootstraps')}{' '}
                <Badge isRead>{external.length}</Badge>
              </div>
            }
          >
            <Content>
              {t('ClusterConnectionDetails.externalServersBootstrapsDescription')}
            </Content>
            <List isPlain>
              {external.map((l, idx) => (
                <ListItem key={idx} className="pf-v6-u-py-sm">
                  <ClipboardCopy isReadOnly>
                    {l.bootstrapServers ?? ''}
                  </ClipboardCopy>
                  <Content component="small">
                    {t('ClusterConnectionDetails.authenticationType')}{' '}
                    {l.authType || 'none'}
                  </Content>
                </ListItem>
              ))}
            </List>
          </ExpandableSection>

          <ExpandableSection
            displaySize="lg"
            isExpanded={isInternalExpanded}
            onToggle={(_event, isExpanded) => setIsInternalExpanded(isExpanded)}
            toggleContent={
              <div>
                {t('ClusterConnectionDetails.internalServersBootstraps')}{' '}
                <Badge isRead>{internal.length}</Badge>
              </div>
            }
            className="pf-v6-u-mt-lg"
          >
            <Content>
              {t('ClusterConnectionDetails.internalServersBootstrapsDescription')}
            </Content>
            <List isPlain>
              {internal.map((l, idx) => (
                <ListItem key={idx} className="pf-v6-u-py-sm">
                  <ClipboardCopy isReadOnly>
                    {l.bootstrapServers ?? ''}
                  </ClipboardCopy>
                  <Content component="small">
                    {t('ClusterConnectionDetails.authenticationType')}{' '}
                    {l.authType || 'none'}
                  </Content>
                </ListItem>
              ))}
            </List>

            <Content>
              {t('ClusterConnectionDetails.whenYouHaveEstablishedConnection')}
            </Content>
          </ExpandableSection>
        </Content>
      </StackItem>
      {showLearning && (
        <StackItem>
          <Divider />
          <Stack hasGutter className="pf-v6-u-p-lg">
            {t('learning.links.connecting') && (
              <StackItem>
                <a
                  href={t('learning.links.connecting')}
                  target="_blank"
                  rel="noopener noreferrer"
                  data-testid="drawer-footer-help-connecting"
                >
                  {t('ClusterConnectionDetails.developingKafkaClientApplications')}{' '}
                  <ExternalLinkAltIcon />
                </a>
              </StackItem>
            )}
            <StackItem>
              <a
                href={t('learning.links.overview')}
                target="_blank"
                rel="noopener noreferrer"
                data-testid="drawer-footer-help-overview"
              >
                {t('learning.labels.overview')} <ExternalLinkAltIcon />
              </a>
            </StackItem>
          </Stack>
        </StackItem>
      )}
    </Stack>
  );
}