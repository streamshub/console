export const isReadonly = (() => {
  if (process.env.CONSOLE_MODE !== "read-write") {
    return true;
  }

  return true;
})();

export const isProductizedBuild = process.env.NEXT_PUBLIC_PRODUCTIZED_BUILD === "true";
