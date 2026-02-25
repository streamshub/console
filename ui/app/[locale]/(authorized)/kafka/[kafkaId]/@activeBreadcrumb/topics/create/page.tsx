import { KafkaParams } from '@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params'
import { BreadcrumbLink } from '@/components/Navigation/BreadcrumbLink'
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { getTranslations } from 'next-intl/server'

export default async function TopicsActiveBreadcrumb({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>
}) {
  const { kafkaId } = await paramsPromise
  const t = await getTranslations('breadcrumbs')
  return (
    <Breadcrumb ouiaId={'create-topic-breadcrumb'}>
      <BreadcrumbItem key="home" to="/" showDivider>
        <Tooltip content={t('view_all_kafka_clusters')}>
          <HomeIcon />
        </Tooltip>
      </BreadcrumbItem>
      <BreadcrumbItem
        key="overview"
        to={`/kafka/${kafkaId}/overview`}
        showDivider
      >
        {t('overview')}
      </BreadcrumbItem>
      <BreadcrumbLink
        key={'topics'}
        href={`/kafka/${kafkaId}/topics`}
        showDivider={true}
      >
        {t('topics')}
      </BreadcrumbLink>
      <BreadcrumbItem key={'create-topic'} showDivider={true}>
        {t('create_topic')}
      </BreadcrumbItem>
    </Breadcrumb>
  )
}
