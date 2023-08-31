"use client";
import { PropsWithChildren } from "react";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import Link from "next/link";

export function BreadcrumbLink({ href, isActive = false, children }: PropsWithChildren<{ href: string, isActive?: boolean }>) {
  return <BreadcrumbItem isActive={isActive} render={props => <Link href={href} {...props}>{children}</Link>} />

}
