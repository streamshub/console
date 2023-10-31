"use client";
import { Button, ButtonProps } from "@/libs/patternfly/react-core";
import { Link } from "@/navigation";
import { Route } from "next";

export function ButtonLink<T extends string>({
  href,
  ...props
}: ButtonProps & { href: Route<T> | URL }) {
  return (
    <Button {...props} component={(props) => <Link {...props} href={href} />} />
  );
}
