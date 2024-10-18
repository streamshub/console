import { notFound } from "next/navigation";
import { getKafkaCluster } from "@/api/kafka/actions";
import { ClusterLinks } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/ClusterLinks";
import { getAuthOptions } from "@/app/api/auth/[...nextauth]/route";
import { AppLayout } from "@/components/AppLayout";
import { AppLayoutProvider } from "@/components/AppLayoutProvider";
import {
  Breadcrumb,
  PageBreadcrumb,
  PageGroup,
} from "@/libs/patternfly/react-core";
import { getServerSession } from "next-auth";
import { PropsWithChildren, ReactNode, Suspense } from "react";
import { KafkaParams } from "./kafka.params";
import { ClusterDetail } from "@/api/kafka/schema";

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
  const cluster = await getKafkaCluster(kafkaId);

  if (!cluster) {
    notFound();
  }

  return (
    <Layout
      username={(session?.user?.name ?? session?.user?.email) ?? "User"}
      kafkaCluster={cluster}
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
  kafkaCluster,
  username,
}: PropsWithChildren<{
  kafkaCluster: ClusterDetail;
  username: string;
  header: ReactNode;
  activeBreadcrumb: ReactNode;
  modal: ReactNode;
}>) {
  return (
    <AppLayoutProvider>
      <AppLayout
        username={username}
        sidebar={<ClusterLinks kafkaCluster={kafkaCluster} />}
        kafkaCluster={kafkaCluster}
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
