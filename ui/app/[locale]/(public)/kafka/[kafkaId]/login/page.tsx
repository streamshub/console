import { getKafkaClusters } from "@/api/kafka/actions";
import { redirect } from "@/navigation";
import { getCsrfToken, getProviders } from "next-auth/react";
import { SignInPage } from "./SignInPage";

export default async function SignIn({
  searchParams,
  params,
}: {
  searchParams?: { callbackUrl?: string };
  params: { kafkaId?: string };
}) {
  const providers = await getProviders();
  const clusters = await getKafkaClusters();
  const cluster = clusters.find((c) => c.id === params.kafkaId);
  if (cluster) {
    const authMethod = cluster.meta.authentication;
    const provider = {
      basic: "credentials" as const,
      oauth: "oauth-token" as const,
      anonymous: "anonymous" as const,
    }[authMethod?.method ?? "anonymous"];
    return (
      <SignInPage
        provider={provider}
        callbackUrl={
          searchParams?.callbackUrl ?? `/kafka/${params.kafkaId}/overview`
        }
        hasMultipleClusters={clusters.length > 1}
      />
    );
  }
  return redirect("/");
}
