#!/bin/sh

# htpasswd for basic authentication
htpasswd -b /etc/nginx/.htpasswd $BASIC_USERNAME $BASIC_PASSWORD

exec nginx -g "daemon off;"
