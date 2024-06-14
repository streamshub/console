
export function readonly() {
  if (process.env.CONSOLE_MODE !== "read-write") {
    return true;
  }

  if (process.env.NEXT_PUBLIC_KEYCLOAK_URL && process.env.KEYCLOAK_CLIENTID && process.env.KEYCLOAK_CLIENTSECRET) {
    return false;
  }

  return true;
}
