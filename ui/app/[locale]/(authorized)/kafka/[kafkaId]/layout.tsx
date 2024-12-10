import { ClusterLinks } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/ClusterLinks";
import { getAuthOptions } from "@/app/api/auth/[...nextauth]/auth-options";
import { AppLayout } from "@/components/AppLayout";
import { AppLayoutProvider } from "@/components/AppLayoutProvider";
import {
  Breadcrumb,
  PageBreadcrumb,
  PageGroup,
} from "@/libs/patternfly/react-core";
import { getServerSession } from "next-auth";
import { useTranslations } from "next-intl";
import { PropsWithChildren, ReactNode, Suspense } from "react";
import { KafkaParams } from "./kafka.params";
import { getKafkaCluster } from "@/api/kafka/actions";
import { NoDataErrorState } from "@/components/NoDataErrorState";

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
  const response = await getKafkaCluster(kafkaId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const cluster = response.payload;

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
        kafkaId={kafkaId}
        sidebar={<ClusterLinks kafkaId={kafkaId} />}
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
