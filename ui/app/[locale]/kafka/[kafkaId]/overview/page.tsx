import { getKafkaClusterMetrics } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
  FlexItem,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  TimesCircleIcon,
} from "@/libs/patternfly/react-icons";
import { Suspense } from "react";

export default function OverviewPage({ params }: { params: KafkaParams }) {
  return (
    <>
      <PageSection isFilled>
        <Card style={{ textAlign: "center" }} component={"div"} isLarge>
          <CardTitle>Nodes</CardTitle>
          <CardBody>
            <Flex display={{ default: "inlineFlex" }}>
              <Flex spaceItems={{ default: "spaceItemsSm" }}>
                <FlexItem>
                  <CheckCircleIcon color="var(--pf-v5-global--success-color--100)" />
                </FlexItem>
                <FlexItem>3</FlexItem>
              </Flex>
              <Flex spaceItems={{ default: "spaceItemsSm" }}>
                <FlexItem>
                  <TimesCircleIcon color="var(--pf-v5-global--danger-color--100)" />
                </FlexItem>
                <FlexItem>1</FlexItem>
              </Flex>
            </Flex>
          </CardBody>
        </Card>
        <Card style={{ textAlign: "center" }} component={"div"} isLarge>
          <CardHeader>
            <CardTitle>Topics</CardTitle>
            <Flex
              alignItems={{ default: "alignItemsCenter" }}
              justifyContent={{ default: "justifyContentCenter" }}
            >
              <FlexItem flex={{ default: "flexNone" }}>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <Title headingLevel="h4" size="3xl">
                      123
                    </Title>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Total</span>
                  </FlexItem>
                </Flex>
              </FlexItem>

              <FlexItem flex={{ default: "flexNone" }}>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <Title headingLevel="h4" size="3xl">
                      842
                    </Title>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Partitions</span>
                  </FlexItem>
                </Flex>
              </FlexItem>

              <FlexItem flex={{ default: "flexNone" }}>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <Title headingLevel="h4" size="3xl">
                      3,45 GB
                    </Title>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Storage</span>
                  </FlexItem>
                </Flex>
              </FlexItem>
            </Flex>
          </CardHeader>
        </Card>
        <Card style={{ textAlign: "center" }} component={"div"} isLarge>
          <CardHeader>
            <CardTitle>Traffic</CardTitle>
            <Flex
              alignItems={{ default: "alignItemsCenter" }}
              justifyContent={{ default: "justifyContentCenter" }}
            >
              <FlexItem flex={{ default: "flexNone" }}>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <Title headingLevel="h4" size="3xl">
                      894,8 KB/s
                    </Title>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Egress</span>
                  </FlexItem>
                </Flex>
              </FlexItem>

              <FlexItem flex={{ default: "flexNone" }}>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <Title headingLevel="h4" size="3xl">
                      456,3 KB/s
                    </Title>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Ingress</span>
                  </FlexItem>
                </Flex>
              </FlexItem>
            </Flex>
          </CardHeader>
        </Card>

        <Suspense fallback={<div>Loading metrics</div>}>
          <ConnectedMetrics params={params} />
        </Suspense>
      </PageSection>
    </>
  );
}

async function ConnectedMetrics({ params }: { params: KafkaParams }) {
  const metrics = await getKafkaClusterMetrics(params.kafkaId);
  return (
    <Card>
      <pre>{JSON.stringify(metrics, null, 2)}</pre>
    </Card>
  );
}
