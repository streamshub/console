import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { getTranslations } from 'next-intl/server'
import { KafkaParams } from '../../kafka.params'

export default async function NodesActiveBreadcrumbPage({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>
}) {
  const { kafkaId } = await paramsPromise
  return <NodesActiveBreadcrumb kafkaId={kafkaId} />
}

async function NodesActiveBreadcrumb({ kafkaId }: { kafkaId: string }) {
  const t = await getTranslations()

  return (
    <Breadcrumb ouiaId={'nodes-breadcrumb'}>
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
      <BreadcrumbItem showDivider={true}>{t('nodes.title')}</BreadcrumbItem>
    </Breadcrumb>
  )
}
