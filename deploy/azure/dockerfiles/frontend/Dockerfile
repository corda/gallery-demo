FROM node:16-alpine as build
WORKDIR /app
COPY galleryapp/public /app/public
COPY galleryapp/src /app/src
COPY galleryapp/package.json /app/package.json
#COPY galleryapp/package-lock.json /app/package-lock.json
COPY galleryapp/tsconfig.json /app/tsconfig.json
COPY galleryapp/tsconfig.paths.json /app/tsconfig.paths.json
COPY galleryapp/.nvmrc /app/.nvmrc
COPY galleryapp/craco.config.js /app/craco.config.js
RUN npm install
RUN npm run build

# production environment
FROM nginx:1.16.0-alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 6005
CMD ["nginx", "-g", "daemon off;"]