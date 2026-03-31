"use client";
import {
  ConsumerGroup,
  MemberDescription,
  OffsetAndMetadata,
} from "@/api/groups/schema";
import { LagTable } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/members/LagTable";
import { Number } from "@/components/Format/Number";
import { ResponsiveTable } from "@/components/Table";
import { Tooltip } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { TableVariant, Th } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";

export function MembersTable({
  kafkaId,
  consumerGroup,
}: {
  kafkaId: string;
  consumerGroup?: ConsumerGroup;
}) {
  const t = useTranslations("MemberTable");
  let members: MemberDescription[] | undefined = undefined;

  if (consumerGroup) {
    if (consumerGroup.attributes.members?.length === 0) {
      members = [
        {
          memberId: "unknown",
          host: "N/A",
          clientId: "unknown",
          assignments: consumerGroup.attributes.offsets?.map((o) => ({
            topicId: o.topicId,
            topicName: o.topicName,
            partition: o.partition,
          })),
        },
      ];
    } else {
      members = consumerGroup.attributes.members ?? [];
    }
  }
  return (
    <ResponsiveTable
      ariaLabel={"Group"}
      columns={
        ["member", "clientId", "overallLag", "assignedPartitions"] as const
      }
      data={members}
      variant={TableVariant.compact}
      renderHeader={({ column, key }) => {
        switch (column) {
          case "member":
            return (
              <Th width={30} key={key}>
                {t("member_id")}
              </Th>
            );
          case "clientId":
            return (
              <Th width={20} key={key}>
                {t("client_id")}{" "}
                <Tooltip content={t("client_id_tooltip")}>
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "overallLag":
            return (
              <Th key={key}>
                {t("overall_lag")}{" "}
                <Tooltip
                  style={{ whiteSpace: "pre-line" }}
                  content={t("overall_lag_tooltip")}
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "assignedPartitions":
            return <Th key={key}>{t("assigned_partitions")}</Th>;
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "member":
            return (
              <Td key={key} dataLabel={"Member ID"}>
                {row.memberId}
              </Td>
            );
          case "clientId":
            return (
              <Td key={key} dataLabel={"Client ID"}>
                {row.clientId}
              </Td>
            );
          case "overallLag": {
            const partitions: { topicName: string, partition: number }[] = row
              .assignments
              ?.map(a => {
                const partition = { topicName: a.topicName, partition: a.partition };
                return partition;
              }) ?? [];

            return (
              <Td key={key} dataLabel={"Overall lag"}>
                <Number
                  value={consumerGroup!.attributes.offsets
                    ?.filter(o => partitions.some(p => p.topicName === o.topicName && p.partition === o.partition))
                    .map(o => o.lag)
                    // lag values may not be available from API, e.g. when there is an error listing the topic offsets
                    .reduce((acc, v) => (acc ?? NaN) + (v ?? NaN), 0)}
                />
              </Td>
            );
          }
          case "assignedPartitions":
            return (
              <Td key={key} dataLabel={"Assigned partitions"}>
                <Number value={row.assignments?.length} />
              </Td>
            );
        }
      }}
      isRowExpandable={() => {
        return true;
      }}
      getExpandedRow={({ row }) => {
        const offsets: OffsetAndMetadata[] | undefined = row.assignments?.map(
          (a) => ({
            ...a,
            ...consumerGroup!.attributes.offsets?.find(
              (o) => o.topicId === a.topicId && o.partition === a.partition,
            )!,
          }),
        );
        offsets?.sort((a, b) => a.topicName.localeCompare(b.topicName));
        return (
          <div className={"pf-v6-u-p-lg"}>
            <LagTable kafkaId={kafkaId} offsets={offsets} />
          </div>
        );
      }}
    />
  );
}
