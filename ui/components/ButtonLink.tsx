"use client";
import { ButtonProps } from "@/libs/patternfly/react-core";
import { Link } from "@/navigation";
import { Route } from "next";

export function ButtonLink<T extends string>({
  href,
  variant,
  children,
}: ButtonProps & { href: Route<T> | URL }) {
  return (
    <Link className={`pf-v5-c-button pf-m-${variant}`} href={href}>
      {children}
    </Link>
  );
}
