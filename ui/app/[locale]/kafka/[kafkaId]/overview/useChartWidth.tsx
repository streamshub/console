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

  const requestRef = useRef<number>();

  const checkSize = () => {
    // The 'state' will always be the initial value here
    requestRef.current = requestAnimationFrame(checkSize);
    handleResize();
  };

  useEffect(() => {
    requestRef.current = requestAnimationFrame(checkSize);
    return () => {
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
    };
  }, []); // Make sure the effect runs only once

  return [containerRef, width];
}
