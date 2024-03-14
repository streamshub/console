"use client"; // Error components must be Client Components

import { ApplicationError } from "@/components/ApplicationError";
import { useEffect } from "react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log the error to an error reporting service
    console.error(error);
  }, [error]);

  return <ApplicationError error={error} onReset={reset} />;
}
