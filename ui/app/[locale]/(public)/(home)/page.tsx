import { getKafkaClusters } from "@/api/kafka/actions";
import { AppLayout } from "@/components/AppLayout";
import { ClusterColumn } from "@/components/ClustersTable";
import { ExpandableCard } from "@/components/ExpandableCard";
import { Number } from "@/components/Format/Number";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";
import {
  Card,
  CardBody,
  CardTitle,
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
} from "@/libs/patternfly/react-core";
import { getTranslations } from "next-intl/server";
import { Suspense } from "react";
import styles from "./home.module.css";
import config from "@/utils/config";
import { logger } from "@/utils/logger";
import { getAuthOptions } from "@/app/api/auth/[...nextauth]/auth-options";
import { getServerSession } from "next-auth";
import { stringToInt } from "@/utils/stringToInt";
import { ConnectedClusterTable } from "./ConnectedClustersTable";

const log = logger.child({ module: "home" });

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("homepage.title")} | ${t("common.title")}`,
  };
}

export default async function Home({
  searchParams,
}: {
  searchParams: {
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  };
}) {
  const t = await getTranslations();
  log.trace("fetching known Kafka clusters...");
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as ClusterColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];

  log.trace(
    "fetching known Kafka clusters inside AsyncConnectedClusterTable...",
  );
  const response = await getKafkaClusters(undefined, {
    sort,
    sortDir,
    pageSize,
    pageCursor,
  });

  if (!response?.payload) {
    log.trace("No cluster data received in AsyncConnectedClusterTable.");
    return (
      <ConnectedClusterTable
        clusters={undefined}
        clusterCount={0}
        page={0}
        perPage={pageSize}
        sort={sort}
        sortDir={sortDir}
        nextPageCursor={undefined}
        prevPageCursor={undefined}
        authenticated={false}
      />
    );
  }

  const allClusters = response.payload;
  const nextPageCursor = allClusters?.links.next
    ? new URLSearchParams(allClusters.links.next).get("page[after]")
    : undefined;

  console.log("next page cursor", nextPageCursor);
  const prevPageCursor = allClusters?.links.prev
    ? new URLSearchParams(allClusters.links.prev).get("page[before]")
    : undefined;
  log.trace(
    `fetched ${allClusters.data.length} clusters in AsyncConnectedClusterTable, total: ${allClusters.meta.page.total}`,
  );
  const productName = t("common.product");
  const brand = t("common.brand");
  log.trace("fetching configuration");
  let cfg = await config();
  log.trace(`fetched configuration: ${cfg ? "yes" : "no"}`);
  let oidcCfg = cfg?.security?.oidc ?? null;
  let oidcEnabled = !!oidcCfg;
  let username: string | undefined;

  if (oidcEnabled) {
    const authOptions = await getAuthOptions();
    const session = await getServerSession(authOptions);
    username = session?.user?.name ?? session?.user?.email ?? undefined;
  }

  if (allClusters?.data.length === 1 && !oidcEnabled) {
    return <RedirectOnLoad url={`/kafka/${allClusters?.data[0].id}/login`} />;
  }

  const showLearning = cfg.showLearning;

  return (
    <AppLayout username={username}>
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
            <Card isCompact={true}>
              <CardTitle>
                <TextContent>
                  {t.rich("homepage.platform_openshift_cluster")}
                  <Text component={"small"}>
                    <Suspense fallback={<Skeleton width={"200px"} />}>
                      <Number value={allClusters?.data.length} />
                      &nbsp;{t("homepage.connected_kafka_clusters")}
                    </Suspense>
                  </Text>
                </TextContent>
              </CardTitle>
              <CardBody>
                <Suspense
                  fallback={
                    <ConnectedClusterTable
                      clusters={undefined}
                      clusterCount={0}
                      page={1}
                      perPage={pageSize}
                      sort={sort}
                      sortDir={sortDir}
                      nextPageCursor={undefined}
                      prevPageCursor={undefined}
                      authenticated={oidcEnabled}
                    />
                  }
                >
                  <ConnectedClusterTable
                    clusters={allClusters.data}
                    clusterCount={allClusters.meta.page.total || 0}
                    page={allClusters.meta.page.pageNumber || 0}
                    perPage={pageSize}
                    sort={sort}
                    sortDir={sortDir}
                    nextPageCursor={nextPageCursor}
                    prevPageCursor={prevPageCursor}
                    authenticated={oidcEnabled}
                  />
                </Suspense>
              </CardBody>
            </Card>
          </StackItem>
          <StackItem></StackItem>
          {showLearning && (
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
