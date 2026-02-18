'use client'

import { useOpenClusterConnectionPanel } from '@/components/ClusterDrawerContext'
import { Button } from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'

export function ConnectButton({ clusterId }: { clusterId: string }) {
  const t = useTranslations()
  const open = useOpenClusterConnectionPanel()
  return (
    <Button ouiaId={'connect-button'} onClick={() => open(clusterId)}>
      {t('ConnectButton.cluster_connection_details')}
    </Button>
  )
}
