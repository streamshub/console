'use client'
import { NavItem } from '@/libs/patternfly/react-core'
import { Link, usePathname } from '@/i18n/routing'
import { Route } from 'next'
import { PropsWithChildren } from 'react'

export function NavItemLink<T extends string>({
  children,
  url,
  exact = false,
}: PropsWithChildren<{
  url: Route<T> | URL
  exact?: boolean
}>) {
  const pathname = usePathname()
  const isActive = exact
    ? pathname === url.toString()
    : pathname.startsWith(url.toString()) && pathname === url.toString()

  return (
    <NavItem ouiaId={'something'} isActive={isActive}>
      <Link href={url}>{children}</Link>
    </NavItem>
  )
}
