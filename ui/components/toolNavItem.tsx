"use client";
import { NavItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useParams, useSelectedLayoutSegment } from "next/navigation";

export function ToolNavItem({ url, title }: { url: string; title: string }) {
  const t = useTranslations();
  const { bookmark } = useParams();
  const segment = useSelectedLayoutSegment();
  const isActive = url === `/${segment}`;

  return (
    <NavItem isActive={isActive}>
      <Link href={`/${bookmark}${url}`}>{t(title)}</Link>
    </NavItem>
  );
}
