import { DEFAULT_PERPAGE } from "@/components/Table/TableView";
import { useCallback, useEffect, useState } from "react";

export function usePagination() {
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(DEFAULT_PERPAGE);

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);

    const pageFromURL = parseInt(searchParams.get("page") || "1", 10);
    const perPageFromURL = parseInt(
      searchParams.get("perPage") || String(DEFAULT_PERPAGE),
      10,
    );

    setPage(pageFromURL);
    setPerPage(perPageFromURL);
  }, []);

  const setPaginationQuery = useCallback(
    (newPage: number, newPerPage?: number) => {
      const searchParams = new URLSearchParams(window.location.search);
      searchParams.set("page", newPage.toString());
      if (newPerPage !== undefined) {
        searchParams.set("perPage", newPerPage.toString());
      }
      window.history.pushState(
        {},
        "",
        `${window.location.pathname}?${searchParams.toString()}`,
      );
    },
    [],
  );

  const setPagination = useCallback(
    (newPage: number, newPerPage?: number) => {
      setPaginationQuery(newPage, newPerPage);
      setPage(newPage);
      if (newPerPage !== undefined) {
        setPerPage(newPerPage);
      }
    },
    [setPaginationQuery],
  );

  return { page, perPage, setPagination };
}
