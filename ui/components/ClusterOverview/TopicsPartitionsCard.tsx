import { Number } from "@/components/Format/Number";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Divider,
  Flex,
  FlexItem,
  Icon,
  Skeleton,
  Text,
  TextContent,
  Tooltip,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
} from "@/libs/patternfly/react-icons";
import { Link } from "@/navigation";
import { useTranslations } from "next-intl";

type TopicsPartitionsCardProps = {
  topicsTotal: number;
  topicsReplicated: number;
  topicsUnderReplicated: number;
  partitions: number;
};

export function TopicsPartitionsCard({
  isLoading,
  topicsTotal,
  topicsReplicated,
  topicsUnderReplicated,
  partitions,
}:
  | ({ isLoading: false } & TopicsPartitionsCardProps)
  | ({
    isLoading: true;
  } & Partial<{ [key in keyof TopicsPartitionsCardProps]?: undefined }>)) {

  const t = useTranslations();
  return (
    <Card component={"div"}>
      <CardHeader
        actions={{
          actions: <Link href={"./topics"}>{t("ClusterOverview.view_all_topics")}</Link>,
        }}
      >
        <CardTitle>{t("ClusterOverview.topic_header")}</CardTitle>
      </CardHeader>
      <CardBody>
        <Flex gap={{ default: "gapLg" }}>
          <Flex
            flex={{ default: "flex_1" }}
            direction={{ default: "column" }}
            alignSelf={{ default: "alignSelfCenter" }}
          >
            <FlexItem>
              {isLoading ? (
                <Skeleton />
              ) : (
                <Flex gap={{ default: "gapMd" }}>
                  <FlexItem>
                    <TextContent>
                      <Text component={"small"}>
                        <Link href={"./topics"}>
                          <Number value={topicsTotal} /> {t("ClusterOverview.total_topics")}
                        </Link>
                      </Text>
                    </TextContent>
                  </FlexItem>
                  <Divider orientation={{ default: "vertical" }} />
                  <FlexItem>
                    <TextContent>
                      <Text component={"small"}>
                        <Number value={partitions} />
                        &nbsp; {t("ClusterOverview.partition")}
                      </Text>
                    </TextContent>
                  </FlexItem>
                </Flex>
              )}
            </FlexItem>
          </Flex>
          <Divider />
          <Flex
            flex={{ default: "flex_1" }}
            style={{ textAlign: "center" }}
            justifyContent={{ default: "justifyContentSpaceAround" }}
            direction={{ default: "column", "2xl": "row" }}
            flexWrap={{ default: "nowrap" }}
          >
            <FlexItem>
              <Link href={"./topics?status=FullyReplicated"}>
                {isLoading ? (
                  <Skeleton
                    shape={"circle"}
                    width={"1rem"}
                    style={{ display: "inline-block" }}
                  />
                ) : (
                  <Number value={topicsReplicated} />
                )}
              </Link>
              <TextContent className={"pf-v5-u-text-nowrap"}>
                <Text component={"small"}>
                  <Icon status={"success"}>
                    <CheckCircleIcon />
                  </Icon>
                  &nbsp;{t("ClusterOverview.fully_replicated_partition")}&nbsp;
                  <Tooltip content={t("ClusterOverview.fully_replicated_partition_tooltip")}>
                    <HelpIcon />
                  </Tooltip>
                </Text>
              </TextContent>
            </FlexItem>
            <FlexItem>
              <Link href={"./topics?status=UnderReplicated"}>
                {isLoading ? (
                  <Skeleton
                    shape={"circle"}
                    width={"1rem"}
                    style={{ display: "inline-block" }}
                  />
                ) : (
                  <Number value={topicsUnderReplicated} />
                )}
              </Link>
              <TextContent className={"pf-v5-u-text-nowrap"}>
                <Text component={"small"}>
                  <Icon status={"warning"}>
                    <ExclamationTriangleIcon />
                  </Icon>
                  &nbsp;{t("ClusterOverview.under_replicated_partition")}&nbsp;
                  <Tooltip content={t("ClusterOverview.under_replicated_partition_tooltip")}>
                    <HelpIcon />
                  </Tooltip>
                </Text>
              </TextContent>
            </FlexItem>
            <FlexItem>
              <Link href={"./topics?status=Offline"}>
                {isLoading ? (
                  <Skeleton
                    shape={"circle"}
                    width={"1rem"}
                    style={{ display: "inline-block" }}
                  />
                ) : (
                  <Number
                    value={
                      topicsTotal - topicsReplicated - topicsUnderReplicated
                    }
                  />
                )}
              </Link>
              <TextContent className={"pf-v5-u-text-nowrap"}>
                <Text component={"small"}>
                  <Icon status={"danger"}>
                    <ExclamationCircleIcon />
                  </Icon>
                  &nbsp;{t("ClusterOverview.unavailable_partition")}&nbsp;
                  <Tooltip content={t("ClusterOverview.unavailable_partition_tooltip")}>
                    <HelpIcon />
                  </Tooltip>
                </Text>
              </TextContent>
            </FlexItem>
          </Flex>
        </Flex>
      </CardBody>
    </Card>
  );
}
