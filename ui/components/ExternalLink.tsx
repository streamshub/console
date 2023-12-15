import type { ButtonProps } from "@/libs/patternfly/react-core";
import { Button, ButtonVariant } from "@/libs/patternfly/react-core";
import { ExternalLinkAltIcon } from "@/libs/patternfly/react-icons";
import { ReactNode } from "react";

export type ExternaLinkProps = {
  testId: string;
  target?: ButtonProps["target"];
  href: NonNullable<ButtonProps["href"]>;
  className?: string;
  ouiaId?: ButtonProps["ouiaId"];
  children: ReactNode;
};

export function ExternalLink({
  testId,
  target = "_blank",
  href,
  className,
  ouiaId,
  children,
}: ExternaLinkProps) {
  return (
    <Button
      data-testid={testId}
      isInline
      ouiaId={ouiaId}
      variant={ButtonVariant.link}
      component="a"
      target={target}
      href={href}
    >
      {children}
      <span style={{ whiteSpace: "nowrap" }}>
        &nbsp;
        <ExternalLinkAltIcon className={className} />
      </span>
    </Button>
  );
}
