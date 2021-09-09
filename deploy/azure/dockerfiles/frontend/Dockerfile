FROM node:14.17.4-alpine as build

# Set working directory
WORKDIR /app
COPY galleryapp/. ./

# Environment variables
ARG CORDA_ARTIFACTORY_USERNAME
ARG CORDA_ARTIFACTORY_PASSWORD
ENV CORDA_ARTIFACTORY_USERNAME $CORDA_ARTIFACTORY_USERNAME
ENV CORDA_ARTIFACTORY_PASSWORD $CORDA_ARTIFACTORY_PASSWORD

RUN npm install
RUN npm run-script build

# production environment
FROM nginx:stable-alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 6005
CMD ["nginx", "-g", "daemon off;"]