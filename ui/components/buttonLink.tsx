"use client";
import { Button, ButtonProps } from "@/libs/patternfly/react-core";
import { Route } from "next";
import Link from "next/link";

export function ButtonLink<T extends string>({
  href,
  ...props
}: ButtonProps & { href: Route<T> | URL }) {
  return (
    <Button {...props} component={(props) => <Link {...props} href={href} />} />
  );
}
