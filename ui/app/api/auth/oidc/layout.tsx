"use client";;
import { use } from "react";

import { SessionProvider } from "next-auth/react";

interface Props {
  children: React.ReactNode
}

export default function AuthLayout({ children }: Props) {
    return (
        <SessionProvider>
            { children }
        </SessionProvider>
    );
}
