import { BreadcrumbLink } from "@/components/breadcrumbLink";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";

export default function DefaultBreadcrumb() {
  return (
    <Breadcrumb>
      <BreadcrumbLink href={"/"}>Bookmarks</BreadcrumbLink>
      <BreadcrumbItem isActive>Create</BreadcrumbItem>
    </Breadcrumb>
  );
}
