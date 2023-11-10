import { ClusterDetail, getKafkaCluster, getKafkaClusters } from "@/api/kafka";
import { getViewedTopics } from "@/api/topics/actions";
import { ClustersTable } from "@/app/[locale]/home/ClustersTable";
import { ExpandableCard } from "@/app/[locale]/home/ExpandableCard";
import { TopicsTable } from "@/app/[locale]/home/TopicsTable";
import {
  Button,
  CardBody,
  CardTitle,
  EmptyState,
  EmptyStateBody,
  Flex,
  Grid,
  Label,
  LabelGroup,
  Level,
  List,
  ListItem,
  PageSection,
  Stack,
  StackItem,
  Title,
} from "@/libs/patternfly/react-core";
import {
  ArrowRightIcon,
  ExternalLinkAltIcon,
  InfoCircleIcon,
} from "@/libs/patternfly/react-icons";
import { Suspense } from "react";
import styles from "./home.module.css";

export default function Home() {
  return (
    <>
      <PageSection padding={{ default: "noPadding" }} variant={"light"}>
        <div className={styles.hero}>
          <div>
            <Title headingLevel={"h1"} size={"3xl"}>
              Welcome to the AMQ Streams Console
            </Title>
            <Title
              headingLevel={"h2"}
              size={"lg"}
              className={"pf-v5-u-color-200"}
            >
              Manage your Kafka resources in one place.
            </Title>
          </div>
        </div>
      </PageSection>
      <PageSection>
        <Stack hasGutter={true}>
          <StackItem>
            <ExpandableCard title={"Platform: TODO Get OS Cluster name"}>
              <CardBody>
                <Suspense fallback={<ClustersTable clusters={undefined} />}>
                  <ConnectedClustersTable />
                </Suspense>
              </CardBody>
            </ExpandableCard>
          </StackItem>
          <StackItem>
            <ExpandableCard title={"Recently viewed topics"}>
              <CardBody>
                <Suspense fallback={<TopicsTable topics={undefined} />}>
                  <RecentTopics />
                </Suspense>
              </CardBody>
            </ExpandableCard>
          </StackItem>
          <StackItem>
            <ExpandableCard
              title={"Getting started"}
              collapsedTitle={
                <Level hasGutter>
                  <CardTitle id="titleId">Getting Started</CardTitle>
                  <LabelGroup isCompact>
                    <Label isCompact icon={<InfoCircleIcon />} color="blue">
                      Documentation
                    </Label>
                    <Label isCompact icon={<InfoCircleIcon />} color="green">
                      Quick starts
                    </Label>
                    <Label isCompact icon={<InfoCircleIcon />} color="orange">
                      Learning resources
                    </Label>
                  </LabelGroup>
                </Level>
              }
            >
              <CardBody>
                <Grid md={6} lg={4} hasGutter>
                  <Flex
                    spaceItems={{ default: "spaceItemsLg" }}
                    alignItems={{ default: "alignItemsFlexStart" }}
                    direction={{ default: "column" }}
                  >
                    <Flex
                      spaceItems={{ default: "spaceItemsSm" }}
                      alignItems={{ default: "alignItemsFlexStart" }}
                      direction={{ default: "column" }}
                      grow={{ default: "grow" }}
                    >
                      <Label icon={<InfoCircleIcon />} color="blue">
                        Documentation
                      </Label>
                      <p>Getting started with AMQ Streams</p>
                      <List isPlain>
                        <ListItem>
                          <a href="#">Add a AMQ Streams Cluster</a>
                        </ListItem>
                        <ListItem>
                          <a href="#">
                            Make an AMQ Streams Cluster discoverable by the AMQ
                            Streams Console
                          </a>
                        </ListItem>
                        <ListItem>
                          <a href="#">Delete an AMQ Streams Cluster</a>
                        </ListItem>
                      </List>
                    </Flex>
                    <Button
                      href="#"
                      component="a"
                      variant="link"
                      isInline
                      icon={<ArrowRightIcon />}
                      iconPosition="end"
                    >
                      View all AMQ Streams documentation
                    </Button>
                  </Flex>
                  <Flex
                    spaceItems={{ default: "spaceItemsLg" }}
                    alignItems={{ default: "alignItemsFlexStart" }}
                    direction={{ default: "column" }}
                  >
                    <Flex
                      spaceItems={{ default: "spaceItemsSm" }}
                      alignItems={{ default: "alignItemsFlexStart" }}
                      direction={{ default: "column" }}
                      grow={{ default: "grow" }}
                    >
                      <Label icon={<InfoCircleIcon />} color="green">
                        Quick starts
                      </Label>
                      <p>
                        Get started with features using our step-by-step
                        documentation
                      </p>
                      <List isPlain>
                        <ListItem>
                          <a href="#">
                            Getting started with AMQ Streams Console
                          </a>
                        </ListItem>
                        <ListItem>
                          <a href="#">Explore the Message Browser</a>
                        </ListItem>
                        <ListItem>
                          <a href="#">
                            Connect to the Cluster from an application
                          </a>
                        </ListItem>
                      </List>
                    </Flex>
                    <Button
                      href="#"
                      component="a"
                      variant="link"
                      isInline
                      icon={<ArrowRightIcon />}
                      iconPosition="end"
                    >
                      View all quick starts
                    </Button>
                  </Flex>
                  <Flex
                    spaceItems={{ default: "spaceItemsLg" }}
                    alignItems={{ default: "alignItemsFlexStart" }}
                    direction={{ default: "column" }}
                  >
                    <Flex
                      spaceItems={{ default: "spaceItemsSm" }}
                      alignItems={{ default: "alignItemsFlexStart" }}
                      direction={{ default: "column" }}
                      grow={{ default: "grow" }}
                    >
                      <Label icon={<InfoCircleIcon />} color="orange">
                        Learning resources
                      </Label>
                      <p>
                        Learn about new features within the Console and get
                        started with demo apps
                      </p>
                      <List isPlain>
                        <ListItem>
                          <a href="#">
                            See what&quot;s possible with the Explore page
                          </a>
                        </ListItem>
                        <ListItem>
                          <a href="#">
                            AMQ Streams 1.2.3: Changelog&nbsp;
                            <ExternalLinkAltIcon />
                          </a>
                        </ListItem>
                      </List>
                    </Flex>
                    <Button
                      href="#"
                      component="a"
                      variant="link"
                      isInline
                      icon={<ArrowRightIcon />}
                      iconPosition="end"
                    >
                      View all learning resources
                    </Button>
                  </Flex>
                </Grid>{" "}
              </CardBody>
            </ExpandableCard>
          </StackItem>
        </Stack>
      </PageSection>
    </>
  );
}

async function ConnectedClustersTable() {
  const allClusters = await getKafkaClusters();
  const clusters = (await Promise.all(
    allClusters.map((c) => getKafkaCluster(c.id)),
  )) as ClusterDetail[];

  return <ClustersTable clusters={clusters} />;
}

async function RecentTopics() {
  const viewedTopics = await getViewedTopics();
  return viewedTopics.length > 0 ? (
    <TopicsTable topics={viewedTopics} />
  ) : (
    <EmptyState variant={"xs"}>
      <EmptyStateBody>
        You haven&quot;t viewed any topic, yet. Topics you view will be listed
        here.
      </EmptyStateBody>
    </EmptyState>
  );
}

async function ChangedTopics() {
  const dull = await new Promise(() => {});
  return <div>eventually</div>;
}
