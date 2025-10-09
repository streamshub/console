import { useEffect, useState } from "react";

export type ThemeMode = "light" | "dark" | "system";

const COLOR_MODES = {
  LIGHT: "light",
  DARK: "dark",
  SYSTEM: "system",
} as const;

export const useTheme = () => {
  const [mode, setMode] = useState<ThemeMode>(() => {
    const stored = localStorage.getItem("theme-mode") as ThemeMode | null;
    return stored || COLOR_MODES.SYSTEM;
  });

  const isDarkMode =
    mode === COLOR_MODES.DARK ||
    (mode === COLOR_MODES.SYSTEM &&
      window.matchMedia("(prefers-color-scheme: dark)").matches);

  useEffect(() => {
    const root = document.documentElement;

    if (isDarkMode) {
      root.classList.add("pf-v6-theme-dark");
    } else {
      root.classList.remove("pf-v6-theme-dark");
    }

    localStorage.setItem("theme-mode", mode);
  }, [mode, isDarkMode]);

  useEffect(() => {
    if (mode !== COLOR_MODES.SYSTEM) return;

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = () => setMode(COLOR_MODES.SYSTEM);
    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, [mode]);

  return {
    mode,
    setMode,
    isDarkMode,
    modes: COLOR_MODES,
  };
};
