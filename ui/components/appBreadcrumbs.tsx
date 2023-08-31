import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";
import { BreadcrumbLink } from "./breadcrumbLink";

export function AppBreadcrumbs({ authProfileName, breadcrumbs = [] }: { authProfileName: string; breadcrumbs?: { href?: string, label: string }[] }) {
  return (
    <Breadcrumb ouiaId="main-breadcrumb">
      <BreadcrumbLink href={'/'}>Authorization Profiles</BreadcrumbLink>
      <BreadcrumbItem isActive={!breadcrumbs}>{authProfileName}</BreadcrumbItem>
      {breadcrumbs?.map(
        ({ href, label }, idx, breadcrumbs) => {
          const isActive = idx === (breadcrumbs.length - 1)
          return href ?
            <BreadcrumbLink key={idx} isActive={isActive} href={href}>{label}</BreadcrumbLink>
            : <BreadcrumbItem key={idx} isActive={isActive}>{label}</BreadcrumbItem>
        })}
    </Breadcrumb>

  )
}
