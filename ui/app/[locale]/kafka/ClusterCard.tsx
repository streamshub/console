"use client";
import { KafkaResource } from "@/api/resources";
import {
  ChartGroup,
  ChartLine,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
  FlexItem,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { Link, useRouter } from "@/navigation";
import { useFormatBytes } from "@/utils/format";
import { Route } from "next";

export function ClusterCard<T extends string>({
  id,
  attributes: { name, mechanism, principal, bootstrapServer },
  href,
}: KafkaResource & { href: Route<T> | URL }) {
  const cardId = `tool-card-${id}`;
  const router = useRouter();
  const formatBytes = useFormatBytes();
  return (
    <Card
      isClickable={true}
      key={id}
      id={cardId}
      ouiaId={cardId}
      isCompact={true}
    >
      <CardHeader
        selectableActions={{
          onClickAction: () => router.push(href.toString()),
          selectableActionId: `link-${id}`,
        }}
      >
        <CardTitle>
          <Link href={href}>{name}</Link>
          <TextContent>
            <Text component={"small"}>
              {principal}@{bootstrapServer}
            </Text>
          </TextContent>
        </CardTitle>
        <Flex className={"pf-v5-u-pt-md"}>
          <FlexItem>
            <Flex
              direction={{ default: "column" }}
              spaceItems={{ default: "spaceItemsNone" }}
            >
              <FlexItem>
                <CardTitle>
                  <Title headingLevel="h4" size="lg">
                    {formatBytes(Math.random() * 100000)}/s
                  </Title>
                </CardTitle>
              </FlexItem>
              <FlexItem>
                <span className="pf-v5-u-color-200">Ingress</span>
              </FlexItem>
            </Flex>
          </FlexItem>
          <FlexItem>
            <Flex
              direction={{ default: "column" }}
              spaceItems={{ default: "spaceItemsNone" }}
            >
              <FlexItem>
                <CardTitle>
                  <Title headingLevel="h4" size="lg">
                    {formatBytes(Math.random() * 100000)}/s
                  </Title>
                </CardTitle>
              </FlexItem>
              <FlexItem>
                <span className="pf-v5-u-color-200">Egress</span>
              </FlexItem>
            </Flex>
          </FlexItem>
        </Flex>
      </CardHeader>
      <CardBody>
        <ChartGroup
          ariaDesc="Mock average cluster utilization"
          ariaTitle="Mock cluster sparkline chart"
          containerComponent={
            <ChartVoronoiContainer
              labels={({ datum }) => `${datum.name}: ${datum.y}`}
              constrainToVisibleArea
            />
          }
          height={100}
          maxDomain={{ y: 83 }}
          padding={0}
          width={400}
        >
          <ChartLine
            data={[
              { name: "Ingress", x: "2015", y: 39 },
              { name: "Ingress", x: "2016", y: 16 },
              { name: "Ingress", x: "2017", y: 83 },
              { name: "Ingress", x: "2018", y: 32 },
              { name: "Ingress", x: "2019", y: 46 },
              { name: "Ingress", x: "2020", y: 10 },
              { name: "Ingress", x: "2021", y: 20 },
            ]}
          />
          <ChartLine
            data={[
              { name: "Egress", x: "2015", y: 7 },
              { name: "Egress", x: "2016", y: 6 },
              { name: "Egress", x: "2017", y: 8 },
              { name: "Egress", x: "2018", y: 3 },
              { name: "Egress", x: "2019", y: 4 },
              { name: "Egress", x: "2020", y: 1 },
              { name: "Egress", x: "2021", y: 0 },
            ]}
          />
        </ChartGroup>
      </CardBody>
    </Card>
  );
}
