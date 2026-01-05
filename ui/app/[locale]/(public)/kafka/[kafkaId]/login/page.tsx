import { getKafkaClusters } from "@/api/kafka/actions";
import { redirect } from "@/i18n/routing";
import { SignInPage } from "./SignInPage";
import { getTranslations } from "next-intl/server";

type Props = {
  params: { kafkaId: string };
  searchParams?: { callbackUrl?: string };
};

type Provider = "anonymous" | "credentials" | "oauth-token";

export async function generateMetadata() {
  const t = await getTranslations();
  return {
    title: `${t("login-in-page.title")} | ${t("common.title")}`,
  };
}

export default async function SignIn({ params, searchParams }: Props) {
  const kafkaId = params.kafkaId;

  const clusters = (await getKafkaClusters(undefined, { pageSize: 1000 }))
    ?.payload;

  const cluster = clusters?.data.find((c) => c.id === kafkaId);

  if (!cluster) {
    redirect("/");
  }

  const safeCluster = cluster!;

  const providerMap: Record<string, Provider> = {
    basic: "credentials",
    oauth: "oauth-token",
    anonymous: "anonymous",
  };

  const provider: Provider =
    providerMap[safeCluster.meta.authentication?.method ?? "anonymous"];

  return (
    <SignInPage
      kafkaId={kafkaId}
      provider={provider}
      callbackUrl={
        searchParams?.callbackUrl ?? `/kafka/${kafkaId}/overview`
      }
      hasMultipleClusters={(clusters?.data?.length ?? 0) > 1}
      clusterName={safeCluster.attributes.name}
    />
  );
}
