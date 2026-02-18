'use client'

import { Tabs, Tab, TabTitleText } from '@/libs/patternfly/react-core'
import { usePathname, useRouter } from '@/i18n/routing'
import { useCallback, useMemo } from 'react'

type TabConfig = {
  key: string
  label: string
  path: string
}

export type NodesTabsProps = {
  kafkaId: string | undefined
  cruiseControlEnable: boolean
}

export function NodesTabs({ kafkaId, cruiseControlEnable }: NodesTabsProps) {
  const pathname = usePathname()
  const router = useRouter()

  const tabsConfig: TabConfig[] = useMemo(() => {
    if (!kafkaId) return []
    const tabs: TabConfig[] = [
      {
        key: 'overview',
        label: 'Overview',
        path: `/kafka/${kafkaId}/nodes`,
      },
    ]
    if (cruiseControlEnable) {
      tabs.push({
        key: 'rebalance',
        label: 'Rebalance',
        path: `/kafka/${kafkaId}/nodes/rebalances`,
      })
    }
    return tabs
  }, [kafkaId, cruiseControlEnable])

  const activeTabKey =
    tabsConfig.find((tab) => pathname === tab.path)?.key ?? tabsConfig[0]?.key

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
      aria-label="Node navigation"
      role="region"
    >
      {tabsConfig.map((tab) => (
        <Tab
          ouiaId={'nodes-tab'}
          key={tab.key}
          eventKey={tab.key}
          title={<TabTitleText>{tab.label}</TabTitleText>}
        />
      ))}
    </Tabs>
  )
}
