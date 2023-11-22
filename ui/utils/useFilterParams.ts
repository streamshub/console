import { usePathname, useRouter } from "next/navigation";
import { useCallback } from "react";

export function useFilterParams(
  params: Record<string, string | number | boolean | null | undefined>,
) {
  const router = useRouter();
  const pathname = usePathname();

  // Get a new searchParams string by merging the current
  // searchParams with a provided key/value pair
  const createQueryString = useCallback(
    (params: { name: string; value: string | null | undefined }[]) => {
      const sp = new URLSearchParams();
      params.forEach(({ name, value }) => sp.set(name, value || ""));

      return sp.toString();
    },
    [],
  );

  return useCallback(
    function updateUrl(
      newParams: Record<string, string | number | boolean | null | undefined>,
    ) {
      const updatedParams = {
        ...params,
        ...newParams,
      };
      const sp = Object.entries(updatedParams).map(([name, value]) => ({
        name,
        value: value?.toString(),
      }));
      router.push(pathname + "?" + createQueryString(sp));
    },
    [createQueryString, params, pathname, router],
  );
}
