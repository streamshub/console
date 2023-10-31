import { getKafkaCluster, getKafkaClusters } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { KafkaBreadcrumbItem } from "@/app/[locale]/kafka/[kafkaId]/KafkaBreadcrumbItem";
import {
  Breadcrumb,
  BreadcrumbItem,
  PageBreadcrumb,
  PageGroup,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, ReactNode } from "react";

export default async function KafkaLayout({
  children,
  activeBreadcrumb,
  header,
  modal,
  params: { kafkaId },
}: PropsWithChildren<{
  params: KafkaParams;
  header: ReactNode;
  activeBreadcrumb: ReactNode;
  modal: ReactNode;
}>) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getKafkaClusters();

  return (
    <>
      <PageGroup stickyOnBreakpoint={{ default: "top" }}>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbItem>Kafka clusters</BreadcrumbItem>
            <BreadcrumbItem>
              <KafkaBreadcrumbItem
                selected={cluster}
                clusters={clusters}
                isActive={activeBreadcrumb === null}
              />
            </BreadcrumbItem>
            {activeBreadcrumb}
          </Breadcrumb>
        </PageBreadcrumb>
        {header}
      </PageGroup>
      {children}
      {modal}
    </>
  );
}
