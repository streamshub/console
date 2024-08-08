import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { getKafkaCluster, getKafkaClusters } from "@/api/kafka/actions";
import { ClusterList } from "@/api/kafka/schema";
import { AppLayout } from "@/components/AppLayout";
import { Number } from "@/components/Format/Number";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import {
  CardBody,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
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
import { isProductizedBuild } from "@/utils/env";
import { useTranslations } from "next-intl";
import { Suspense } from "react";
import { ClustersTable } from "@/components/ClustersTable";
import { ExpandableCard } from "@/components/ExpandableCard";
import styles from "./home.module.css";

export default function Home() {
  const t = useTranslations();
  const allClusters = getKafkaClusters();
  const productName = t("common.product");
  const brand = t("common.brand");

  return (
    <AppLayout>
      <PageSection padding={{ default: "noPadding" }} variant={"light"}>
        <div className={styles.hero}>
          <div>
            <TextContent>
              <Title headingLevel={"h1"} size={"2xl"}>
                {t.rich("homepage.page_header", { product: productName })}
              </Title>
              <Text className={"pf-v5-u-color-200"}>
                {t("homepage.page_subtitle", {
                  brand: brand,
                  product: productName,
                })}
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
                  {t.rich("homepage.platform_openshift_cluster")}
                  <Text component={"small"}>
                    <Suspense fallback={<Skeleton width={"200px"} />}>
                      <ClustersCount clusterPromise={allClusters} />
                      &nbsp;{t("homepage.connected_kafka_clusters")}
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
          <StackItem></StackItem>
          {isProductizedBuild && (
            <StackItem>
              <ExpandableCard
                title={
                  <Level>
                    <LevelItem>
                      <TextContent>
                        {t.rich("homepage.recommended_learning_resources")}
                      </TextContent>
                    </LevelItem>
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
                              {t("homepage.documentation")}
                            </Label>
                          </LabelGroup>
                        </StackItem>
                      </Stack>
                    </LevelItem>
                  </Level>
                }
                isCompact={true}
              >
                <CardBody>
                  <DataList aria-label="Recommended learning resources">
                    <DataListItem aria-labelledby="gs-1-1">
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="gs-1-1" width={2}>
                              <span id="gs-1-1">
                                {t("learning.labels.overview")}
                              </span>
                            </DataListCell>,
                            <DataListCell key="gs-1-2">
                              <Label isCompact={true} color={"orange"}>
                                {t("homepage.documentation")}
                              </Label>
                            </DataListCell>,
                            <DataListCell key="gs-1-3">
                              <ExternalLink
                                testId={"gs-1-3"}
                                href={t("learning.links.overview")}
                              >
                                {t("homepage.view_documentation")}
                              </ExternalLink>
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                    {t("learning.links.gettingStarted") && (
                      <DataListItem aria-labelledby="gs-2-1">
                        <DataListItemRow>
                          <DataListItemCells
                            dataListCells={[
                              <DataListCell key="gs-2-1" width={2}>
                                <span id="gs-2-1">
                                  {t("learning.labels.gettingStarted")}
                                </span>
                              </DataListCell>,
                              <DataListCell key="gs-2-2">
                                <Label isCompact={true} color={"orange"}>
                                  {t("homepage.documentation")}
                                </Label>
                              </DataListCell>,
                              <DataListCell key="gs-2-3">
                                <ExternalLink
                                  testId={"gs-2-3"}
                                  href={t("learning.links.gettingStarted")}
                                >
                                  {t("homepage.view_documentation")}
                                </ExternalLink>
                              </DataListCell>,
                            ]}
                          />
                        </DataListItemRow>
                      </DataListItem>
                    )}
                    {t("learning.links.connecting") && (
                      <DataListItem aria-labelledby="gs-3-1">
                        <DataListItemRow>
                          <DataListItemCells
                            dataListCells={[
                              <DataListCell key="gs-3-1" width={2}>
                                <span id="gs-3-1">
                                  {t("learning.labels.connecting")}
                                </span>
                              </DataListCell>,
                              <DataListCell key="gs-3-2">
                                <Label isCompact={true} color={"orange"}>
                                  {t("homepage.documentation")}
                                </Label>
                              </DataListCell>,
                              <DataListCell key="gs-3-3">
                                <ExternalLink
                                  testId={"gs-3-3"}
                                  href={t("learning.links.connecting")}
                                >
                                  {t("homepage.view_documentation")}
                                </ExternalLink>
                              </DataListCell>,
                            ]}
                          />
                        </DataListItemRow>
                      </DataListItem>
                    )}
                    <DataListItem aria-labelledby="gs-4-1">
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="gs-4-1" width={2}>
                              <span id="gs-4-1">
                                {t("learning.labels.topicOperatorUse")}
                              </span>
                            </DataListCell>,
                            <DataListCell key="gs-4-2">
                              <Label isCompact={true} color={"orange"}>
                                {t("homepage.documentation")}
                              </Label>
                            </DataListCell>,
                            <DataListCell key="gs-4-3">
                              <ExternalLink
                                testId={"gs-4-3"}
                                href={t("learning.links.topicOperatorUse")}
                              >
                                {t("homepage.view_documentation")}
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
          )}
        </Stack>
      </PageSection>
    </AppLayout>
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
  const clusters = await clusterPromise;
  return <ClustersTable clusters={clusters} />;
}
