FROM clojure:lein-2.7.1-alpine

RUN apk add --update imagemagick
RUN mkdir -p /opt/fuggle
WORKDIR /opt/fuggle

ENV JVM_OPTS=-Xmx200m

EXPOSE 80

CMD echo fuggle_base
