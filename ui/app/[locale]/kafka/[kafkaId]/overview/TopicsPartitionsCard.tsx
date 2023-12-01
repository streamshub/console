import { Number } from "@/components/Number";
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
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
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
          actions: <Link href={""}>View all</Link>,
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
                        <span className="pf-v5-u-primary-color-100">
                          <Number value={topicsTotal} /> total topics
                        </span>
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
              <div className="pf-v5-u-primary-color-100">
                {isLoading ? (
                  <Skeleton
                    shape={"circle"}
                    width={"1rem"}
                    style={{ display: "inline-block" }}
                  />
                ) : (
                  <Number value={topicsReplicated} />
                )}
              </div>
              <TextContent className={"pf-v5-u-text-nowrap"}>
                <Text component={"small"}>
                  <Icon status={"success"}>
                    <CheckCircleIcon />
                  </Icon>
                  &nbsp;Fully replicated
                </Text>
              </TextContent>
            </FlexItem>
            <FlexItem>
              <div className="pf-v5-u-primary-color-100">
                {isLoading ? (
                  <Skeleton
                    shape={"circle"}
                    width={"1rem"}
                    style={{ display: "inline-block" }}
                  />
                ) : (
                  <Number value={topicsUnderReplicated} />
                )}
              </div>
              <TextContent className={"pf-v5-u-text-nowrap"}>
                <Text component={"small"}>
                  <Icon status={"warning"}>
                    <ExclamationTriangleIcon />
                  </Icon>
                  &nbsp;Under replicated
                </Text>
              </TextContent>
            </FlexItem>
            <FlexItem>
              <div className="pf-v5-u-primary-color-100">
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
              </div>
              <TextContent className={"pf-v5-u-text-nowrap"}>
                <Text component={"small"}>
                  <Icon status={"danger"}>
                    <ExclamationCircleIcon />
                  </Icon>
                  &nbsp;Unavailable
                </Text>
              </TextContent>
            </FlexItem>
          </Flex>
        </Flex>
      </CardBody>
    </Card>
  );
}
