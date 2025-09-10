"use client";

import { useEffect } from "react";
import { signOut } from "next-auth/react";

export default function LogoutLocal() {
  useEffect(() => {
    signOut({ callbackUrl: "/" });
  }, []);

  return null;
}
