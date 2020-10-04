# build

FROM node:13.12.0-alpine as build
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json ./
COPY package-lock.json ./
RUN npm ci
RUN npm install react-scripts@3.4.1 -g
COPY . ./

ARG REACT_APP_HOST
ARG REACT_APP_PORT

ENV REACT_APP_HOST=$REACT_APP_HOST
ENV REACT_APP_PORT=$REACT_APP_PORT

RUN npm run build

# production

FROM nginx:stable-alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]