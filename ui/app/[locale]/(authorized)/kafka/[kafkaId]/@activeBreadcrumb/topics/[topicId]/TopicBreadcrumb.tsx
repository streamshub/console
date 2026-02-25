import { getTopic } from '@/api/topics/actions'
import { KafkaTopicParams } from '@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params'
import { BreadcrumbLink } from '@/components/Navigation/BreadcrumbLink'
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { getTranslations } from 'next-intl/server'

export async function TopicBreadcrumb({
  params: paramsPromise,
}: {
  params: Promise<KafkaTopicParams>
}) {
  const { kafkaId, topicId } = await paramsPromise
  const t = await getTranslations('breadcrumbs')
  const response = await getTopic(kafkaId, topicId)

  return (
    <Breadcrumb ouiaId={'topic-breadcrumb'}>
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
      <BreadcrumbItem key={'current-topic'} showDivider={true}>
        {response.payload?.attributes.name ?? topicId}
      </BreadcrumbItem>
    </Breadcrumb>
  )
}
