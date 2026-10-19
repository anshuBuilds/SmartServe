#!/usr/bin/env bash
#
# Deploys a SmartServe frontend build to the directory host nginx serves.
#
# Installed on EC2 as /opt/smartserve/deploy-frontend.sh and invoked by
# GitHub Actions through SSM. Rollback = run it again with an older tag.
#
# Usage: deploy-frontend.sh <image-tag>
#
# Layout:
#   /var/www/smartserve-releases/<tag>/   one directory per deployed build
#   /var/www/smartserve -> <tag>          nginx `root`, swapped atomically
#
set -Eeuo pipefail

# Overrides (ECR_REGISTRY, LIVE_ROOT, ...) live in this file on the server.
CONFIG_FILE="/opt/smartserve/frontend.env"
[[ -f "$CONFIG_FILE" ]] && source "$CONFIG_FILE"

AWS_REGION="${AWS_REGION:-ap-south-1}"
ECR_REGISTRY="${ECR_REGISTRY:-157824577163.dkr.ecr.ap-south-1.amazonaws.com}"
ECR_REPOSITORY="${ECR_REPOSITORY:-smartserve-frontend}"
LIVE_ROOT="${LIVE_ROOT:-/var/www/smartserve}"
RELEASES_DIR="${RELEASES_DIR:-/var/www/smartserve-releases}"
KEEP_RELEASES="${KEEP_RELEASES:-5}"

IMAGE_TAG="${1:?usage: deploy-frontend.sh <image-tag>}"
IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
RELEASE_DIR="${RELEASES_DIR}/${IMAGE_TAG}"
STAGING_DIR="${RELEASES_DIR}/.staging-${IMAGE_TAG}"
CONTAINER=""

cleanup() {
    [[ -n "$CONTAINER" ]] && docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
    rm -rf "$STAGING_DIR"
}
trap cleanup EXIT

echo "Deploying frontend ${IMAGE}"

mkdir -p "$RELEASES_DIR"

# First run: LIVE_ROOT is still the hand-copied directory. Keep it as a
# release so it can be rolled back to, and free the path for the symlink.
if [[ -d "$LIVE_ROOT" && ! -L "$LIVE_ROOT" ]]; then
    LEGACY_DIR="${RELEASES_DIR}/legacy-$(date +%Y%m%d%H%M%S)"
    echo "Converting ${LIVE_ROOT} into release ${LEGACY_DIR}"
    cp -a "$LIVE_ROOT" "$LEGACY_DIR"
fi

if [[ ! -d "$RELEASE_DIR" ]]; then
    aws ecr get-login-password --region "$AWS_REGION" \
        | docker login --username AWS --password-stdin "$ECR_REGISTRY"

    docker pull "$IMAGE"

    # The image is FROM scratch and is never started; the command
    # given to `docker create` only has to exist on paper.
    CONTAINER="$(docker create "$IMAGE" /noop)"

    rm -rf "$STAGING_DIR"
    docker cp "${CONTAINER}:/dist" "$STAGING_DIR"

    if [[ ! -f "${STAGING_DIR}/index.html" ]]; then
        echo "Build ${IMAGE_TAG} has no index.html, refusing to deploy" >&2
        exit 1
    fi

    chmod -R a+rX "$STAGING_DIR"
    mv "$STAGING_DIR" "$RELEASE_DIR"

    docker rm -f "$CONTAINER" >/dev/null
    CONTAINER=""
    docker rmi "$IMAGE" >/dev/null 2>&1 || true
else
    echo "Release ${IMAGE_TAG} already on disk, switching to it"
fi

# docker cp keeps the image's build timestamps; mark activation time so
# pruning (newest first) treats this as the most recent release.
touch "$RELEASE_DIR"

# Swap the symlink atomically so nginx never sees a half-written build.
# On the first run the old directory (already copied above) is replaced.
ln -sfn "$RELEASE_DIR" "${LIVE_ROOT}.tmp"
if [[ -d "$LIVE_ROOT" && ! -L "$LIVE_ROOT" ]]; then
    rm -rf "$LIVE_ROOT"
fi
mv -Tf "${LIVE_ROOT}.tmp" "$LIVE_ROOT"

echo "${LIVE_ROOT} -> $(readlink "$LIVE_ROOT")"

# Prune old releases, never the one just activated.
ls -1dt "${RELEASES_DIR}"/*/ \
    | tail -n +"$((KEEP_RELEASES + 1))" \
    | { grep -v "/${IMAGE_TAG}/$" || true; } \
    | xargs -r rm -rf

echo "Frontend ${IMAGE_TAG} deployed"
