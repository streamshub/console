import { getAuthProfile } from "@/api/auth";
import { getTopic } from "@/api/topics";
import { BreadcrumbLink } from "@/components/breadcrumbLink";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";

export default async function TopicBreadcrumb({
  params,
}: {
  params: { authProfile: string; topic: string };
}) {
  const authProfile = await getAuthProfile(params.authProfile);
  const topic = await getTopic(authProfile.attributes.cluster.id, params.topic);
  return (
    <Breadcrumb>
      <BreadcrumbLink href={`../`}>Topics</BreadcrumbLink>
      <BreadcrumbItem isActive>{topic.attributes.name}</BreadcrumbItem>
    </Breadcrumb>
  );
}
