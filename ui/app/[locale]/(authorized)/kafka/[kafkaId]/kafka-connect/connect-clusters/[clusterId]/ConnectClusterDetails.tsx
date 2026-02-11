"use client";

import {
  ConnectorState,
  ConnectorType,
  plugins,
} from "@/api/kafkaConnect/schema";
import { ResponsiveTable } from "@/components/Table";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  HelpIcon,
  HistoryIcon,
  PauseCircleIcon,
  PendingIcon,
} from "@/libs/patternfly/react-icons";
import { TableVariant, Th } from "@/libs/patternfly/react-table";
import {
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  Icon,
  Tab,
  TabContentBody,
  Tabs,
  TabTitleText,
  Tooltip,
  Truncate,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { ReactNode, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { ManagedConnectorLabel } from "../../ManagedConnectorLabel";

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

const TypeLabel: Record<ConnectorType, { label: ReactNode }> = {
  source: {
    label: <>Source</>,
  },
  sink: {
    label: <>Sink</>,
  },
  "source:mm": {
    label: <>Mirror Source</>,
  },
  "source:mm-checkpoint": {
    label: <>Mirror Checkpoint</>,
  },
  "source:mm-heartbeat": {
    label: <>Mirror Heartbeat</>,
  },
};

export function ConnectClusterDetails({
  connectVersion,
  workers,
  data,
  plugins,
  kafkaId,
}: {
  connectVersion: string;
  workers: number;
  data: {
    managed: boolean;
    id: string;
    name: string;
    type: ConnectorType;
    state: ConnectorState;
    replicas: number | null;
  }[];
  plugins: plugins[];
  kafkaId: string;
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
              {t("connect_clusters.workers")}{" "}
              <Tooltip content={t("connect_clusters.workers_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {workers ?? "-"}
            </DescriptionListDescription>
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
            title={<TabTitleText>{t("connectors_title")}</TabTitleText>}
            aria-label="Connectors Tab"
          >
            <TabContentBody>
              <ResponsiveTable
                ariaLabel="Kafka connectors"
                variant={TableVariant.compact}
                columns={["name", "type", "state", "replicas"] as const}
                data={data}
                renderHeader={({ column, key }) => (
                  <Th key={key}>
                    {column.charAt(0).toUpperCase() + column.slice(1)}
                  </Th>
                )}
                renderCell={({ column, key, row, Td }) => {
                  switch (column) {
                    case "name":
                      return (
                        <Td key={key}>
                          <Link
                            href={`/kafka/${kafkaId}/kafka-connect/${encodeURIComponent(row.id)}`}
                          >
                            <Truncate content={row.name!} />
                          </Link>
                          {row.managed === true && <ManagedConnectorLabel />}
                        </Td>
                      );
                    case "type":
                      return <Td key={key}>{TypeLabel[row.type].label}</Td>;
                    case "state":
                      return <Td key={key}>{StateLabel[row.state].label}</Td>;
                    case "replicas":
                      return <Td key={key}>{row.replicas ?? "-"}</Td>;
                  }
                }}
              />
            </TabContentBody>
          </Tab>
          <Tab
            eventKey={1}
            title={<TabTitleText>{t("connect_clusters.plugins")}</TabTitleText>}
            aria-label="Plugins"
          >
            <TabContentBody>
              <ResponsiveTable
                ariaLabel="Plugins"
                variant={TableVariant.compact}
                columns={["class", "type", "version"] as const}
                data={plugins}
                renderHeader={({ column, key }) => (
                  <Th key={key}>
                    {column.charAt(0).toUpperCase() + column.slice(1)}
                  </Th>
                )}
                renderCell={({ column, key, row, Td }) => {
                  switch (column) {
                    case "class":
                      return <Td key={key}>{row.class}</Td>;
                    case "type":
                      return <Td key={key}>{TypeLabel[row.type].label}</Td>;
                    case "version":
                      return <Td key={key}>{row.version}</Td>;
                  }
                }}
              />
            </TabContentBody>
          </Tab>
        </Tabs>
      </FlexItem>
    </Flex>
  );
}
