import { ClusterLinks } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/ClusterLinks";
import { getAuthOptions } from "@/app/api/auth/[...nextauth]/auth-options";
import { AppLayout } from "@/components/AppLayout";
import { AppLayoutProvider } from "@/components/AppLayoutProvider";
import { PageBreadcrumb, PageGroup } from "@/libs/patternfly/react-core";
import { getServerSession } from "next-auth";
import { getTranslations } from "next-intl/server"; // Use server-side version
import { ReactNode, Suspense } from "react";
import { KafkaParams } from "./kafka.params";
import { getKafkaCluster, getKafkaClusters } from "@/api/kafka/actions";
import { ClusterDetail } from "@/api/kafka/schema";
import { getMetadata } from "@/api/meta/actions";
import { MetadataResponse } from "@/api/meta/schema";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ClusterInfo } from "@/components/AppDropdown";
import { oidcEnabled } from "@/utils/config";
import { ThemeInitializer } from "@/components/ThemeInitializer";

type LayoutProps = {
  children: ReactNode;
  params: Promise<KafkaParams>;
  header: ReactNode;
  activeBreadcrumb: ReactNode;
  modal: ReactNode;
};

export default async function AsyncLayout(props: LayoutProps) {
  const params = await props.params;
  const { kafkaId } = params;

  const { children, activeBreadcrumb, header, modal } = props;

  const authOptions = await getAuthOptions();
  const session = await getServerSession(authOptions);
  const response = await getKafkaCluster(kafkaId);

  const isOidcEnabled = await oidcEnabled();
  const loginRequired = !isOidcEnabled;

  const clusters = (await getKafkaClusters(undefined, { pageSize: 1000 }))
    ?.payload;

  const clusterInfoList = clusters?.data.map((cluster: any) => {
    return {
      clusterName: cluster.attributes?.name,
      projectName: cluster.attributes?.namespace ?? "Not provided",
      authenticationMethod: cluster.meta?.authentication?.method ?? "no authentication",
      id: cluster.id,
      loginRequired: loginRequired,
    };
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const metadata = (await getMetadata()).payload ?? undefined;
  
  const t = await getTranslations();

  return (
    <Layout
      username={(session?.user?.name || session?.user?.email) ?? "User"}
      kafkaDetail={response.payload!}
      metadata={metadata}
      activeBreadcrumb={activeBreadcrumb}
      header={header}
      modal={modal}
      clusterInfoList={clusterInfoList || []}
      isOidcEnabled={isOidcEnabled}
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
  kafkaDetail,
  metadata,
  username,
  clusterInfoList,
  isOidcEnabled,
}: {
  children: ReactNode;
  kafkaDetail: ClusterDetail;
  metadata?: MetadataResponse;
  username: string;
  header: ReactNode;
  activeBreadcrumb: ReactNode;
  modal: ReactNode;
  clusterInfoList: ClusterInfo[];
  isOidcEnabled: boolean;
}) {
  return (
    <AppLayoutProvider>
      <ThemeInitializer />
      <AppLayout
        username={username}
        kafkaDetail={kafkaDetail}
        metadata={metadata}
        sidebar={<ClusterLinks kafkaDetail={kafkaDetail} />}
        clusterInfoList={clusterInfoList}
        isOidcEnabled={isOidcEnabled}
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
