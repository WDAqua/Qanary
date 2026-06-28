# Serves the static Qanary project page (the GitHub Pages content in this folder)
# as a small nginx image, published to Docker Hub as qanary/qanary-webpage.
#
# It listens on container port 40121 to match the port mapping in
# service_config/service_config.json ("40121:40121").
#
# Build context is this docs/ folder, e.g.:
#   docker build -t qanary/qanary-webpage:latest ./docs
# (see service_config/build_webpage_image.sh and the
#  .github/workflows/docker-deployment-webpage.yml workflow).
FROM nginx:1.27-alpine

# serve on 40121 instead of nginx's default port 80
RUN sed -i 's/listen *80;/listen 40121;/; s/listen *\[::\]:80;/listen [::]:40121;/' \
      /etc/nginx/conf.d/default.conf

# the static site (index.html + assets/); see .dockerignore for what is excluded
COPY . /usr/share/nginx/html

EXPOSE 40121
