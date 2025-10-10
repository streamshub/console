"use client";

import { useEffect, useState } from "react";

export type ThemeMode = "light" | "dark" | "system";

const COLOR_MODES = {
  LIGHT: "light",
  DARK: "dark",
  SYSTEM: "system",
} as const;

export const useColorTheme = () => {
  const [mode, setMode] = useState<ThemeMode>(() => {
    if (typeof window === "undefined") return COLOR_MODES.SYSTEM;
    return (
      (localStorage.getItem("theme-mode") as ThemeMode) || COLOR_MODES.SYSTEM
    );
  });

  const [isSystemDark, setIsSystemDark] = useState(() =>
    typeof window !== "undefined"
      ? window.matchMedia("(prefers-color-scheme: dark)").matches
      : false,
  );

  const isDarkMode =
    mode === COLOR_MODES.DARK || (mode === COLOR_MODES.SYSTEM && isSystemDark);

  useEffect(() => {
    if (typeof document === "undefined") return;

    document.documentElement.classList.toggle("pf-v6-theme-dark", isDarkMode);
    localStorage.setItem("theme-mode", mode);
  }, [mode, isDarkMode]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const handleChange = (e: MediaQueryListEvent) => setIsSystemDark(e.matches);
    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, []);

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
