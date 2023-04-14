FROM nginx:1.23.4-alpine

# Install apache2-utils to get htpasswd command
RUN apk add apache2-utils

RUN touch /etc/nginx/.htpasswd

# # Basic auth credentials
# ENV BASIC_USERNAME=
# ENV BASIC_PASSWORD=

# # Forward host and foward port as env variables
# ENV FORWARD_HOST=
# ENV FORWARD_PORT=

# Nginx config file
WORKDIR /
COPY default.conf /etc/nginx/conf.d/default.conf

# Startup script
COPY run.sh ./
RUN chmod 0755 ./run.sh
CMD [ "./run.sh" ]
