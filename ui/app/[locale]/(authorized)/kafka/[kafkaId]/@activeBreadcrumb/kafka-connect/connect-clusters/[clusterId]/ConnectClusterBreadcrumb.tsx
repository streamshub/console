'use client'

import RichText from '@/components/RichText'
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { useTranslations } from 'next-intl'

export function ConnectClusterBreadcrumb({
  kafkaId,
  connectClusterName,
}: {
  kafkaId: string
  connectClusterName: string
}) {
  const t = useTranslations()

  return (
    <Breadcrumb ouiaId={'connect-cluster-breadcrumb'}>
      <BreadcrumbItem key="home" to="/" showDivider>
        <Tooltip content={t('breadcrumbs.view_all_kafka_clusters')}>
          <HomeIcon />
        </Tooltip>
      </BreadcrumbItem>
      <BreadcrumbItem
        key="overview"
        to={`/kafka/${kafkaId}/overview`}
        showDivider
      >
        {t('breadcrumbs.overview')}
      </BreadcrumbItem>
      <BreadcrumbItem
        key="kafka-connect"
        to={`/kafka/${kafkaId}/kafka-connect`}
        showDivider
      >
        {t('breadcrumbs.Kafka_connect')}
      </BreadcrumbItem>
      <BreadcrumbItem
        key="connect_clusters"
        to={`/kafka/${kafkaId}/kafka-connect/connect-clusters`}
        showDivider
      >
        {t('breadcrumbs.connect_clusters')}
      </BreadcrumbItem>
      <BreadcrumbItem key={'cgm'} showDivider={true} isActive={true}>
        {decodeURIComponent(connectClusterName) === '+' ? (
          <RichText>{(tags) => t.rich('common.empty_name', tags)}</RichText>
        ) : (
          connectClusterName
        )}
      </BreadcrumbItem>
    </Breadcrumb>
  )
}
