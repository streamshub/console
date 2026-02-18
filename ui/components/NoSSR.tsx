'use client'
import { useEffect, useState, ReactNode } from 'react'

export const NoSSR = ({ children }: { children: ReactNode }) => {
  const [mounted, setMounted] = useState(false)
  useEffect(() => setMounted(true), [])
  return mounted ? <>{children}</> : null
}
