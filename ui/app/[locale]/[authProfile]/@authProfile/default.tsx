import { getAuthProfile } from "@/api/auth";
import { AppBreadcrumbs } from "@/components/appBreadcrumbs";

export default async function ToolsBreadcrumb({
  params,
}: {
  params: {
    authProfile: string;
  };
}) {
  const authProfile = await getAuthProfile(params.authProfile);
  return (
    <AppBreadcrumbs
      authProfileName={`${authProfile.attributes.name}@${authProfile.attributes.cluster.attributes.name}`}
    />
  );
}
