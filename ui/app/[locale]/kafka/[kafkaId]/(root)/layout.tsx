import { getKafkaCluster } from "@/api/kafka";
import { getResources } from "@/api/resources";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
import {
  Breadcrumb,
  BreadcrumbItem,
  Divider,
  Nav,
  NavList,
  PageBreadcrumb,
  PageGroup,
  PageNavigation,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, ReactNode, Suspense } from "react";
import { KafkaSelectorBreadcrumbItem } from "./KafkaSelectorBreadcrumbItem";

export default async function KafkaLayout({
  children,
  activeBreadcrumb,
  params: { kafkaId },
}: PropsWithChildren<{ params: KafkaParams; activeBreadcrumb: ReactNode }>) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getResources("kafka");

  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem isActive={activeBreadcrumb === null}>
              <Suspense fallback={"Loading clusters..."}>
                <KafkaSelectorBreadcrumbItem
                  selected={cluster}
                  clusters={clusters}
                  isActive={activeBreadcrumb === null}
                />
              </Suspense>
            </BreadcrumbItem>
            {activeBreadcrumb && (
              <BreadcrumbItem>{activeBreadcrumb}</BreadcrumbItem>
            )}
          </Breadcrumb>
        </PageBreadcrumb>
        <PageSection
          variant={"light"}
          padding={{ default: "noPadding" }}
          className={"pf-v5-u-px-lg pf-v5-u-pt-sm"}
        >
          <Title headingLevel={"h1"}>{cluster.attributes.name}</Title>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              {/*<NavItemLink url={`/kafka/${kafkaId}/overview`}>*/}
              {/*  Overview*/}
              {/*</NavItemLink>*/}
              <NavItemLink url={`/kafka/${kafkaId}/brokers`}>
                Brokers
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/topics`}>Topics</NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/schema-registry`}>
                Schema registry
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/consumer-groups`}>
                Consumer groups
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
