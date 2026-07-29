/**
 * Home Page - Kafka Cluster Selection
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Skeleton,
  Card,
  CardTitle,
  CardBody,
  CardHeader,
  CardExpandableContent,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListItemCells,
  DataListCell,
  Label,
  Content,
  Divider,
  Flex,
  FlexItem,
} from '@patternfly/react-core';
import { ExternalLinkAltIcon } from '@patternfly/react-icons';
import { useKafkaClusters } from '../api/hooks/useKafkaClusters';
import { useMetadata } from '../api/hooks/useMetadata';
import { useShowLearning } from '../hooks/useShowLearning';
import { usePageTitle } from '@/hooks';
import { AppLayout } from '@/components/app/AppLayout';
import { ClustersDataView } from '@/components/home/ClustersDataView';
import { ResourceListParams } from '@/api/hooks/useResourceList';

export function HomePage() {
  const { t } = useTranslation();
  usePageTitle();
  const [dataParams, setDataParams] = useState<ResourceListParams>({});
  const clusterResult = useKafkaClusters(dataParams);
  const { data: metadata } = useMetadata();

  const totalCount = clusterResult.data?.meta?.page?.total;
  const platform = metadata?.data?.attributes?.platform;

  const showLearning = useShowLearning();
  const [isLearningExpanded, setIsLearningExpanded] = useState(false);

  return (
    <AppLayout>
      <PageSection variant="default">
        {platform && (
          <p>
            <strong>{t('common.platform', 'Platform')}:</strong> {platform}
          </p>
        )}
        <p>
          { totalCount 
            ? totalCount + ' ' + t('kafka.connectedClusters') 
            : <Skeleton width='5px' style={{display: 'inline'}}/>
          }
        </p>
      </PageSection>
      <PageSection>
        <ClustersDataView
          clusterResult={clusterResult}
          onDataViewChange={setDataParams}
        />
      </PageSection>
      {showLearning && (
        <PageSection>
          <Card
            component="div"
            isFullHeight
            isExpanded={isLearningExpanded}
            isCompact
          >
            <CardHeader onExpand={() => setIsLearningExpanded(!isLearningExpanded)}>
              <Content>
                <Flex
                  direction={{ default: 'column' }}
                  alignItems={{ default: 'alignItemsFlexStart' }}
                  spaceItems={{ default: 'spaceItemsXs' }}
                >
                  <FlexItem>
                    <CardTitle>
                      {t('home.recommendedLearningResources')}
                    </CardTitle>
                  </FlexItem>
                  {!isLearningExpanded && (
                    <FlexItem>
                      <Label color="orange" isCompact>
                        {t('home.documentation')}
                      </Label>
                    </FlexItem>
                  )}
                </Flex>
              </Content>
            </CardHeader>
            <CardExpandableContent>
              <CardBody>
                <Divider />
                <DataList
                  aria-label={t('home.recommendedLearningResources')}
                  isCompact
                  style={{ paddingTop: '1rem' }}
                >
                  <DataListItem aria-labelledby="learning-overview">
                    <DataListItemRow style={{ alignItems: 'center', minHeight: '3rem' }}>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="overview-label" width={2} style={{ display: 'flex', alignItems: 'center' }}>
                            <span id="learning-overview">{t('learning.labels.overview')}</span>
                          </DataListCell>,
                          <DataListCell key="overview-type" style={{ display: 'flex', alignItems: 'center' }}>
                            <Label color="orange" isCompact>
                              {t('home.documentation')}
                            </Label>
                          </DataListCell>,
                          <DataListCell key="overview-link" style={{ display: 'flex', alignItems: 'center' }}>
                            <a
                              href={t('learning.links.overview')}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {t('home.viewDocumentation')} <ExternalLinkAltIcon />
                            </a>
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>

                  {t('learning.links.gettingStarted') && (
                    <DataListItem aria-labelledby="learning-getting-started">
                      <DataListItemRow style={{ alignItems: 'center', minHeight: '3rem' }}>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="getting-started-label" width={2} style={{ display: 'flex', alignItems: 'center' }}>
                              <span id="learning-getting-started">
                                {t('learning.labels.gettingStarted')}
                              </span>
                            </DataListCell>,
                            <DataListCell key="getting-started-type" style={{ display: 'flex', alignItems: 'center' }}>
                              <Label color="orange" isCompact>
                                {t('home.documentation')}
                              </Label>
                            </DataListCell>,
                            <DataListCell key="getting-started-link" style={{ display: 'flex', alignItems: 'center' }}>
                              <a
                                href={t('learning.links.gettingStarted')}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                {t('home.viewDocumentation')} <ExternalLinkAltIcon />
                              </a>
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  )}

                  {t('learning.links.connecting') && (
                    <DataListItem aria-labelledby="learning-connecting">
                      <DataListItemRow style={{ alignItems: 'center', minHeight: '3rem' }}>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="connecting-label" width={2} style={{ display: 'flex', alignItems: 'center' }}>
                              <span id="learning-connecting">{t('learning.labels.connecting')}</span>
                            </DataListCell>,
                            <DataListCell key="connecting-type" style={{ display: 'flex', alignItems: 'center' }}>
                              <Label color="orange" isCompact>
                                {t('home.documentation')}
                              </Label>
                            </DataListCell>,
                            <DataListCell key="connecting-link" style={{ display: 'flex', alignItems: 'center' }}>
                              <a
                                href={t('learning.links.connecting')}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                {t('home.viewDocumentation')} <ExternalLinkAltIcon />
                              </a>
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  )}

                  <DataListItem aria-labelledby="learning-topic-operator">
                    <DataListItemRow style={{ alignItems: 'center', minHeight: '3rem' }}>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="topic-operator-label" width={2} style={{ display: 'flex', alignItems: 'center' }}>
                            <span id="learning-topic-operator">
                              {t('learning.labels.topicOperatorUse')}
                            </span>
                          </DataListCell>,
                          <DataListCell key="topic-operator-type" style={{ display: 'flex', alignItems: 'center' }}>
                            <Label color="orange" isCompact>
                              {t('home.documentation')}
                            </Label>
                          </DataListCell>,
                          <DataListCell key="topic-operator-link" style={{ display: 'flex', alignItems: 'center' }}>
                            <a
                              href={t('learning.links.topicOperatorUse')}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {t('home.viewDocumentation')} <ExternalLinkAltIcon />
                            </a>
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                </DataList>
              </CardBody>
            </CardExpandableContent>
          </Card>
        </PageSection>
      )}
    </AppLayout>
  );
}
