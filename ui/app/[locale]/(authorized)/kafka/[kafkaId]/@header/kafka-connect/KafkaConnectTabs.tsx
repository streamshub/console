'use client'

import { Tabs, Tab, TabTitleText } from '@/libs/patternfly/react-core'
import { usePathname, useRouter } from '@/i18n/routing'
import { useCallback, useMemo } from 'react'

type TabConfig = {
  key: string
  label: string
  path: string
}

export type KafkaConnectTabsProps = {
  kafkaId: string | undefined
}

export function KafkaConnectTabs({ kafkaId }: KafkaConnectTabsProps) {
  const pathname = usePathname()
  const router = useRouter()

  const tabsConfig: TabConfig[] = useMemo(() => {
    if (!kafkaId) return []
    const tabs: TabConfig[] = [
      {
        key: 'connectors',
        label: 'Connectors',
        path: `/kafka/${kafkaId}/kafka-connect`,
      },
      {
        key: 'connect-clusters',
        label: 'Connect clusters',
        path: `/kafka/${kafkaId}/kafka-connect/connect-clusters`,
      },
    ]
    return tabs
  }, [kafkaId])

  const activeTabKey =
    tabsConfig
      .slice()
      .sort((a, b) => b.path.length - a.path.length)
      .find((tab) => pathname.startsWith(tab.path))?.key ?? tabsConfig[0]?.key

  const handleTabClick = useCallback(
    (_: React.MouseEvent<HTMLElement>, eventKey: string | number) => {
      const selectedTab = tabsConfig.find((tab) => tab.key === eventKey)
      if (selectedTab) {
        router.push(selectedTab.path)
      }
    },
    [router, tabsConfig],
  )

  if (!kafkaId) return null

  return (
    <Tabs
      activeKey={activeTabKey}
      onSelect={handleTabClick}
      aria-label="kafka connect navigation"
      role="region"
    >
      {tabsConfig.map((tab) => (
        <Tab
          ouiaId={'kafka-connect-tabs'}
          key={tab.key}
          eventKey={tab.key}
          title={<TabTitleText>{tab.label}</TabTitleText>}
        />
      ))}
    </Tabs>
  )
}
