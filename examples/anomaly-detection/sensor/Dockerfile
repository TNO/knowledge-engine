FROM alpine:3.23.3

RUN apk add --no-cache bash curl

COPY test_device_publisher.sh /
RUN chmod +x /test_device_publisher.sh

ENTRYPOINT ["/bin/bash", "-C", "/test_device_publisher.sh"]