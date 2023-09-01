import { BreadcrumbLink } from "@/components/breadcrumbLink";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";

export default function DefaultBreadcrumb() {
  return (
    <Breadcrumb>
      <BreadcrumbLink href={"/"}>Authorization Profiles</BreadcrumbLink>
      <BreadcrumbItem isActive>New Authorization Profile</BreadcrumbItem>
    </Breadcrumb>
  );
}
