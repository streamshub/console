"use client";

import { signIn, useSession } from "next-auth/react";
import { useRouter } from 'next/navigation'
import { useEffect } from "react";

export default function SignIn() {
    const router = useRouter()
    const { status } = useSession()

    useEffect(() => {
        if (status === 'unauthenticated') {
            signIn('oidc')
        }
        else if (status === 'authenticated') {
            router.push('/')
        }
    }, [ router, status ])

    return (
        <div/>
    )
}
