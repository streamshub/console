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
import { TableVariant } from "@patternfly/react-table";
import { useTranslations } from "next-intl";
import { useState } from "react";

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
  const authorizationData = Object.entries(authorization || {});

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
                columns={["property", "value"] as const}
                data={authorizationData}
                renderHeader={({ column, key, Th }) => {
                  switch (column) {
                    case "property":
                      return (
                        <Th key={key} width={60}>
                          {"Property"}
                        </Th>
                      );
                    case "value":
                      return <Th key={key}>{"Value"}</Th>;
                  }
                }}
                renderCell={({ column, key, row: [name, value], Td }) => {
                  switch (column) {
                    case "property":
                      return (
                        <Td key={key} dataLabel={"Property"}>
                          {name}
                        </Td>
                      );
                    case "value":
                      return (
                        <Td key={key} dataLabel={"Value"}>
                          {value ?? t("n_a")}
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
