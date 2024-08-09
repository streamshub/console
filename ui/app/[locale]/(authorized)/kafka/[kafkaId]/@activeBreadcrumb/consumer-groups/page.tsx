import { BreadcrumbItem } from "@/libs/patternfly/react-core";

export default function ConsumerGroupsActiveBreadcrumb() {
  return (
    <BreadcrumbItem showDivider={true} isActive={true}>
      Consumer groups
    </BreadcrumbItem>
  );
}
