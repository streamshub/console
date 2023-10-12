import { getKafkaCluster, getKafkaClusters } from "@/api/kafka";
import { KafkaNodeParams } from "@/app/[locale]/kafka/[kafkaId]/nodes/kafkaNode.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
import {
  Breadcrumb,
  BreadcrumbItem,
  ClipboardCopy,
  Divider,
  Label,
  Nav,
  NavList,
  PageBreadcrumb,
  PageGroup,
  PageNavigation,
  PageSection,
  Split,
  SplitItem,
  Title,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, Suspense } from "react";
import { KafkaBreadcrumbItem } from "./KafkaBreadcrumbItem";

export default async function NodeDetails({
  children,
  params: { kafkaId, nodeId },
}: PropsWithChildren<{
  params: KafkaNodeParams;
}>) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getKafkaClusters();
  const node = cluster.attributes.nodes.find((n) => `${n.id}` === nodeId);
  if (!node) {
    notFound();
  }
  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem>
              <KafkaBreadcrumbItem selected={cluster} clusters={clusters} />
            </BreadcrumbItem>
            <BreadcrumbLink href={`/kafka/${kafkaId}/nodes`}>
              Nodes
            </BreadcrumbLink>
            <BreadcrumbItem isActive={true}>Node {node.id}</BreadcrumbItem>
          </Breadcrumb>
        </PageBreadcrumb>
        <PageSection
          variant={"light"}
          padding={{ default: "noPadding" }}
          className={"pf-v5-u-px-lg pf-v5-u-pt-sm"}
        >
          <Split hasGutter={true}>
            <SplitItem>
              <Title headingLevel={"h1"} className={"pf-v5-u-pb-sm"}>
                Node {node.id}
              </Title>
            </SplitItem>
            {cluster.attributes.controller.id === node.id && (
              <SplitItem>
                <Label color={"purple"}>Controller</Label>
              </SplitItem>
            )}
          </Split>

          <ClipboardCopy
            hoverTip="Copy"
            clickTip="Copied"
            variant="inline-compact"
            isBlock={true}
          >
            {node.host}:{node.port}
          </ClipboardCopy>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink
                url={`/kafka/${kafkaId}/nodes/${nodeId}/configuration`}
              >
                Configuration
              </NavItemLink>
            </NavList>
          </Nav>
        </PageNavigation>
      </PageGroup>
      <Divider />
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </>
  );
}
