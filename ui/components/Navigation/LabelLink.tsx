"use client";
import { Label, LabelProps } from "@/libs/patternfly/react-core";
import { Link } from "@/navigation";
import { Route } from "next";

export function LabelLink<T extends string>({
  href,
  ...props
}: LabelProps & { href: Route<T> | URL }) {
  return (
    <Label
      {...props}
      render={({ className, content }) => (
        <Link className={className} href={href}>
          {content}
        </Link>
      )}
    />
  );
}
