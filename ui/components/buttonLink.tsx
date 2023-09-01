"use client";
import { Button, ButtonProps } from "@/libs/patternfly/react-core";
import Link from "next/link";

export function ButtonLink({ href, ...props }: ButtonProps & { href: string }) {
  return (
    <Button {...props} component={(props) => <Link {...props} href={href} />} />
  );
}
