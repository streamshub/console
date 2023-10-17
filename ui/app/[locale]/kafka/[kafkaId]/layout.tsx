import { getKafkaCluster, getKafkaClusters } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { KafkaBreadcrumbItem } from "@/app/[locale]/kafka/[kafkaId]/KafkaBreadcrumbItem";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import {
  Breadcrumb,
  BreadcrumbItem,
  Divider,
  PageBreadcrumb,
  PageGroup,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, ReactNode, Suspense } from "react";

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
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
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
      <Divider />
      <Suspense fallback={<Loading />}>{children}</Suspense>
      {modal}
    </>
  );
}
