"use client";;
import { use } from "react";

import { SessionProvider } from "next-auth/react";

interface Props {
  children: React.ReactNode
}

export default function AuthLayout(props: Props) {
    const children = use(props.children);
    return (
        <SessionProvider>
            { children }
        </SessionProvider>
    );
}
