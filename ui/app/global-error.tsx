"use client";
import { logger } from "@/utils/logger";
import { useEffect } from "react";
const log = logger.child({ module: "ui" });
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    log.error({error}, "unmanaged error");
  }, [error]);
  return (
    <html>
      <body>
        <h2>Something went wrong!</h2>
        <button onClick={() => reset()}>Try again</button>
      </body>
    </html>
  );
}
