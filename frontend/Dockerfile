# Stage 1: Build the React application
FROM node:22-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json ./

RUN npm ci --no-audit --no-fund

COPY . .

# Vite inlines VITE_* variables at build time, and environment
# variables take precedence over any .env file.
ARG VITE_API_BASE_URL
ARG VITE_ENABLE_NOTIFICATIONS=false
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_ENABLE_NOTIFICATIONS=${VITE_ENABLE_NOTIFICATIONS}

RUN test -n "$VITE_API_BASE_URL" \
    || (echo "VITE_API_BASE_URL build argument is required" >&2 && exit 1)

RUN npm test && npm run build


# Stage 2: Ship only the static files.
# This image is never run; the EC2 deploy script copies /dist
# out of it into the directory host nginx serves.
FROM scratch

COPY --from=build /workspace/dist /dist
