import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  RefObject,
} from "react";

export function useChartWidth(): [RefObject<HTMLDivElement | null>, number] {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [width, setWidth] = useState<number>(0);

  const handleResize = useCallback(() => {
    if (containerRef.current) {
      setWidth(containerRef.current.clientWidth);
    }
  }, []);

  useLayoutEffect(() => {
    handleResize();
  }, [handleResize]);

  useEffect(() => {
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [handleResize]);

  const requestRef = useRef<number | undefined>(undefined);

  const checkSize = useCallback(() => {
    requestRef.current = requestAnimationFrame(checkSize);
    handleResize();
  }, [handleResize]);

  useEffect(() => {
    requestRef.current = requestAnimationFrame(checkSize);
    return () => {
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
    };
  }, [checkSize]);

  return [containerRef, width];
}
