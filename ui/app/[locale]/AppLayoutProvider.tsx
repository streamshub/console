"use client";
import { createContext, PropsWithChildren, useContext, useState } from "react";

const AppLayoutContext = createContext({
  sidebarExpanded: true,
  toggleSidebar: () => {},
  closeSidebar: () => {},
  openSidebar: () => {},
});

export function AppLayoutProvider({ children }: PropsWithChildren) {
  const [sidebarExpanded, setSidebarExpanded] = useState(true);
  const closeSidebar = () => {
    setSidebarExpanded(false);
  };
  const openSidebar = () => {
    setSidebarExpanded(true);
  };
  const toggleSidebar = () => {
    setSidebarExpanded((o) => !o);
  };
  return (
    <AppLayoutContext.Provider
      value={{ sidebarExpanded, closeSidebar, openSidebar, toggleSidebar }}
    >
      {children}
    </AppLayoutContext.Provider>
  );
}

export function useAppLayout() {
  return useContext(AppLayoutContext);
}
