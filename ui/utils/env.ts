export const isReadonly = (() => {
  return process.env.CONSOLE_MODE === "read-only";
})();

export const isProductizedBuild = process.env.NEXT_PUBLIC_PRODUCTIZED_BUILD === "true";
