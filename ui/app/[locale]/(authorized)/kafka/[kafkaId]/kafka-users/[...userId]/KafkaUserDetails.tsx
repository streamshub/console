"use client";

import { KafkaUser } from "@/api/kafkaUsers/schema";
import { DateTime } from "@/components/Format/DateTime";
import { TableView } from "@/components/Table";
import {
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  Tab,
  TabContentBody,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";
import { TableVariant, Th } from "@patternfly/react-table";
import { useTranslations } from "next-intl";
import { useState } from "react";

export const AuthorizationColumns = [
  "type",
  "resourceName",
  "patternType",
  "host",
  "operations",
  "permissionType",
] as const;

export function KafkaUserDetails({
  kafkaUser,
}: {
  kafkaUser: KafkaUser | undefined;
}) {
  const t = useTranslations("kafkausers");
  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);

  const handleTabClick = (
    _event: React.MouseEvent<unknown> | React.KeyboardEvent,
    tabIndex: string | number,
  ) => {
    setActiveTabKey(tabIndex);
  };

  if (!kafkaUser) {
    return <div>{"No user data"}</div>;
  }

  const {
    name,
    namespace,
    creationTimestamp,
    username,
    authenticationType,
    authorization,
  } = kafkaUser.attributes;
  const authorizationData = authorization?.accessControls ?? [];

  return (
    <Flex direction={{ default: "column" }} gap={{ default: "gap2xl" }}>
      <FlexItem>
        <DescriptionList isHorizontal columnModifier={{ default: "2Col" }}>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("name")}</DescriptionListTerm>
            <DescriptionListDescription>{name}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("username")}</DescriptionListTerm>
            <DescriptionListDescription>{username}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("authentication")}</DescriptionListTerm>
            <DescriptionListDescription>
              {authenticationType}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("namespace")}</DescriptionListTerm>
            <DescriptionListDescription>
              {namespace ?? "-"}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("creationTimestamp")}</DescriptionListTerm>
            <DescriptionListDescription>
              {creationTimestamp ? <DateTime value={creationTimestamp} /> : "-"}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </FlexItem>

      <FlexItem>
        <Tabs
          activeKey={activeTabKey}
          onSelect={handleTabClick}
          aria-label="Kafka User Details Tabs"
          role="region"
        >
          <Tab
            eventKey={0}
            title={<TabTitleText>{t("authorization")}</TabTitleText>}
            aria-label={t("authorization")}
          >
            <TabContentBody>
              <TableView
                ariaLabel={t("authorization")}
                variant={TableVariant.compact}
                onPageChange={() => {}}
                emptyStateNoData={<div>{t("no_authorization")}</div>}
                emptyStateNoResults={<div></div>}
                columns={AuthorizationColumns}
                data={authorizationData}
                renderHeader={({ column, key }) => {
                  switch (column) {
                    case "type":
                      return (
                        <Th key={key}>{t("authorization_columns.type")}</Th>
                      );
                    case "resourceName":
                      return (
                        <Th key={key}>
                          {t("authorization_columns.resourceName")}
                        </Th>
                      );
                    case "patternType":
                      return (
                        <Th key={key}>
                          {t("authorization_columns.patternType")}
                        </Th>
                      );
                    case "host":
                      return (
                        <Th key={key}>{t("authorization_columns.host")}</Th>
                      );
                    case "operations":
                      return (
                        <Th key={key}>
                          {t("authorization_columns.operations")}
                        </Th>
                      );
                    case "permissionType":
                      return (
                        <Th key={key}>
                          {t("authorization_columns.permissionType")}
                        </Th>
                      );
                  }
                }}
                renderCell={({ column, key, row, Td }) => {
                  switch (column) {
                    case "type":
                      return (
                        <Td
                          key={key}
                          dataLabel={t("authorization_columns.type")}
                        >
                          {row.type}
                        </Td>
                      );
                    case "resourceName":
                      return (
                        <Td
                          key={key}
                          dataLabel={t("authorization_columns.resourceName")}
                        >
                          {row.resourceName ?? "-"}
                        </Td>
                      );
                    case "patternType":
                      return (
                        <Td
                          key={key}
                          dataLabel={t("authorization_columns.patternType")}
                        >
                          {row.patternType ?? "-"}
                        </Td>
                      );
                    case "host":
                      return (
                        <Td
                          key={key}
                          dataLabel={t("authorization_columns.host")}
                        >
                          {row.host ?? "-"}
                        </Td>
                      );
                    case "operations":
                      return (
                        <Td
                          key={key}
                          dataLabel={t("authorization_columns.operations")}
                        >
                          {row.operations?.join(", ")}
                        </Td>
                      );
                    case "permissionType":
                      return (
                        <Td
                          key={key}
                          dataLabel={t("authorization_columns.permissionType")}
                        >
                          {row.permissionType}
                        </Td>
                      );
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
