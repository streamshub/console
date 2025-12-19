/** @type {import('next').NextConfig} */

const withNextIntl = require("next-intl/plugin")(
  // This is the default (also the `src` folder is supported out of the box)
  "./i18n.tsx",
);

const nextConfig = {
  output: "standalone",
  experimental: {
    // typedRoutes: true, // disabled until next-intl is compatible with this
  },
  turbopack: {
    root: __dirname, // optional but recommended
  },
  typescript: {
    //ignoreBuildErrors: true,
  },
  transpilePackages: [
    "@patternfly/quickstarts",
    "@patternfly/react-core",
    "@patternfly/react-styles",
    "@patternfly/react-charts",
    "@patternfly/react-table",
    "@patternfly/react-tokens",
    "@patternfly/react-icons",
  ],
  logging:
    process.env.NODE_ENV === "development"
      ? {
          fetches: {
            fullUrl: true,
            hmrRefreshes: true,
          },
        }
      : undefined,
};

module.exports = withNextIntl(nextConfig);
