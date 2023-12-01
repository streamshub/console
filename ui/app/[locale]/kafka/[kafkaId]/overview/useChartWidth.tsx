import type { RefObject } from "react";
import { useEffect, useLayoutEffect, useRef, useState } from "react";

export function useChartWidth(): [RefObject<HTMLDivElement>, number] {
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState<number>(0);

  const handleResize = () =>
    containerRef.current && setWidth(containerRef.current.clientWidth);

  useLayoutEffect(() => {
    handleResize();
  }, []);

  useEffect(() => {
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);
  return [containerRef, width];
}
