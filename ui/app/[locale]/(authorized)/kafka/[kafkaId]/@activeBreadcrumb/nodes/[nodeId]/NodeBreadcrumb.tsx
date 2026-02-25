import { KafkaNodeParams } from '@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params'
import { BreadcrumbLink } from '@/components/Navigation/BreadcrumbLink'
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { getTranslations } from 'next-intl/server'

export async function NodeBreadcrumb({
  params: paramsPromise,
}: {
  params: Promise<KafkaNodeParams>
}) {
  const { kafkaId, nodeId } = await paramsPromise
  const t = await getTranslations()
  return (
    <Breadcrumb ouiaId={'node-breadcrumb'}>
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
      <BreadcrumbLink
        key={'nodes'}
        href={`/kafka/${kafkaId}/nodes`}
        showDivider={true}
      >
        {t('nodes.title')}
      </BreadcrumbLink>
      <BreadcrumbItem key={'current-node'} showDivider={true}>
        Node&nbsp;{nodeId ?? '-'}
      </BreadcrumbItem>
    </Breadcrumb>
  )
}
