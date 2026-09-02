# Image de production : build Vite puis service statique par nginx.

# ── Étape 1 : construction ───────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /build

# Vite fige les variables VITE_* à la construction : ce sont des build args.
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_USE_MOCKS=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCKS=$VITE_USE_MOCKS

# Dépendances d'abord : le cache survit aux modifications de source.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Étape 2 : exécution ──────────────────────────────────────────────────────
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80
