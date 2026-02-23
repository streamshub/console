import { OffsetAndMetadata } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Format/Number";
import { ResponsiveTable } from "@/components/Table";
import { Tooltip } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { Link } from "@/i18n/routing";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";

export function LagTable({
  kafkaId,
  offsets,
}: {
  kafkaId: string;
  offsets: OffsetAndMetadata[] | undefined;
}) {
  const t = useTranslations("MemberTable");

  return (
    <ResponsiveTable
      ariaLabel={"Group lag"}
      columns={
        ["topic", "partition", "behind", "currentOffset", "endOffset"] as const
      }
      data={offsets}
      variant={TableVariant.compact}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "topic":
            return (
              <Th key={key} width={30}>
                {t("topic")}
              </Th>
            );
          case "partition":
            return <Th key={key}>{t("partition")}</Th>;
          case "behind":
            return <Th key={key}>{t("lag")}</Th>;
          case "currentOffset":
            return (
              <Th key={key}>
                {t("committed_offset")}{" "}
                <Tooltip content={t("committed_offset_tooltip")}>
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "endOffset":
            return (
              <Th key={key}>
                {t("end_offset")}{" "}
                <Tooltip content={t("end_offset_tooltip")}>
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "topic":
            return (
              <Td key={key} dataLabel={"Lagging topic"}>
                <Link href={`/kafka/${kafkaId}/topics/${row.topicId}`}>
                  {row.topicName}
                </Link>
              </Td>
            );
          case "partition":
            return (
              <Td key={key} dataLabel={"Partition"}>
                <Number value={row.partition} />
              </Td>
            );
          case "behind":
            return (
              <Td key={key} dataLabel={"Messages behind"}>
                <Number value={row.lag} />
              </Td>
            );
          case "currentOffset":
            return (
              <Td key={key} dataLabel={"Current offset"}>
                <Number value={row.offset} />
              </Td>
            );
          case "endOffset":
            return (
              <Td key={key} dataLabel={"End offset"}>
                <Number value={row.logEndOffset} />
              </Td>
            );
        }
      }}
    />
  );
}
