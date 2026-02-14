import { getKafkaClusters } from "@/api/kafka/actions";
import { getMetadata } from "@/api/meta/actions";
import { AppLayout } from "@/components/AppLayout";
import { ClusterTableColumn } from "@/components/ClustersTable";
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
  Content,
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
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectedClustersTable } from "./ConnectedClustersTable";
import RichText from "@/components/RichText";

const log = logger.child({ module: "home" });

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("homepage.title")} | ${t("common.title")}`,
  };
}

export default async function Home({
  searchParams: searchParamsPromise,
}: {
  searchParams: Promise<{
    name: string | undefined;
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  }>;
}) {
  const t = await getTranslations();
  const searchParams = await searchParamsPromise;

  const name = searchParams["name"];
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as ClusterTableColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];

  log.trace("fetching known Kafka clusters...");
  const response = await getKafkaClusters(undefined, {
    pageSize,
    pageCursor,
    sort,
    sortDir,
    name,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const allClusters = response.payload!;
  log.trace(`fetched ${allClusters?.data?.length ?? 0} Kafka clusters`);

  const nextPageCursor = allClusters.links.next
    ? `after:${new URLSearchParams(allClusters.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = allClusters.links.prev
    ? `before:${new URLSearchParams(allClusters.links.prev).get("page[before]")}`
    : undefined;

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

  if (
    allClusters?.data.length === 1 &&
    !oidcEnabled &&
    !Object.values(searchParams).some((p) => p !== undefined)
  ) {
    return <RedirectOnLoad url={`/kafka/${allClusters.data[0].id}/login`} />;
  }

  const showLearning = cfg.showLearning;
  const metadata = (await getMetadata()).payload ?? undefined;

  return (
    <AppLayout username={username} metadata={metadata}>
      <PageSection padding={{ default: "noPadding" }} variant={"default"}>
        <div className={styles.hero}>
          <div>
            <Content>
              <Title headingLevel={"h1"} size={"2xl"}>
                <RichText>
                  {(tags) =>
                    t.rich("homepage.page_header", {
                      ...tags,
                      product: productName,
                    })
                  }
                </RichText>
              </Title>
              <Content className={"pf-v6-u-color-200"}>
                {t("homepage.page_subtitle", {
                  brand: brand,
                  product: productName,
                })}
              </Content>
            </Content>
          </div>
        </div>
      </PageSection>
      <PageSection>
        <Stack hasGutter={true}>
          <StackItem>
            <Card isCompact={true}>
              <CardTitle>
                <Content>
                  <RichText>
                    {(tags) =>
                      t.rich("homepage.platform", {
                        ...tags,
                        platform: metadata?.attributes.platform ?? "Unknown"
                      })
                    }
                  </RichText>
                  <Content component={"small"}>
                    <Suspense fallback={<Skeleton width={"200px"} />}>
                      <Number value={allClusters.meta.page.total} />
                      &nbsp;{t("homepage.connected_kafka_clusters")}
                    </Suspense>
                  </Content>
                </Content>
              </CardTitle>
              <CardBody>
                <Suspense
                  fallback={
                    <ConnectedClustersTable
                      clusters={undefined}
                      authenticated={oidcEnabled}
                      clustersCount={0}
                      page={1}
                      perPage={pageSize}
                      sort={sort}
                      sortDir={sortDir}
                      nextPageCursor={undefined}
                      prevPageCursor={undefined}
                      name={name}
                    />
                  }
                >
                  <ConnectedClustersTable
                    clusters={allClusters.data}
                    clustersCount={allClusters.meta.page.total}
                    page={allClusters.meta.page.pageNumber || 1}
                    perPage={pageSize}
                    sort={sort}
                    sortDir={sortDir}
                    nextPageCursor={nextPageCursor}
                    prevPageCursor={prevPageCursor}
                    authenticated={oidcEnabled}
                    name={name}
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
                      <Content>
                        <RichText>
                          {(tags) =>
                            t.rich(
                              "homepage.recommended_learning_resources",
                              tags,
                            )
                          }
                        </RichText>
                      </Content>
                    </LevelItem>
                  </Level>
                }
                collapsedTitle={
                  <Level>
                    <LevelItem>
                      <Stack>
                        <StackItem>
                          <Content>
                            <RichText>
                              {(tags) =>
                                t.rich(
                                  "homepage.recommended_learning_resources",
                                  tags,
                                )
                              }
                            </RichText>
                          </Content>
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
