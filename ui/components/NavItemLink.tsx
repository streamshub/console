"use client";
import { NavItem } from "@/libs/patternfly/react-core";
import { Link, usePathname } from "@/navigation";
import { Route } from "next";
import { PropsWithChildren } from "react";

export function NavItemLink<T extends string>({
  children,
  url,
  exact = false,
}: PropsWithChildren<{
  url: Route<T> | URL;
  exact?: boolean;
}>) {
  const pathname = usePathname();
  const isActive = exact
    ? url === pathname
    : pathname.startsWith(url.toString());
  return (
    <NavItem isActive={isActive}>
      <Link href={url}>{children}</Link>
    </NavItem>
  );
}
