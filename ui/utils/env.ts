export const isReadonly = process.env.NEXT_PUBLIC_CONSOLE_MODE === "read-only";

export const isProductizedBuild = process.env.NEXT_PUBLIC_PRODUCTIZED_BUILD === "true";

export const isTechPreview = (process.env.NEXT_PUBLIC_TECH_PREVIEW ?? "false") === "true";
