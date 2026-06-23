# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# ── Stage 2: serve ────────────────────────────────────────────────────────────
FROM nginx:alpine
COPY --from=builder /app/dist/fleetmanager-web/browser /usr/share/nginx/html
# nginx:alpine procesa automáticamente /etc/nginx/templates/*.template con envsubst
# BFF_HOST se inyecta como variable de entorno en el Container App (ej: "bff-web")
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80
# Valor por defecto para desarrollo local con docker-compose
ENV BFF_HOST=bff-web:8080
CMD ["nginx", "-g", "daemon off;"]
