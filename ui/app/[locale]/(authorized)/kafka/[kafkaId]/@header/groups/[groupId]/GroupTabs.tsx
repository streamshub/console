"use client";

import {
  Tab,
  Tabs,
  TabTitleText,
} from "@/libs/patternfly/react-core";
import { usePathname, useRouter } from "@/i18n/routing";
import { useCallback, useMemo } from "react";

type TabConfig = {
  key: string;
  label: string;
  path: string;
  extra?: React.ReactNode;
};

type GroupTabsProps = {
  kafkaId: string;
  groupId: string;
};

export const GroupTabs = ({
  kafkaId,
  groupId,
}: GroupTabsProps) => {
  const pathname = usePathname();
  const router = useRouter();

  const tabsConfig: TabConfig[] = useMemo(
    () => [
      {
        key: "members",
        label: "Members",
        path: `/kafka/${kafkaId}/groups/${groupId}/members`,
      },
      {
        key: "configuration",
        label: "Configuration",
        path: `/kafka/${kafkaId}/groups/${groupId}/configuration`,
      },
    ],
    [kafkaId, groupId],
  );

  const activeTabKey =
    tabsConfig.find((tab) => pathname.startsWith(tab.path))?.key ??
    tabsConfig[0].key;

  const handleTabClick = useCallback(
    (_: React.MouseEvent<HTMLElement>, eventKey: string | number) => {
      const selectedTab = tabsConfig.find((tab) => tab.key === eventKey);
      if (selectedTab) {
        router.push(selectedTab.path);
      }
    },
    [router, tabsConfig],
  );

  return (
    <Tabs
      activeKey={activeTabKey}
      onSelect={handleTabClick}
      aria-label="Kafka group tabs"
      role="region"
    >
      {tabsConfig.map((tab) => (
        <Tab
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
  );
};
