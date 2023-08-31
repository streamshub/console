import { getAuthProfiles } from "@/api/auth";
import { AppBreadcrumbs } from "@/components/appBreadcrumbs";
import { useTranslations } from "next-intl";
import { redirect } from "next/navigation";

export default async function ToolsBreadcrumb({
  params,
}: {
  params: {
    authProfile: string;
  };
}) {
  const authProfiles = await getAuthProfiles();

  const authProfile = authProfiles.find((p) => p.id === params.authProfile);
  if (!authProfile) {
    redirect("/");
  }
  return (
    <ToolsBreadcrumbContent authProfileName={authProfile.attributes.name} />
  );
}

function ToolsBreadcrumbContent({
  authProfileName,
}: {
  authProfileName: string;
}) {
  const t = useTranslations("common");
  return <AppBreadcrumbs authProfileName={authProfileName} />;
}
