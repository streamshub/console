"use client";
import {
  NavItem
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useParams, useSelectedLayoutSegment } from "next/navigation";

export function ToolNavItem({ url, title }: Tool) {
  const t = useTranslations();
  const { authProfile } = useParams();
  const segment = useSelectedLayoutSegment()
  const isActive = url === `/${segment}`;

  return (
    <NavItem isActive={isActive}>
      <Link href={`/${authProfile}${url}`}>{t(title)}</Link>
    </NavItem>
  );
}
