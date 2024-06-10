import config from '@/utils/config';

export const dynamic = "force-dynamic";

/*
 * This route serves as an endpoint for middleware.js to fetch whether
 * OIDC security is enabled or not.
 */
export async function GET() {
    const oidcEnabled = await config().then(cfg => !!cfg.security?.oidc);

    return Response.json({
        "oidc": oidcEnabled,
    });
}
