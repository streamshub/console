"use client";

import {
  ConnectorConfig,
  ConnectorState,
  ConnectorTask,
} from "@/api/kafkaConnect/schema";
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
  TabContentBody,
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

export function ConnectorDetails({
  workerId,
  className,
  connectorTask,
  state,
  type,
  topics,
  maxTasks,
  config,
}: {
  className: string;
  workerId: string;
  state: ConnectorState;
  type: string;
  topics: string[];
  maxTasks: number;
  connectorTask: ConnectorTask[];
  config: ConnectorConfig;
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
              {t("connectors.connector_worker_id")}
            </DescriptionListTerm>
            <DescriptionListDescription>{workerId}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("connectors.class")}</DescriptionListTerm>
            <DescriptionListDescription>{className}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("connectors.state")}</DescriptionListTerm>
            <DescriptionListDescription>
              {StateLabel[state].label}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("connectors.type")}</DescriptionListTerm>
            <DescriptionListDescription>{type}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("connectors.topics")}</DescriptionListTerm>
            <DescriptionListDescription>{topics}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("connectors.max_tasks")}
            </DescriptionListTerm>
            <DescriptionListDescription>{maxTasks}</DescriptionListDescription>
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
            title={<TabTitleText>{t("connectors.tasks")}</TabTitleText>}
            aria-label={t("connectors.tasks")}
          >
            <TabContentBody hasPadding>
              <ResponsiveTable
                ariaLabel={t("connectors.tasks")}
                variant={TableVariant.compact}
                columns={["taskId", "state", "workerId"] as const}
                data={connectorTask}
                renderHeader={({ column, key, Th }) => {
                  switch (column) {
                    case "taskId":
                      return <Th key={key}>{t("connectors.taskId")}</Th>;
                    case "state":
                      return <Th key={key}>{t("connectors.state")}</Th>;
                    case "workerId":
                      return <Th key={key}>{t("connectors.workerId")}</Th>;
                  }
                }}
                renderCell={({ column, key, row, Td }) => {
                  switch (column) {
                    case "taskId":
                      return <Td key={key}>{row.attributes.taskId}</Td>;
                    case "state":
                      return (
                        <Td key={key}>
                          {StateLabel[row.attributes.state].label}
                        </Td>
                      );
                    case "workerId":
                      return <Td key={key}>{row.attributes.workerId}</Td>;
                  }
                }}
              />
            </TabContentBody>
          </Tab>
          <Tab
            eventKey={1}
            title={<TabTitleText>{t("connectors.configuration")}</TabTitleText>}
            aria-label={t("connectors.configuration")}
          >
            <TabContentBody hasPadding>
              <DescriptionList isHorizontal>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("connectors.connector_class")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {config["connector.class"]}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("connectors.max_tasks")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {config["tasks.max"]}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("connectors.configuration_file_name")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {config.name}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {"connectors.topics"}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {config.topic}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </DescriptionList>
            </TabContentBody>
          </Tab>
        </Tabs>
      </FlexItem>
    </Flex>
  );
}
