"use client";
import { EmptyStateLoading } from "@/components/EmptyStateLoading";
import { useRouter } from "@/i18n/routing";
import { useEffect } from "react";

export function RedirectOnLoad({ url }: { url: string }) {
  const router = useRouter();
  useEffect(() => {
    router.replace(url);
  }, [router, url]);
  return <EmptyStateLoading />;
}
