import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { getKafkaCluster, getKafkaClusters } from "@/api/kafka/actions";
import { ClusterList } from "@/api/kafka/schema";
import { getViewedTopics } from "@/api/topics/actions";
import {
  ClustersTable,
  EnrichedClusterList,
} from "@/app/[locale]/home/ClustersTable";
import { ExpandableCard } from "@/app/[locale]/home/ExpandableCard";
import { TopicsTable } from "@/app/[locale]/home/TopicsTable";
import { ExternalLink } from "@/components/ExternalLink";
import { Number } from "@/components/Number";
import {
  CardBody,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  Label,
  LabelGroup,
  Level,
  LevelItem,
  PageSection,
  Skeleton,
  Stack,
  StackItem,
  Text,
  TextContent,
  Title,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { Suspense } from "react";
import styles from "./home.module.css";

export default function Home() {
  const allClusters = getKafkaClusters();
  return (
    <>
      <PageSection padding={{ default: "noPadding" }} variant={"light"}>
        <div className={styles.hero}>
          <div>
            <TextContent>
              <Title headingLevel={"h1"} size={"2xl"}>
                Welcome to the AMQ streams console
              </Title>
              <Text className={"pf-v5-u-color-200"}>
                The Red Hat AMQ Streams console provides a user interface for managing
                and monitoring your streaming resources
              </Text>
            </TextContent>
          </div>
        </div>
      </PageSection>
      <PageSection>
        <Stack hasGutter={true}>
          <StackItem>
            <ExpandableCard
              title={
                <TextContent>
                  <strong>Platform: OpenShift Cluster</strong>
                  <Text component={"small"}>
                    <Suspense fallback={<Skeleton width={"200px"} />}>
                      <ClustersCount clusterPromise={allClusters} />
                      &nbsp;Connected Kafka clusters
                    </Suspense>
                  </Text>
                </TextContent>
              }
              isCompact={true}
            >
              <CardBody>
                <Suspense fallback={<ClustersTable clusters={undefined} />}>
                  <ConnectedClustersTable clusterPromise={allClusters} />
                </Suspense>
              </CardBody>
            </ExpandableCard>
          </StackItem>
          <StackItem>
            <ExpandableCard
              title={
                <TextContent>
                  <b>
                    Recently viewed topics{" "}
                    <Tooltip
                      content={
                        "When you start looking at specific topics through the AMQ Streams console, they'll start showing here."
                      }
                    >
                      <HelpIcon />
                    </Tooltip>
                  </b>
                  <Text component={"small"}>
                  The last 5 topics this account has accessed from the AMQ 
                  Streams console.
                  </Text>
                </TextContent>
              }
              isCompact={true}
            >
              <CardBody>
                <Suspense fallback={<TopicsTable topics={undefined} />}>
                  <RecentTopics />
                </Suspense>
              </CardBody>
            </ExpandableCard>
          </StackItem>
          <StackItem>
            <ExpandableCard
              title={
                <Level>
                  <LevelItem>
                    <TextContent>
                      <b>Recommended learning resources</b>
                    </TextContent>
                  </LevelItem>
                  {/*<LevelItem>*/}
                  {/*  <Link href={"/learning-resources"}>View all</Link>*/}
                  {/*</LevelItem>*/}
                </Level>
              }
              collapsedTitle={
                <Level>
                  <LevelItem>
                    <Stack>
                      <StackItem>
                        <TextContent>
                          <b>Recommended learning resources</b>
                        </TextContent>
                      </StackItem>
                      <StackItem>
                        <LabelGroup isCompact>
                          <Label isCompact color="orange">
                            Documentation
                          </Label>
                          {/*<Label isCompact icon={<InfoCircleIcon />} color="green">*/}
                          {/*  Quick starts*/}
                          {/*</Label>*/}
                          {/*<Label isCompact icon={<InfoCircleIcon />} color="orange">*/}
                          {/*  Learning resources*/}
                          {/*</Label>*/}
                        </LabelGroup>
                      </StackItem>
                    </Stack>
                  </LevelItem>
                  {/*<LevelItem>*/}
                  {/*  <Link href={"/learning-resources"}>View all</Link>*/}
                  {/*</LevelItem>*/}
                </Level>
              }
              isCompact={true}
            >
              <CardBody>
                <DataList aria-label="Reccomended learning resources">
                  <DataListItem aria-labelledby="gs-1-1">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="gs-1-1" width={2}>
                            <span id="gs-1-1">
                              AMQ Streams on OpenShift Overview
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-1-2">
                            <Label isCompact={true} color={"orange"}>
                              Documentation
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-1-3">
                            <ExternalLink
                              testId={"gs-1-3"}
                              href={
                                "https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/html/amq_streams_on_openshift_overview"
                              }
                            >
                              View documentation
                            </ExternalLink>
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="gs-2-1">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="gs-2-1" width={2}>
                            <span id="gs-2-1">
                              Getting Started with AMQ Streams on Openshift
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-2-2">
                            <Label isCompact={true} color={"orange"}>
                              Documentation
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-2-3">
                            <ExternalLink
                              testId={"gs-2-3"}
                              href={
                                "https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/html/getting_started_with_amq_streams_on_openshift"
                              }
                            >
                              View documentation
                            </ExternalLink>
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="gs-3-1">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="gs-3-1" width={2}>
                            <span id="gs-3-1">
                              Connect to a Kafka cluster from an application
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-3-2">
                            <Label isCompact={true} color={"orange"}>
                              Documentation
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-3-3">
                            <ExternalLink
                              testId={"gs-3-3"}
                              href={
                                "https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/html/developing_kafka_client_applications"
                              }
                            >
                              View documentation
                            </ExternalLink>
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="gs-4-1">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="gs-4-1" width={2}>
                            <span id="gs-4-1">
                              Using the Topic Operator to manage Kafka topics
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-4-2">
                            <Label isCompact={true} color={"orange"}>
                              Documentation
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-4-3">
                            <ExternalLink
                              testId={"gs-4-3"}
                              href={
                                "https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/html/deploying_and_managing_amq_streams_on_openshift/using-the-topic-operator-str#doc-wrapper"
                              }
                            >
                              View documentation
                            </ExternalLink>
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                </DataList>
              </CardBody>
            </ExpandableCard>
          </StackItem>
        </Stack>
      </PageSection>
    </>
  );
}

async function ClustersCount({
  clusterPromise,
}: {
  clusterPromise: Promise<ClusterList[]>;
}) {
  const count = (await clusterPromise).length;
  return <Number value={count} />;
}

async function ConnectedClustersTable({
  clusterPromise,
}: {
  clusterPromise: Promise<ClusterList[]>;
}) {
  const allClusters = await clusterPromise;
  const clusters = allClusters.map<EnrichedClusterList>((c) => {
    async function getNodesCounts() {
      const cluster = await getKafkaCluster(c.id);
      if (cluster) {
        return {
          count: cluster.attributes.nodes.length,
          online: cluster.attributes.nodes.length, // TODO,
        };
      }
      return {
        count: 0,
        online: 0,
      };
    }

    async function getConsumerGroupsCount() {
      const cg = await getConsumerGroups(c.id, {});
      return cg.meta.page.total || 0;
    }

    const ec: EnrichedClusterList = {
      ...c,
      extra: {
        nodes: getNodesCounts(),
        consumerGroupsCount: getConsumerGroupsCount(),
      },
    };
    return ec;
  });
  return <ClustersTable clusters={clusters} />;
}

async function RecentTopics() {
  const viewedTopics = await getViewedTopics();
  return viewedTopics.length > 0 ? (
    <TopicsTable topics={viewedTopics} />
  ) : (
    <EmptyState variant={"xs"}>
      <EmptyStateHeader title={"No topics were viewed yet"} />
      <EmptyStateBody>
        When you start looking at specific topics through the AMQ Streams
        console, they&quot;ll start showing here.
      </EmptyStateBody>
      <EmptyStateFooter>
        <EmptyStateActions className={"pf-v5-u-font-size-sm"}>
          <ExternalLink
            testId={"recent-topics-empty-state-link"}
            href={
              "https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/html-single/deploying_and_managing_amq_streams_on_openshift/index#ref-operator-topic-str"
            }
          >
            Using the Topic Operator to manage Kafka topics
          </ExternalLink>
        </EmptyStateActions>
      </EmptyStateFooter>
    </EmptyState>
  );
}
