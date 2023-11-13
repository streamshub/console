import { getKafkaCluster, getKafkaClusters } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { KafkaBreadcrumbItem } from "@/app/[locale]/kafka/[kafkaId]/KafkaBreadcrumbItem";
import {
  Breadcrumb,
  BreadcrumbItem,
  PageBreadcrumb,
  PageGroup,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, ReactNode, Suspense } from "react";

export default function KafkaLayout({
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
  return (
    <>
      <PageGroup stickyOnBreakpoint={{ default: "top" }}>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbItem>Kafka clusters</BreadcrumbItem>
            <BreadcrumbItem>
              <Suspense>
                <ConnectedKafkaBreadcrumbItem
                  kafkaId={kafkaId}
                  isActive={activeBreadcrumb === null}
                />
              </Suspense>
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

async function ConnectedKafkaBreadcrumbItem({
  kafkaId,
  isActive,
}: KafkaParams & { isActive: boolean }) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getKafkaClusters();
  return (
    <KafkaBreadcrumbItem
      selected={cluster}
      clusters={clusters}
      isActive={isActive}
    />
  );
}
