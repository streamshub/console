/**
 * Hook to track chart container width for responsive charts
 * 
 * Uses ResizeObserver and requestAnimationFrame to efficiently
 * track container width changes for responsive chart rendering.
 */

import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  RefObject,
} from 'react';

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
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [handleResize]);

  const requestRef = useRef<number | undefined>(undefined);
  const checkSizeRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    checkSizeRef.current = () => {
      requestRef.current = requestAnimationFrame(checkSizeRef.current!);
      handleResize();
    };
  }, [handleResize]);

  useEffect(() => {
    if (checkSizeRef.current) {
      requestRef.current = requestAnimationFrame(checkSizeRef.current);
    }
    return () => {
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
    };
  }, [handleResize]);

  return [containerRef, width];
}