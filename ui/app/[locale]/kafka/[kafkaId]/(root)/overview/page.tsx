"use client";
import {
  ChartArea,
  ChartGroup,
  ChartVoronoiContainer,
} from "@/libs/patternfly/react-charts";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
  FlexItem,
  Gallery,
  GalleryItem,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  TimesCircleIcon,
} from "@/libs/patternfly/react-icons";

export default function OverviewPage() {
  return (
    <>
      <PageSection isFilled>
        <Gallery
          hasGutter
          style={
            { "--pf-v5-l-gallery--GridTemplateColumns--min": "30%" } as any
          }
        >
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
          <GalleryItem>
            <Card id="trend-card-1-card" component="div">
              <CardHeader>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <CardTitle>
                      <Title headingLevel="h4" size="lg">
                        1,050,765 IOPS
                      </Title>
                    </CardTitle>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Workload</span>
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
                  maxDomain={{ y: 9 }}
                  padding={0}
                  width={400}
                >
                  <ChartArea
                    data={[
                      { name: "Cluster", x: "2015", y: 7 },
                      { name: "Cluster", x: "2016", y: 6 },
                      { name: "Cluster", x: "2017", y: 8 },
                      { name: "Cluster", x: "2018", y: 3 },
                      { name: "Cluster", x: "2019", y: 4 },
                      { name: "Cluster", x: "2020", y: 1 },
                      { name: "Cluster", x: "2021", y: 0 },
                    ]}
                  />
                </ChartGroup>
              </CardBody>
            </Card>
          </GalleryItem>
          <GalleryItem>
            <Card id="trend-card-1-card" component="div">
              <CardHeader>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <CardTitle>
                      <Title headingLevel="h4" size="lg">
                        1,050,765 IOPS
                      </Title>
                    </CardTitle>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Workload</span>
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
                  maxDomain={{ y: 9 }}
                  padding={0}
                  width={400}
                >
                  <ChartArea
                    data={[
                      { name: "Cluster", x: "2015", y: 7 },
                      { name: "Cluster", x: "2016", y: 6 },
                      { name: "Cluster", x: "2017", y: 8 },
                      { name: "Cluster", x: "2018", y: 3 },
                      { name: "Cluster", x: "2019", y: 4 },
                      { name: "Cluster", x: "2020", y: 1 },
                      { name: "Cluster", x: "2021", y: 0 },
                    ]}
                  />
                </ChartGroup>
              </CardBody>
            </Card>
          </GalleryItem>
          <GalleryItem>
            <Card id="trend-card-1-card" component="div">
              <CardHeader>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <CardTitle>
                      <Title headingLevel="h4" size="lg">
                        1,050,765 IOPS
                      </Title>
                    </CardTitle>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Workload</span>
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
                  maxDomain={{ y: 9 }}
                  padding={0}
                  width={400}
                >
                  <ChartArea
                    data={[
                      { name: "Cluster", x: "2015", y: 7 },
                      { name: "Cluster", x: "2016", y: 6 },
                      { name: "Cluster", x: "2017", y: 8 },
                      { name: "Cluster", x: "2018", y: 3 },
                      { name: "Cluster", x: "2019", y: 4 },
                      { name: "Cluster", x: "2020", y: 1 },
                      { name: "Cluster", x: "2021", y: 0 },
                    ]}
                  />
                </ChartGroup>
              </CardBody>
            </Card>
          </GalleryItem>
          <GalleryItem>
            <Card id="trend-card-1-card" component="div">
              <CardHeader>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <CardTitle>
                      <Title headingLevel="h4" size="lg">
                        1,050,765 IOPS
                      </Title>
                    </CardTitle>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Workload</span>
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
                  maxDomain={{ y: 9 }}
                  padding={0}
                  width={400}
                >
                  <ChartArea
                    data={[
                      { name: "Cluster", x: "2015", y: 7 },
                      { name: "Cluster", x: "2016", y: 6 },
                      { name: "Cluster", x: "2017", y: 8 },
                      { name: "Cluster", x: "2018", y: 3 },
                      { name: "Cluster", x: "2019", y: 4 },
                      { name: "Cluster", x: "2020", y: 1 },
                      { name: "Cluster", x: "2021", y: 0 },
                    ]}
                  />
                </ChartGroup>
              </CardBody>
            </Card>
          </GalleryItem>
          <GalleryItem>
            <Card id="trend-card-1-card" component="div">
              <CardHeader>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <CardTitle>
                      <Title headingLevel="h4" size="lg">
                        1,050,765 IOPS
                      </Title>
                    </CardTitle>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Workload</span>
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
                  maxDomain={{ y: 9 }}
                  padding={0}
                  width={400}
                >
                  <ChartArea
                    data={[
                      { name: "Cluster", x: "2015", y: 7 },
                      { name: "Cluster", x: "2016", y: 6 },
                      { name: "Cluster", x: "2017", y: 8 },
                      { name: "Cluster", x: "2018", y: 3 },
                      { name: "Cluster", x: "2019", y: 4 },
                      { name: "Cluster", x: "2020", y: 1 },
                      { name: "Cluster", x: "2021", y: 0 },
                    ]}
                  />
                </ChartGroup>
              </CardBody>
            </Card>
          </GalleryItem>
          <GalleryItem>
            <Card id="trend-card-1-card" component="div">
              <CardHeader>
                <Flex
                  direction={{ default: "column" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <CardTitle>
                      <Title headingLevel="h4" size="lg">
                        1,050,765 IOPS
                      </Title>
                    </CardTitle>
                  </FlexItem>
                  <FlexItem>
                    <span className="pf-v5-u-color-200">Workload</span>
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
                  maxDomain={{ y: 9 }}
                  padding={0}
                  width={400}
                >
                  <ChartArea
                    data={[
                      { name: "Cluster", x: "2015", y: 7 },
                      { name: "Cluster", x: "2016", y: 6 },
                      { name: "Cluster", x: "2017", y: 8 },
                      { name: "Cluster", x: "2018", y: 3 },
                      { name: "Cluster", x: "2019", y: 4 },
                      { name: "Cluster", x: "2020", y: 1 },
                      { name: "Cluster", x: "2021", y: 0 },
                    ]}
                  />
                </ChartGroup>
              </CardBody>
            </Card>
          </GalleryItem>
        </Gallery>
      </PageSection>
    </>
  );
}
