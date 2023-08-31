import { getTopic } from "@/api/topics";
import { BreadcrumbLink } from "@/components/breadcrumbLink";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";

export default async function TopicBreadcrumb({
  params,
}: {
  params: { authProfile: string; topic: string };
}) {
  const topic = await getTopic(params.authProfile, params.topic);
  return (
    <Breadcrumb>
      <BreadcrumbLink href={"."}>Topics</BreadcrumbLink>
      <BreadcrumbItem isActive>{topic.attributes.name}</BreadcrumbItem>
    </Breadcrumb>
  );
}
