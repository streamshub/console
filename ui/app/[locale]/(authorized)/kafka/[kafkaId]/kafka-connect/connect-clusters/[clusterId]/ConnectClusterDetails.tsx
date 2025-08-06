"use client";

import { ConnectorState } from "@/api/kafkaConnect/schema";
import { ResponsiveTable } from "@/components/Table";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  HistoryIcon,
  PauseCircleIcon,
  PendingIcon,
} from "@/libs/patternfly/react-icons";
import { TableVariant } from "@/libs/patternfly/react-table";
import {
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  Icon,
  Tab,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";
import { useTranslations } from "next-intl";
import { ReactNode, useState } from "react";
import Image from "next/image";

const StateLabel: Record<ConnectorState, { label: ReactNode }> = {
  UNASSIGNED: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp; Unassigned
      </>
    ),
  },
  RUNNING: {
    label: (
      <>
        <Icon status="success">
          <CheckCircleIcon />
        </Icon>
        &nbsp;Running
      </>
    ),
  },
  PAUSED: {
    label: (
      <>
        <Icon>
          <PauseCircleIcon />
        </Icon>
        &nbsp;Paused
      </>
    ),
  },
  STOPPED: {
    label: (
      <>
        <Icon>
          <Image
            src={"/stop-icon.svg"}
            alt="stop icon"
            width={100}
            height={100}
          />
        </Icon>
        &nbsp;Stopped
      </>
    ),
  },
  FAILED: {
    label: (
      <>
        <Icon status="danger">
          <ExclamationCircleIcon />
        </Icon>
        &nbsp;Failed
      </>
    ),
  },
  RESTARTING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Restarting
      </>
    ),
  },
};

export function ConnectClusterDetails({
  connectVersion,
  workers,
  data,
}: {
  connectVersion: string;
  workers: number;
  data: {
    name: string;
    type: "source" | "sink";
    state: ConnectorState;
    replicas: number | null;
  }[];
}) {
  const t = useTranslations("KafkaConnect");

  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);

  const handleTabClick = (
    event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent,
    tabIndex: string | number,
  ) => {
    setActiveTabKey(tabIndex);
  };

  return (
    <Flex direction={{ default: "column" }} gap={{ default: "gap2xl" }}>
      <FlexItem>
        <DescriptionList isHorizontal columnModifier={{ default: "2Col" }}>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("connect_clusters.version")}
            </DescriptionListTerm>
            <DescriptionListDescription>
              {connectVersion}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("connect_clusters.workers")}
            </DescriptionListTerm>
            <DescriptionListDescription>{workers}</DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </FlexItem>
      <FlexItem>
        <Tabs
          activeKey={activeTabKey}
          onSelect={handleTabClick}
          aria-label="Kafka Connect Tabs"
          role="region"
        >
          <Tab
            eventKey={0}
            title={<TabTitleText>Connectors</TabTitleText>}
            aria-label="Connectors Tab"
          >
            <ResponsiveTable
              ariaLabel="Kafka connectors"
              variant={TableVariant.compact}
              columns={["name", "type", "state", "replicas"] as const}
              data={data}
              renderHeader={({ column, key, Th }) => (
                <Th key={key}>
                  {column.charAt(0).toUpperCase() + column.slice(1)}
                </Th>
              )}
              renderCell={({ column, key, row, Td }) => {
                switch (column) {
                  case "name":
                    return <Td key={key}>{row.name}</Td>;
                  case "type":
                    return <Td key={key}>{row.type}</Td>;
                  case "state":
                    return <Td key={key}>{StateLabel[row.state].label}</Td>;
                  case "replicas":
                    return <Td key={key}>{row.replicas ?? "N/A"}</Td>;
                }
              }}
            />
          </Tab>
        </Tabs>
      </FlexItem>
    </Flex>
  );
}
