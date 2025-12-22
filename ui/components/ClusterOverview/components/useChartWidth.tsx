import {
  useCallback,
  useLayoutEffect,
  useRef,
  useState,
  RefObject,
} from "react";

export function useChartWidth(): [RefObject<HTMLDivElement | null>, number] {
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState<number>(0);

  const handleResize = useCallback((entries: ResizeObserverEntry[]) => {
    const entry = entries[0];
    if (entry) {
      setWidth(entry.contentRect.width);
    }
  }, []);

  useLayoutEffect(() => {
    const element = containerRef.current;
    if (!element) return;

    setWidth(element.clientWidth);

    const observer = new ResizeObserver(handleResize);
    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [handleResize]);

  return [containerRef, width];
}
