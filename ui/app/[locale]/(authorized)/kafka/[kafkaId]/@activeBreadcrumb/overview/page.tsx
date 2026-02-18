import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from '@/libs/patternfly/react-core'
import { HomeIcon } from '@/libs/patternfly/react-icons'
import { useTranslations } from 'next-intl'

export default function OverviewBreadcrumb() {
  const t = useTranslations('breadcrumbs')

  return (
    <Breadcrumb ouiaId={'overview-breadcrumb'}>
      <BreadcrumbItem key="home" to="/" showDivider>
        <Tooltip content={t('view_all_kafka_clusters')}>
          <HomeIcon />
        </Tooltip>
      </BreadcrumbItem>
      <BreadcrumbItem key={t('overview')} showDivider>
        {t('overview')}
      </BreadcrumbItem>
    </Breadcrumb>
  )
}
