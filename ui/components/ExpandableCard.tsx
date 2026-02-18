'use client'
import {
  Card,
  CardExpandableContent,
  CardHeader,
  CardTitle,
} from '@/libs/patternfly/react-core'
import { PropsWithChildren, ReactNode, useState, useEffect } from 'react'

export function ExpandableCard({
  title,
  collapsedTitle,
  isCompact,
  children,
}: PropsWithChildren<{
  title: ReactNode
  collapsedTitle?: ReactNode
  isCompact?: boolean
}>) {
  const [expanded, setExpanded] = useState(true)

  const [mounted, setMounted] = useState(false)
  useEffect(() => {
    setMounted(true)
  }, [])

  const titleNode =
    typeof title === 'string' ? <CardTitle>{title}</CardTitle> : title

  if (!mounted) {
    return (
      <Card isCompact={isCompact} ouiaId="expandable-card-ssr">
        <CardHeader>{titleNode}</CardHeader>
      </Card>
    )
  }

  return (
    <Card isExpanded={expanded} isCompact={isCompact} ouiaId="expandable-card">
      <CardHeader onExpand={() => setExpanded((e) => !e)}>
        {expanded && titleNode}
        {!expanded && (collapsedTitle || titleNode)}
      </CardHeader>
      <CardExpandableContent>{children}</CardExpandableContent>
    </Card>
  )
}
