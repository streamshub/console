import { getKafkaClusters } from "@/api/kafka/actions";
import { redirect } from "@/i18n/routing";
import { SignInPage } from "./SignInPage";
import { getTranslations } from "next-intl/server";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("login-in-page.title")} | ${t("common.title")}`,
  };
}

export default async function SignIn(
  props: {
    searchParams?: Promise<{ callbackUrl?: string }>;
    params: Promise<{ kafkaId?: string }>;
  }
) {
  const params = await props.params;
  const searchParams = await props.searchParams;
  const clusters = (await getKafkaClusters(undefined, { pageSize: 1000 }))
    ?.payload;

  const cluster = clusters?.data.find((c) => c.id === params.kafkaId);

  if (cluster) {
    const authMethod = cluster.meta.authentication;
    const provider = {
      basic: "credentials" as const,
      oauth: "oauth-token" as const,
      anonymous: "anonymous" as const,
    }[authMethod?.method ?? "anonymous"];
    const clusterName = cluster.attributes.name;
    return (
      <SignInPage
        kafkaId={params.kafkaId!}
        provider={provider}
        callbackUrl={
          searchParams?.callbackUrl ?? `/kafka/${params.kafkaId}/overview`
        }
        hasMultipleClusters={(clusters?.data?.length ?? 0) > 1}
        clusterName={clusterName}
      />
    );
  }
  return redirect("/");
}
