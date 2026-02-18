'use client'

import {
  Label,
  Spinner,
  Tab,
  Tabs,
  TabTitleText,
} from '@/libs/patternfly/react-core'
import { usePathname, useRouter } from '@/i18n/routing'
import { Number } from '@/components/Format/Number'
import { useCallback, useMemo } from 'react'

type TabConfig = {
  key: string
  label: string
  path: string
  extra?: React.ReactNode
}

type TopicsTabsProps = {
  kafkaId: string
  topicId: string
  numPartitions?: number
  consumerGroupCount?: number
  isLoading?: boolean
}

export const TopicsTabs = ({
  kafkaId,
  topicId,
  numPartitions,
  consumerGroupCount,
  isLoading,
}: TopicsTabsProps) => {
  const pathname = usePathname()
  const router = useRouter()

  const tabsConfig: TabConfig[] = useMemo(
    () => [
      {
        key: 'messages',
        label: 'Messages',
        path: `/kafka/${kafkaId}/topics/${topicId}/messages`,
      },
      {
        key: 'partitions',
        label: 'Partitions',
        path: `/kafka/${kafkaId}/topics/${topicId}/partitions`,
        extra: (
          <Label isCompact>
            {isLoading ? (
              <Spinner size="sm" />
            ) : (
              <Number value={numPartitions ?? 0} />
            )}
          </Label>
        ),
      },
      {
        key: 'consumer-groups',
        label: 'Consumer Groups',
        path: `/kafka/${kafkaId}/topics/${topicId}/consumer-groups`,
        extra: (
          <Label isCompact>
            {isLoading ? (
              <Spinner size="sm" />
            ) : (
              <Number value={consumerGroupCount ?? 0} />
            )}
          </Label>
        ),
      },
      {
        key: 'configuration',
        label: 'Configuration',
        path: `/kafka/${kafkaId}/topics/${topicId}/configuration`,
      },
    ],
    [kafkaId, topicId, numPartitions, consumerGroupCount, isLoading],
  )

  const activeTabKey =
    tabsConfig.find((tab) => pathname.startsWith(tab.path))?.key ??
    tabsConfig[0].key

  const handleTabClick = useCallback(
    (_: React.MouseEvent<HTMLElement>, eventKey: string | number) => {
      const selectedTab = tabsConfig.find((tab) => tab.key === eventKey)
      if (selectedTab) {
        router.push(selectedTab.path)
      }
    },
    [router, tabsConfig],
  )

  return (
    <Tabs
      activeKey={activeTabKey}
      onSelect={handleTabClick}
      aria-label="Kafka topic tabs"
      role="region"
    >
      {tabsConfig.map((tab) => (
        <Tab
          id="topic-tabs"
          ouiaId={'Topic-tab'}
          key={tab.key}
          eventKey={tab.key}
          title={
            <TabTitleText>
              {tab.label}&nbsp;
              {tab.extra}
            </TabTitleText>
          }
        />
      ))}
    </Tabs>
  )
}
