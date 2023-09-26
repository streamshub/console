import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";
import { ReactElement } from "react";
import { BreadcrumbLink } from "./BreadcrumbLink";

export function AppBreadcrumbs({
  bookmarkName,
  breadcrumbs = [],
}: {
  bookmarkName: ReactElement | string;
  breadcrumbs?: { href?: string; label: string }[];
}) {
  return (
    <Breadcrumb ouiaId="main-breadcrumb">
      <BreadcrumbLink href={"/"}>Bookmarks</BreadcrumbLink>
      <BreadcrumbItem isActive={!breadcrumbs}>{bookmarkName}</BreadcrumbItem>
      {breadcrumbs?.map(({ href, label }, idx, breadcrumbs) => {
        const isActive = idx === breadcrumbs.length - 1;
        return href ? (
          <BreadcrumbLink key={idx} isActive={isActive} href={href}>
            {label}
          </BreadcrumbLink>
        ) : (
          <BreadcrumbItem key={idx} isActive={isActive}>
            {label}
          </BreadcrumbItem>
        );
      })}
    </Breadcrumb>
  );
}
