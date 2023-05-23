import { usePathname, useSearchParams } from "next/navigation";
import { useRouter } from "next/router";
import { useCallback } from "react";
import { DEFAULT_PERPAGE } from "./TableView";

export function usePaginationSearchParams() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage =
    parseInt(searchParams.get("perPage") || "", 10) || DEFAULT_PERPAGE;

  const setPagination = useCallback(
    (page: number, perPage: number) => {
      const params = new URLSearchParams(searchParams);
      params.set("page", page.toString());
      params.set("perPage", perPage.toString());
      void router.push(pathname + "?" + params.toString());
    },
    [pathname, router, searchParams]
  );

  return {
    page,
    perPage,
    setPagination,
  };
}
