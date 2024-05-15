import { useTranslations } from "next-intl";
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
import { Number } from "@/components/Format/Number";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
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
  const t = useTranslations();
  const allClusters = getKafkaClusters();

  const productName = t('common.product');
  const brand = t("common.brand")
  return (
    <>
      <PageSection padding={{ default: "noPadding" }} variant={"light"}>
        <div className={styles.hero}>
          <div>
            <TextContent>
              <Title headingLevel={"h1"} size={"2xl"}>
                {t.rich('homepage.page_header', { product: productName })}
              </Title>
              <Text className={"pf-v5-u-color-200"}>
                {t('homepage.page_subtitle', { brand: brand, product: productName })}
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
                  {t.rich('homepage.platform_openshift_cluster')}
                  <Text component={"small"}>
                    <Suspense fallback={<Skeleton width={"200px"} />}>
                      <ClustersCount clusterPromise={allClusters} />
                      &nbsp;{t('homepage.connected_kafka_clusters')}
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
                    {t("homepage.recently_viewed_topics_header")} {" "}
                    <Tooltip
                      content={
                        t("homepage.recently_viewed_topics_header_popover", { product: productName })
                      }
                    >
                      <HelpIcon />
                    </Tooltip>
                  </b>
                  <Text component={"small"}>
                    {t('homepage.last_accessed_topics', { product: productName })}
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
                      {t.rich("homepage.recommended_learning_resources")}
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
                          {t.rich("homepage.recommended_learning_resources")}
                        </TextContent>
                      </StackItem>
                      <StackItem>
                        <LabelGroup isCompact>
                          <Label isCompact color="orange">
                            {t('homepage.documentation')}
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
                              {t('homepage.openshift_overview', { product: productName })}
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-1-2">
                            <Label isCompact={true} color={"orange"}>
                              {t('homepage.documentation')}
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-1-3">
                            <ExternalLink testId={"gs-1-3"} href={t("learning.links.overview")}>
                              {t('homepage.view_documentation')}
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
                              {t('homepage.get_started_with_openshift', { product: productName })}
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-2-2">
                            <Label isCompact={true} color={"orange"}>
                              {t('homepage.documentation')}
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-2-3">
                            <ExternalLink testId={"gs-2-3"} href={t("learning.links.gettingStarted")}>
                              {t('homepage.view_documentation')}
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
                              {t('homepage.connect_to_kafka_cluster')}
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-3-2">
                            <Label isCompact={true} color={"orange"}>
                              {t('homepage.documentation')}
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-3-3">
                            <ExternalLink testId={"gs-3-3"} href={t("learning.links.connecting")}>
                              {t('homepage.view_documentation')}
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
                              {t('homepage.using_topic_operator_to_manage_kafka_topic')}
                            </span>
                          </DataListCell>,
                          <DataListCell key="gs-4-2">
                            <Label isCompact={true} color={"orange"}>
                              {t('homepage.documentation')}
                            </Label>
                          </DataListCell>,
                          <DataListCell key="gs-4-3">
                            <ExternalLink testId={"gs-4-3"} href={t("learning.links.topicOperatorUse")}>
                              {t('homepage.view_documentation')}
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
      let cluster;

      if (c.meta.configured === true) {
        cluster = await getKafkaCluster(c.id);
      } else {
        cluster = null;
      }

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
      let cg;

      if (c.meta.configured === true) {
        cg = await getConsumerGroups(c.id, {});
      } else {
        cg = null;
      }

      return cg?.meta.page.total ?? 0;
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
  const t = useTranslations();
  const productName = t('common.product');
  const viewedTopics = await getViewedTopics();
  return viewedTopics.length > 0 ? (
    <TopicsTable topics={viewedTopics} />
  ) : (
    <EmptyState variant={"xs"}>
      <EmptyStateHeader title={"No topics were viewed yet"} />
      <EmptyStateBody>
        {t("homepage.empty_topics_description", { product: productName })}
      </EmptyStateBody>
      <EmptyStateFooter>
        <EmptyStateActions className={"pf-v5-u-font-size-sm"}>
          <ExternalLink testId={"recent-topics-empty-state-link"} href={t("learning.links.topicOperatorUse")}>
            {t("homepage.topic_operator_link")}
          </ExternalLink>
        </EmptyStateActions>
      </EmptyStateFooter>
    </EmptyState>
  );
}
