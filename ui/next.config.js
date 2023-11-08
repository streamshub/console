/** @type {import('next').NextConfig} */

const withNextIntl = require("next-intl/plugin")(
  // This is the default (also the `src` folder is supported out of the box)
  "./i18n.ts",
);

const nextConfig = {
  output: "standalone",
  experimental: {
    // typedRoutes: true, // disabled until next-intl is compatible with this
  },
  typescript: {
    //ignoreBuildErrors: true,
  },
};

module.exports = withNextIntl(nextConfig);
