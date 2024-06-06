"use client";
import { usePathname } from "@/navigation";
import { useSearchParams } from "next/navigation";
import { ReactNode, useEffect } from "react";

export default function Template({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    if (window.analytics) {
      window.analytics.page();
    }
  }, [pathname, searchParams]);
  return <>{children}</>;
}
