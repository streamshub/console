"use client";
import { DateTime } from "@/components/Format/DateTime";
import { Number } from "@/components/Format/Number";
import {
  Button,
  Card,
  CardBody,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Divider,
  Flex,
  FlexItem,
  Grid,
  GridItem,
  Icon,
  Skeleton,
  Text,
  TextContent,
  Title,
  Truncate,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
} from "@/libs/patternfly/react-icons";
import { Link } from "@/i18n/routing";
import { useTranslations } from "next-intl";
import { ErrorsAndWarnings } from "./components/ErrorsAndWarnings";
import { updateKafkaCluster } from "@/api/kafka/actions";
import { useReconciliationContext } from "../ReconciliationContext";
import { ReconciliationPauseButton } from "./ReconciliationPauseButton";

type ClusterCardProps = {
  name: string;
  status: string;
  brokersOnline?: number;
  brokersTotal?: number;
  consumerGroups?: number;
  kafkaVersion: string;
  kafkaId: string | undefined;
  messages: Array<{
    variant: "danger" | "warning";
    subject: { type: string; name: string; id: string };
    message: string;
    date: string;
  }>;
  managed: boolean;
};

export function ClusterCard({
  isLoading,
  name,
  status,
  brokersOnline,
  brokersTotal,
  consumerGroups,
  kafkaVersion,
  messages,
  kafkaId,
  managed,
}:
  | ({
      isLoading: false;
    } & ClusterCardProps)
  | ({ isLoading: true } & { [K in keyof ClusterCardProps]?: undefined })) {
  const t = useTranslations();

  const { isReconciliationPaused, setReconciliationPaused } =
    useReconciliationContext();

  const resumeReconciliation = async () => {
    if (!kafkaId) {
      console.log("kafkaId is undefined");
      return;
    }

    try {
      const response = await updateKafkaCluster(kafkaId, false);

      if (response.errors) {
        console.log("Unknown error occurred", response.errors);
      } else {
        setReconciliationPaused(false);
      }
    } catch (e: unknown) {
      console.log("Unknown error occurred");
    }
  };

  const warnings =
    messages?.filter((m) => m.variant === "warning").length ||
    0 + (isReconciliationPaused ? 1 : 0);
  const dangers = messages?.filter((m) => m.variant === "danger").length || 0;

  return (
    <Card component={"div"}>
      <CardBody>
        <Flex>
          <FlexItem>
            <Title headingLevel={"h2"}>
              {t("ClusterCard.Kafka_cluster_details")}
            </Title>
          </FlexItem>
          <FlexItem align={{ default: "alignRight" }}>
            <ReconciliationPauseButton
              clusterId={kafkaId || ""}
              managed={managed || false}
            />
          </FlexItem>
        </Flex>
        <Flex direction={{ default: "column" }} gap={{ default: "gap" }}>
          <Flex
            flexWrap={{ default: "wrap", sm: "nowrap" }}
            gap={{ default: "gap" }}
            justifyContent={{ default: "justifyContentCenter" }}
            style={{ textAlign: "center" }}
          >
            <Flex
              direction={{ default: "column" }}
              alignSelf={{ default: "alignSelfCenter" }}
              style={{ minWidth: "200px" }}
            >
              <FlexItem className={"pf-v5-u-py-md"}>
                {isLoading ? (
                  <>
                    <Icon status={"success"}>
                      <Skeleton shape={"circle"} width={"1rem"} />
                    </Icon>

                    <Skeleton width={"100%"} />
                    <Skeleton
                      width={"50%"}
                      height={"0.7rem"}
                      style={{ display: "inline-block" }}
                    />
                  </>
                ) : (
                  <>
                    <Icon status={"success"}>
                      {status ? (
                        <CheckCircleIcon />
                      ) : (
                        <Skeleton shape={"circle"} width={"1rem"} />
                      )}
                    </Icon>

                    <Title headingLevel={"h2"}>{name}</Title>

                    <TextContent>
                      <Text component={"small"}>{status}</Text>
                    </TextContent>
                  </>
                )}
              </FlexItem>
            </Flex>
            <Divider orientation={{ default: "horizontal", sm: "vertical" }} />
            <Flex
              direction={{ default: "column" }}
              alignSelf={{ default: "alignSelfCenter" }}
              flex={{ default: "flex_1" }}
              style={{ minWidth: "200px", textAlign: "center" }}
            >
              <Grid>
                <GridItem span={12} xl={4}>
                  <Link className="pf-v5-u-font-size-xl" href={"./nodes"}>
                    {isLoading ? (
                      <Skeleton
                        shape={"circle"}
                        width={"1.5rem"}
                        style={{ display: "inline-block" }}
                      />
                    ) : (
                      <>
                        <Number value={brokersOnline} />
                        /
                        <Number value={brokersTotal} />
                      </>
                    )}
                  </Link>
                  <TextContent>
                    <Text component={"small"}>
                      {t("ClusterCard.online_brokers")}
                    </Text>
                  </TextContent>
                </GridItem>
                <GridItem span={12} xl={4}>
                  <Link
                    className="pf-v5-u-font-size-xl"
                    href={"./consumer-groups"}
                  >
                    {isLoading ? (
                      <Skeleton
                        shape={"circle"}
                        width={"1.5rem"}
                        style={{ display: "inline-block" }}
                      />
                    ) : (
                      <Number value={consumerGroups} />
                    )}
                  </Link>
                  <TextContent>
                    <Text component={"small"}>
                      {t("ClusterCard.consumer_groups")}
                    </Text>
                  </TextContent>
                </GridItem>
                <GridItem span={12} xl={4}>
                  <div className="pf-v5-u-font-size-xl">
                    {isLoading ? <Skeleton /> : kafkaVersion}
                  </div>
                  <TextContent>
                    <Text component={"small"}>
                      {t("ClusterCard.kafka_version")}
                    </Text>
                  </TextContent>
                </GridItem>
              </Grid>
            </Flex>
          </Flex>
          <Divider />
          <FlexItem>
            <ErrorsAndWarnings warnings={warnings} dangers={dangers}>
              <DataList
                aria-label={t("ClusterCard.cluster_errors_and_warnings")}
                isCompact={true}
              >
                {isLoading ? (
                  new Array(5).fill(0).map((_, i) => (
                    <DataListItem key={i}>
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="name">
                              <Skeleton />
                            </DataListCell>,
                            <DataListCell key="message">
                              <Skeleton />
                            </DataListCell>,
                            <DataListCell key="date">
                              <Skeleton />
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  ))
                ) : (
                  <>
                    {messages.length === 0 && (
                      <DataListItem aria-labelledby={`no-messages`}>
                        <DataListItemRow>
                          <DataListItemCells
                            dataListCells={[
                              <DataListCell key="name">
                                <span id={"no-messages"}>
                                  {t("ClusterCard.no_messages")}
                                </span>
                              </DataListCell>,
                            ]}
                          />
                        </DataListItemRow>
                      </DataListItem>
                    )}
                    {messages
                      .sort((a, b) => a.date.localeCompare(b.date))
                      .reverse()
                      .map((m, i) => (
                        <DataListItem aria-labelledby={`message-${i}`} key={i}>
                          <DataListItemRow>
                            <DataListItemCells
                              dataListCells={[
                                <DataListCell
                                  key="name"
                                  className={"pf-v5-u-text-nowrap"}
                                  width={1}
                                >
                                  <Icon status={m.variant}>
                                    {m.variant === "danger" && (
                                      <ExclamationCircleIcon />
                                    )}
                                    {m.variant === "warning" && (
                                      <ExclamationTriangleIcon />
                                    )}
                                  </Icon>
                                  &nbsp;
                                  {m.subject.type}
                                </DataListCell>,
                                <DataListCell key="message" width={5}>
                                  <div
                                    className={
                                      "pf-v5-u-display-none pf-v5-u-display-block-on-md"
                                    }
                                  >
                                    <Truncate
                                      content={
                                        m.subject.type ===
                                        "ReconciliationPaused"
                                          ? t(
                                              "reconciliation.reconciliation_paused_warning",
                                            )
                                          : m.message
                                      }
                                    />
                                    {m.subject.type ===
                                      "ReconciliationPaused" && (
                                      <>
                                        &nbsp;
                                        <Button
                                          variant="link"
                                          isInline
                                          onClick={resumeReconciliation}
                                        >
                                          {t("reconciliation.resume")}
                                        </Button>
                                      </>
                                    )}
                                  </div>
                                  <div
                                    className={
                                      "pf-v5-u-display-block pf-v5-u-display-none-on-md"
                                    }
                                  >
                                    {m.message}
                                  </div>
                                </DataListCell>,
                                <DataListCell
                                  key="date"
                                  width={1}
                                  className={"pf-v5-u-text-nowrap"}
                                >
                                  <DateTime
                                    value={m.date}
                                    tz={"UTC"}
                                    dateStyle={"short"}
                                    timeStyle={"short"}
                                  />
                                </DataListCell>,
                              ]}
                            />
                          </DataListItemRow>
                        </DataListItem>
                      ))}
                  </>
                )}
              </DataList>
            </ErrorsAndWarnings>
          </FlexItem>
        </Flex>
      </CardBody>
    </Card>
  );
}
