# Serves the static Qanary project page (the GitHub Pages content in this folder)
# as a small nginx image, published to Docker Hub as qanary/qanary-webpage.
#
# It serves on nginx's default port 80; the deployment maps it as "40121:80"
# (see service_config/service_config.json).
#
# Build context is this docs/ folder, e.g.:
#   docker build -t qanary/qanary-webpage:latest ./docs
# (see service_config/build_webpage_image.sh and the
#  .github/workflows/docker-deployment-webpage.yml workflow).
FROM nginx:1.27-alpine

# the static site (index.html + assets/); see .dockerignore for what is excluded
COPY . /usr/share/nginx/html

EXPOSE 80
