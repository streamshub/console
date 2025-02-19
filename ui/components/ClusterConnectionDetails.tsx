import { getKafkaCluster } from "@/api/kafka/actions";
import { ExpandableSection } from "@/components/ExpandableSection";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import {
  Badge,
  ClipboardCopy,
  List,
  ListItem,
  Text,
  TextContent,
} from "@/libs/patternfly/react-core";
import { useEffect, useState } from "react";
import { Divider, Stack, StackItem } from "@patternfly/react-core";
import { useTranslations } from "next-intl";
import { clientConfig as config } from '@/utils/config';

export async function ClusterConnectionDetails({
  clusterId,
}: {
  clusterId: string;
}) {
  const [showLearning, setShowLearning] = useState(false);

  useEffect(() => {
    config().then(cfg => {
      setShowLearning(cfg.showLearning);
    })
  }, []);

  const t = useTranslations();
  const data = (await getKafkaCluster(clusterId))?.payload;
  if (!data) {
    return null;
  }
  const listeners = data.attributes.listeners || [];
  const external = listeners.filter((l) => l.type !== "internal");
  const internal = listeners.filter((l) => l.type === "internal");
  return (
    <Stack>
      <StackItem isFilled={true}>
        <TextContent className={"pf-v5-u-p-lg"}>
          <Text>{t("ClusterConnectionDetails.description")}</Text>

          <ExpandableSection
            displaySize={"lg"}
            initialExpanded={true}
            toggleContent={
              <div>
                {t("ClusterConnectionDetails.external_servers_bootstraps")}{" "}
                <Badge isRead={true}>{external.length}</Badge>
              </div>
            }
          >
            <Text>
              {t(
                "ClusterConnectionDetails.external_servers_bootstraps_description",
              )}
            </Text>
            <List isPlain={true}>
              {external.map((l, idx) => (
                <ListItem key={idx} className={"pf-v5-u-py-sm"}>
                  <ClipboardCopy isReadOnly={true}>
                    {l.bootstrapServers ?? ""}
                  </ClipboardCopy>
                  <Text component={"small"}>
                    {/*Listener type: {l.type}*/}
                    {/*<br />*/}
                    {t("ClusterConnectionDetails.authentication_type")}{" "}
                    {l.authType || "none"}
                  </Text>
                </ListItem>
              ))}
            </List>
          </ExpandableSection>

          <ExpandableSection
            displaySize={"lg"}
            initialExpanded={true}
            toggleContent={
              <div>
                {t("ClusterConnectionDetails.internal_servers_bootstraps")}{" "}
                <Badge isRead={true}>{internal.length}</Badge>
              </div>
            }
            className={"pf-v5-u-mt-lg"}
          >
            <Text>
              {t(
                "ClusterConnectionDetails.internal_Servers_bootstraps_description",
              )}
            </Text>
            <List isPlain={true}>
              {internal.map((l, idx) => (
                <ListItem key={idx} className={"pf-v5-u-py-sm"}>
                  <ClipboardCopy isReadOnly={true}>
                    {l.bootstrapServers ?? ""}
                  </ClipboardCopy>
                  <Text component={"small"}>
                    {t("ClusterConnectionDetails.authentication_type")}{" "}
                    {l.authType || "none"}
                  </Text>
                </ListItem>
              ))}
            </List>

            <Text>
              {t(
                "ClusterConnectionDetails.when_you_have_established_a_connection",
              )}
            </Text>
          </ExpandableSection>
        </TextContent>
      </StackItem>
      {showLearning && (
        <StackItem>
          <Divider />
          <Stack hasGutter={true} className={"pf-v5-u-p-lg"}>
            {t("learning.links.connecting") && (
              <StackItem>
                <ExternalLink
                  testId={"drawer-footer-help-1"}
                  href={t("learning.links.connecting")}
                >
                  {t(
                    "ClusterConnectionDetails.developing_kafka_client_applications",
                  )}
                </ExternalLink>
              </StackItem>
            )}
            <StackItem>
              <ExternalLink
                testId={"drawer-footer-help-1"}
                href={t("learning.links.overview")}
              >
                {t("learning.labels.overview")}
              </ExternalLink>
            </StackItem>
          </Stack>
        </StackItem>
      )}
    </Stack>
  );
}
