"use client";
import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { Route } from "next";
import Link from "next/link";
import { PropsWithChildren } from "react";

export function BreadcrumbLink<T extends string>({
  href,
  isActive = false,
  children,
}: PropsWithChildren<{ href: Route<T> | URL; isActive?: boolean }>) {
  return (
    <BreadcrumbItem
      isActive={isActive}
      render={(props) => (
        <Link href={href} {...props}>
          {children}
        </Link>
      )}
    />
  );
}
