/**
 * Home Page - Kafka Cluster Selection
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  EmptyState,
  EmptyStateBody,
  Spinner,
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
} from '@patternfly/react-core';
import { ExternalLinkAltIcon } from '@patternfly/react-icons';
import { useKafkaClusters } from '../api/hooks/useKafkaClusters';
import { useMetadata } from '../api/hooks/useMetadata';
import { useShowLearning } from '../hooks/useShowLearning';
import { AppLayout } from '@/components/app/AppLayout';
import { ClustersTable } from '@/components/home/ClustersTable';

export function HomePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [filterName, setFilterName] = useState<string>('');

  const { data, isLoading, error } = useKafkaClusters({
    pageSize: perPage,
    sort: sortBy,
    sortDir: sortDirection,
    name: filterName || undefined,
  });

  const { data: metadata } = useMetadata();

  const clusters = data?.data || [];
  const totalCount = data?.meta?.page?.total;
  const platform = metadata?.data?.attributes?.platform;

  // If only one cluster, redirect to it
  useEffect(() => {
    if (clusters.length === 1 && totalCount === 1 && !filterName) {
      navigate(`/kafka/${clusters[0].id}`);
    }
  }, [clusters, totalCount, filterName, navigate]);

  const handleSort = (column: string, direction: 'asc' | 'desc') => {
    setSortBy(column);
    setSortDirection(direction);
    setPage(1); // Reset to first page when sorting changes
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handlePerPageChange = (newPerPage: number) => {
    setPerPage(newPerPage);
    setPage(1); // Reset to first page when page size changes
  };

  const handleFilterNameChange = (name: string) => {
    setFilterName(name);
    setPage(1); // Reset to first page when filter changes
  };

  const showLearning = useShowLearning();
  const [isLearningExpanded, setIsLearningExpanded] = useState(false);

  if (isLoading) {
    return (
      <AppLayout>
        <PageSection>
          <EmptyState>
            <Spinner size="xl" />
            <Title headingLevel="h1" size="lg">
              {t('common.loading')}
            </Title>
          </EmptyState>
        </PageSection>
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout>
        <PageSection>
          <EmptyState>
            <Title headingLevel="h1" size="lg">
              {t('common.error')}
            </Title>
            <EmptyStateBody>{error.message}</EmptyStateBody>
          </EmptyState>
        </PageSection>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <PageSection variant="default">
        {platform && (
          <p>
            <strong>{t('common.platform', 'Platform')}:</strong> {platform}
          </p>
        )}
        {totalCount !== undefined && (
          <p>
            {totalCount} {t('kafka.connectedClusters', 'Connected Kafka clusters')}
          </p>
        )}
      </PageSection>
      <PageSection>
        <ClustersTable
          clusters={clusters}
          totalCount={totalCount}
          page={page}
          perPage={perPage}
          onPageChange={handlePageChange}
          onPerPageChange={handlePerPageChange}
          sortBy={sortBy}
          sortDirection={sortDirection}
          onSort={handleSort}
          filterName={filterName}
          onFilterNameChange={handleFilterNameChange}
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
                <CardTitle>
                  {t('home.recommendedLearningResources')}
                </CardTitle>
                {!isLearningExpanded && (
                  <Content component="small">
                    <Label color="orange" isCompact>
                      {t('home.documentation')}
                    </Label>
                  </Content>
                )}
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