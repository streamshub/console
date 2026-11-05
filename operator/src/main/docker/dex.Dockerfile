FROM ghcr.io/dexidp/dex:v2.46.1@sha256:65b72ca0f4b0bf6d4ada84ec21da8851782563404ac7b8f926923d27ceced9ab

# Remove support for templated configurations. The Console operator will always
# provide its own configuration based on the Console custom resource.
USER 0
RUN rm -vf /usr/local/bin/docker-entrypoint /usr/local/bin/gomplate

# Switch to the numeric equivalent of dex:dex from the parent image
USER 1001:1001
ENTRYPOINT ["/usr/local/bin/dex"]
CMD ["serve"]
