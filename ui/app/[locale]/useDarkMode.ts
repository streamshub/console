import { useEffect, useState } from "react";

export const useDarkMode = () => {
  const [isDarkMode, setIsDarkMode] = useState(false);

  useEffect(() => {
    const storedPref = localStorage.getItem("isDarkMode") === "true";
    setIsDarkMode(storedPref);
    document.documentElement.classList.toggle("pf-v6-theme-dark", storedPref);
  }, []);

  const toggleDarkMode = (value: boolean) => {
    setIsDarkMode(value);
    localStorage.setItem("isDarkMode", value.toString());
    document.documentElement.classList.toggle("pf-v6-theme-dark", value);
  };

  return { isDarkMode, toggleDarkMode };
};
