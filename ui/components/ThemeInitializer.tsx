"use client";

import { useColorTheme } from "@/app/[locale]/useColorTheme";

export function ThemeInitializer() {
  // This hook applies the correct theme on page load
  useColorTheme();
  return null; // It doesn’t render anything visible
}
