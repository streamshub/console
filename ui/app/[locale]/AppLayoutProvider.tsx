"use client";
import { createContext, PropsWithChildren, useContext, useState } from "react";

const AppLayoutContext = createContext({
  sidebarExpanded: true,
  toggleSidebar: () => {},
});

export function AppLayoutProvider({ children }: PropsWithChildren) {
  const [sidebarExpanded, setSidebarExpanded] = useState(true);
  const toggleSidebar = () => {
    setSidebarExpanded((o) => !o);
  };
  return (
    <AppLayoutContext.Provider value={{ sidebarExpanded, toggleSidebar }}>
      {children}
    </AppLayoutContext.Provider>
  );
}

export function useAppLayout() {
  return useContext(AppLayoutContext);
}
