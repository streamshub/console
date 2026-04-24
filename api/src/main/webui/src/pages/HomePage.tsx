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
} from '@patternfly/react-core';
import { useKafkaClusters } from '../api/hooks/useKafkaClusters';
import { useMetadata } from '../api/hooks/useMetadata';
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
    </AppLayout>
  );
}