import { usePathname, useRouter } from "next/navigation";
import { useCallback } from "react";

export function useFilterParams(
  params: Record<string, string | number | undefined>,
) {
  const router = useRouter();
  const pathname = usePathname();

  // Get a new searchParams string by merging the current
  // searchParams with a provided key/value pair
  const createQueryString = useCallback(
    (params: { name: string; value: string | undefined }[]) => {
      const sp = new URLSearchParams();
      params.forEach(({ name, value }) => sp.set(name, value || ""));

      return sp.toString();
    },
    [],
  );

  const updateUrl = useCallback(
    (name: string, value: string | undefined) => {
      const newParams = {
        ...params,
        [name]: value,
      };
      const sp = Object.entries(newParams).map(([name, value]) => ({
        name,
        value: value?.toString(),
      }));
      router.push(pathname + "?" + createQueryString(sp));
    },
    [createQueryString, params, pathname, router],
  );

  return useCallback(
    function updateUrl(name: string, value: string | undefined) {
      const newParams = {
        ...params,
        [name]: value,
      };
      const sp = Object.entries(newParams).map(([name, value]) => ({
        name,
        value: value?.toString(),
      }));
      router.push(pathname + "?" + createQueryString(sp));
    },
    [createQueryString, params, pathname, router],
  );
}
