import { getKafkaCluster } from "@/api/kafka/actions";
import { ExpandableSection } from "@/components/ExpandableSection";
import { ExternalLink } from "@/components/ExternalLink";
import {
  Badge,
  ClipboardCopy,
  List,
  ListItem,
  Text,
  TextContent,
} from "@/libs/patternfly/react-core";
import { Divider, Stack, StackItem } from "@patternfly/react-core";

export async function ClusterConnectionDetails({
  clusterId,
}: {
  clusterId: string;
}) {
  const data = await getKafkaCluster(clusterId);
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
          <Text>
            To connect to a Kafka cluster, add a bootstrap address and the
            properties to establish a secure connection to the configuration of
            your client application.
          </Text>

          <ExpandableSection
            displaySize={"lg"}
            initialExpanded={true}
            toggleContent={
              <div>
                External servers bootstraps{" "}
                <Badge isRead={true}>{external.length}</Badge>
              </div>
            }
          >
            <Text>
              External listeners provide client access to a Kafka cluster from
              outside the OpenShift cluster.
            </Text>
            <List isPlain={true}>
              {external.map((l, idx) => (
                <ListItem key={idx} className={"pf-v5-u-py-sm"}>
                  <ClipboardCopy isReadOnly={true}>
                    {l.bootstrapServers}
                  </ClipboardCopy>
                  <Text component={"small"}>
                    {/*Listener type: {l.type}*/}
                    {/*<br />*/}
                    Authentication type: {l.authType || "none"}
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
                Internal servers bootstraps{" "}
                <Badge isRead={true}>{internal.length}</Badge>
              </div>
            }
            className={"pf-v5-u-mt-lg"}
          >
            <Text>
              Internal listeners provide client access to a Kafka cluster only
              from within the OpenShift cluster.
            </Text>
            <List isPlain={true}>
              {internal.map((l, idx) => (
                <ListItem key={idx} className={"pf-v5-u-py-sm"}>
                  <ClipboardCopy isReadOnly={true}>
                    {l.bootstrapServers}
                  </ClipboardCopy>
                  <Text component={"small"}>
                    Authentication type: {l.authType || "none"}
                  </Text>
                </ListItem>
              ))}
            </List>

            <Text>
              When you have established a connection, you can begin consuming
              messages from Kafka topics or producing messages to them.
            </Text>
          </ExpandableSection>
        </TextContent>
      </StackItem>
      <StackItem>
        <Divider />
        <Stack hasGutter={true} className={"pf-v5-u-p-lg"}>
          <StackItem>
            <ExternalLink
              testId={"drawer-footer-help-1"}
              href={
                "https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/html/developing_kafka_client_applications/"
              }
            >
              Developing Kafka client applications
            </ExternalLink>
          </StackItem>
          <StackItem>
            <ExternalLink
              testId={"drawer-footer-help-1"}
              href={
                "https://access.redhat.com/documentation/en-us/red_hat_amq/7.7/html-single/amq_streams_on_openshift_overview/index"
              }
            >
              AMQ Streams portal
            </ExternalLink>
          </StackItem>
        </Stack>
      </StackItem>
    </Stack>
  );
}
