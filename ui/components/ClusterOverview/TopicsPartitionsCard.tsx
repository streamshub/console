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
  return (
    <Card component={"div"}>
      <CardHeader
        actions={{
          actions: <Link href={"./topics"}>View all</Link>,
        }}
      >
        <CardTitle>Topics</CardTitle>
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
                          <Number value={topicsTotal} /> total topics
                        </Link>
                      </Text>
                    </TextContent>
                  </FlexItem>
                  <Divider orientation={{ default: "vertical" }} />
                  <FlexItem>
                    <TextContent>
                      <Text component={"small"}>
                        <Number value={partitions} />
                        &nbsp; partitions
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
                  &nbsp;Fully replicated&nbsp;
                  <Tooltip
                    content={
                      "All partitions are fully replicated. A partition is fully-replicated when its replicas (followers) are 'in sync' with the designated partition leader. Replicas are 'in sync' if they have fetched records up to the log end offset of the leader partition within an allowable lag time, as determined by replica.lag.time.max.ms."
                    }
                  >
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
                  &nbsp;Under replicated&nbsp;
                  <Tooltip
                    content={
                      "Some partitions are not fully replicated. A partition is fully replicated when its replicas (followers) are 'in sync' with the designated partition leader. Replicas are 'in sync' if they have fetched records up to the log end offset of the leader partition within an allowable lag time, as determined by replica.lag.time.max.ms."
                    }
                  >
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
                  &nbsp;Unavailable&nbsp;
                  <Tooltip
                    content={
                      "Some or all partitions are currently unavailable. This may be due to issues such as broker failures or network problems. Investigate and address the underlying issues to restore normal functionality."
                    }
                  >
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
