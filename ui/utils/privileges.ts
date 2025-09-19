type HasMeta = {
  meta?: {
    privileges?: string[],
  }
}

export function hasPrivilege(privilege: string, resource?: HasMeta | null): boolean {
    return resource?.meta?.privileges?.includes(privilege) ?? false;
}
