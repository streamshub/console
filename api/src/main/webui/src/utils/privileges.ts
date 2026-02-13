import { MetaWithPrivileges } from '../api/types';

type HasMeta = {
  meta?: MetaWithPrivileges | null;
};

export function hasPrivilege(privilege: string, resource?: HasMeta | null): boolean {
  return resource?.meta?.privileges?.includes(privilege) ?? false;
}
