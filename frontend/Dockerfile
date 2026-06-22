# Estágio 1: Build do Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# Estágio 2: Servidor Web Nginx para servir o Front
FROM nginx:alpine
# Caminho ajustado para a nova estrutura do Angular application builder
COPY --from=build /app/dist/agora-front/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]