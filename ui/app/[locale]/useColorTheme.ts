"use client";

import { useEffect, useState } from "react";

export type ThemeMode = "light" | "dark" | "system";

const COLOR_MODES = {
  LIGHT: "light",
  DARK: "dark",
  SYSTEM: "system",
} as const;

export const useColorTheme = () => {
  const [mode, setMode] = useState<ThemeMode>(COLOR_MODES.SYSTEM);

  // Load saved mode
  useEffect(() => {
    if (typeof window === "undefined") return;
    const stored = localStorage.getItem("theme-mode") as ThemeMode | null;
    if (stored) setMode(stored);
  }, []);

  const isDarkMode =
    mode === COLOR_MODES.DARK ||
    (mode === COLOR_MODES.SYSTEM &&
      typeof window !== "undefined" &&
      window.matchMedia("(prefers-color-scheme: dark)").matches);

  // Apply mode + persist to localStorage
  useEffect(() => {
    if (typeof document === "undefined") return;
    const root = document.documentElement;

    root.classList.toggle("pf-v6-theme-dark", isDarkMode);
    if (typeof window !== "undefined") {
      localStorage.setItem("theme-mode", mode);
    }
  }, [mode, isDarkMode]);

  // Watch system preference changes
  useEffect(() => {
    if (typeof window === "undefined" || mode !== COLOR_MODES.SYSTEM) return;

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = () => setMode(COLOR_MODES.SYSTEM);
    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, [mode]);

  // Derived helpers
  const isLightMode = mode === COLOR_MODES.LIGHT;
  const isSystemMode = mode === COLOR_MODES.SYSTEM;

  const toggleDarkMode = (enableDark: boolean) =>
    setMode(enableDark ? COLOR_MODES.DARK : COLOR_MODES.LIGHT);

  const setSystemMode = () => setMode(COLOR_MODES.SYSTEM);

  const cycleTheme = () => {
    if (isSystemMode) setMode(COLOR_MODES.LIGHT);
    else if (isLightMode) setMode(COLOR_MODES.DARK);
    else setMode(COLOR_MODES.SYSTEM);
  };

  return {
    mode,
    setMode,
    modes: COLOR_MODES,
    isDarkMode,
    isLightMode,
    isSystemMode,
    toggleDarkMode,
    setSystemMode,
    cycleTheme,
  };
};
