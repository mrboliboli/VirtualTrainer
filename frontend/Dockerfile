FROM node:22-alpine AS construction
WORKDIR /source
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
USER root
RUN apk add --no-cache apache2-utils gettext \
    && rm /etc/nginx/conf.d/default.conf
COPY --from=construction /source/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/pace/pace.conf.template
COPY docker-entrypoint-pace.sh /docker-entrypoint.d/20-pace.sh
RUN chmod 755 /docker-entrypoint.d/20-pace.sh \
    && chown -R nginx:nginx /usr/share/nginx/html /etc/nginx/conf.d /etc/pace
USER nginx
EXPOSE 8080
