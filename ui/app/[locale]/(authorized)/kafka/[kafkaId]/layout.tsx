import { ClusterLinks } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/ClusterLinks";
import { getAuthOptions } from "@/app/api/auth/[...nextauth]/auth-options";
import { AppLayout } from "@/components/AppLayout";
import { AppLayoutProvider } from "@/components/AppLayoutProvider";
import { PageBreadcrumb, PageGroup } from "@/libs/patternfly/react-core";
import { getServerSession } from "next-auth";
import { useTranslations } from "next-intl";
import { PropsWithChildren, ReactNode, Suspense } from "react";
import { KafkaParams } from "./kafka.params";
import { getKafkaCluster, getKafkaClusters } from "@/api/kafka/actions";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ClusterInfo } from "@/components/AppDropdown";
import { oidcEnabled } from "@/utils/config";

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
  const loginRequired = !(await oidcEnabled());

  const clusters = (await getKafkaClusters(undefined, { pageSize: 1000 }))
    ?.payload;

  const clusterInfoList = clusters?.data.map((cluster: any) => {
    const id = cluster.id;
    const name = cluster.attributes?.name;
    const namespace = cluster.attributes?.namespace ?? "Not provided";
    const authMethod =
      cluster.meta?.authentication?.method ?? "no authentication";

    return {
      clusterName: name,
      projectName: namespace,
      authenticationMethod: authMethod,
      id: id,
      loginRequired: loginRequired,
    };
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  return (
    <Layout
      username={(session?.user?.name || session?.user?.email) ?? "User"}
      kafkaId={kafkaId}
      activeBreadcrumb={activeBreadcrumb}
      header={header}
      modal={modal}
      clusterInfoList={clusterInfoList || []}
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
  clusterInfoList,
}: PropsWithChildren<{
  kafkaId: string;
  username: string;
  header: ReactNode;
  activeBreadcrumb: ReactNode;
  modal: ReactNode;
  clusterInfoList: ClusterInfo[];
}>) {
  const t = useTranslations();
  return (
    <AppLayoutProvider>
      <AppLayout
        username={username}
        kafkaId={kafkaId}
        sidebar={<ClusterLinks kafkaId={kafkaId} />}
        clusterInfoList={clusterInfoList}
      >
        <PageGroup stickyOnBreakpoint={{ default: "top" }}>
          <PageBreadcrumb>{activeBreadcrumb}</PageBreadcrumb>
          {header}
        </PageGroup>
        <Suspense>{children}</Suspense>
        {modal}
      </AppLayout>
    </AppLayoutProvider>
  );
}
