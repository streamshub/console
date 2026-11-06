# LDAP Authentication Example

This directory demonstrates how to configure StreamsHub Console for LDAP authentication using the operator's built-in Dex identity broker integration (`spec.security.ldap`). It includes a sample OpenLDAP deployment for local development and testing.

## Files

- [`openldap.yaml`](./openldap.yaml): Deploys an OpenLDAP instance pre-populated with sample organizational units, users, groups, and a read-only service account for binding.
- [`console.yaml`](./console.yaml): A sample `Console` Custom Resource configured with `spec.security.ldap` and role mappings for the LDAP `developers` group.

## Pre-populated Sample LDAP Data

The demo OpenLDAP instance is bootstrapped with the base DN `dc=streamshub,dc=dev` and contains:

### Users (`ou=people,dc=streamshub,dc=dev`)

| Username (`uid`) | Password | Full Name (`cn`) | Email (`mail`) | Group Memberships |
|---|---|---|---|---|
| `alice` | `alice` | Alice User | `alice@streamshub.io` | `developers` |
| `bob` | `bob` | Bob User | `bob@streamshub.io` | `developers` |

### Groups (`ou=groups,dc=streamshub,dc=dev`)

- **`developers`** (`cn=developers,ou=groups,dc=streamshub,dc=dev`): Contains members `alice` and `bob`. In `console.yaml`, members of this group are mapped to the `console-role` role.

### Bind Account

- **Bind DN**: `cn=readonly,dc=streamshub,dc=dev`
- **Bind Password**: `password123`

## Prerequisites

This example assumes that the StreamsHub Console Operator is already deployed on your cluster and watching for `Console` custom resources in the namespace where the example CR is created.

## Usage

### 1. Deploy OpenLDAP

Deploy the sample OpenLDAP server into your target namespace (for example, `default`):

```shell
kubectl apply -f openldap.yaml -n default
```

Verify that the `openldap` pod and service are running:

```shell
kubectl get pods,svc -l app=openldap -n default
```

### 2. Deploy Console with LDAP Configuration

Review and update [`console.yaml`](./console.yaml) to match your environment:
- Set `spec.hostname` to the desired ingress hostname (e.g. `console.example.com`).
- Adjust `spec.security.ldap.host` if OpenLDAP was deployed to a different namespace or service name.

Apply the Console resource:

```shell
kubectl apply -f console.yaml -n default
```

When `spec.security.ldap` is specified, the Console operator automatically:
1. Deploys and manages a local Dex identity broker instance.
2. Configures Dex with the LDAP connector settings, user search, and group search queries.
3. Wires the Console to use Dex as its OIDC provider with PKCE and group claim propagation.

### 3. Log In

Navigate to your Console URL in a browser and log in using the sample credentials (e.g. username `alice`, password `alice`). The user will be authenticated via LDAP and assigned the `console-role` permissions based on their `developers` group membership.
