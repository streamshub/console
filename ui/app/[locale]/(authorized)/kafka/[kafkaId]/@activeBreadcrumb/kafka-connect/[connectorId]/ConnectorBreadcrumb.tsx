'use client'

import RichText from '@/components/RichText'
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { useTranslations } from 'next-intl'

export function ConnectorBreadcrumb({
  kafkaId,
  connectorName,
}: {
  kafkaId: string
  connectorName: string
}) {
  const t = useTranslations()

  return (
    <Breadcrumb ouiaId={'connector-breadcrumb'}>
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
        showDivider={true}
      >
        {t('breadcrumbs.Kafka_connect')}
      </BreadcrumbItem>
      <BreadcrumbItem key={'cgm'} showDivider={true} isActive={true}>
        {decodeURIComponent(connectorName) === '+' ? (
          <RichText>{(tags) => t.rich('common.empty_name', tags)}</RichText>
        ) : (
          connectorName
        )}
      </BreadcrumbItem>
    </Breadcrumb>
  )
}
