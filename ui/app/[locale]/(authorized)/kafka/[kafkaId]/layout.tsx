import { getKafkaCluster, getKafkaClusters } from "@/api/kafka/actions";
import { ClusterLinks } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/ClusterLinks";
import { getAuthOptions } from "@/app/api/auth/[...nextauth]/route";
import { AppLayout } from "@/components/AppLayout";
import { AppLayoutProvider } from "@/components/AppLayoutProvider";
import { getServerSession } from "next-auth";
import { KafkaParams } from "./kafka.params";
import { KafkaBreadcrumbItem } from "./KafkaBreadcrumbItem";
import {
  Breadcrumb,
  BreadcrumbItem,
  PageBreadcrumb,
  PageGroup,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { notFound } from "next/navigation";
import { PropsWithChildren, ReactNode, Suspense } from "react";

export default async function AsyncLayout({
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
  const authOptions = await getAuthOptions();
  const session = await getServerSession(authOptions);
  return (
    <Layout
      username={(session?.user?.name || session?.user?.email) ?? "User"}
      kafkaId={kafkaId}
      activeBreadcrumb={activeBreadcrumb}
      header={header}
      modal={modal}
    >
      {children}
    </Layout>
  );
}

function Layout({
  children,
  activeBreadcrumb,
  header,
  modal,
  kafkaId,
  username,
}: PropsWithChildren<{
  kafkaId: string;
  username: string;
  header: ReactNode;
  activeBreadcrumb: ReactNode;
  modal: ReactNode;
}>) {
  const t = useTranslations();
  return (
    <AppLayoutProvider>
      <AppLayout
        username={username}
        sidebar={
          <Suspense>
            <ClusterLinks kafkaId={kafkaId} />
          </Suspense>
        }
      >
        <PageGroup stickyOnBreakpoint={{ default: "top" }}>
          <PageBreadcrumb>
            <Breadcrumb>{activeBreadcrumb}</Breadcrumb>
          </PageBreadcrumb>
          {header}
        </PageGroup>
        <Suspense>{children}</Suspense>
        {modal}
      </AppLayout>
    </AppLayoutProvider>
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
