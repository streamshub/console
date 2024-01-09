"use client";
import { Loading } from "@/components/Loading";
import { useRouter } from "@/navigation";
import { useEffect } from "react";

export function RedirectOnLoad({ url }: { url: string }) {
  const router = useRouter();
  useEffect(() => {
    router.replace(url);
  }, [router, url]);
  return <Loading />;
}
