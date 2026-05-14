FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

RUN apk add --no-cache ffmpeg

COPY entrypoint-backend.sh .
RUN chmod +x entrypoint-backend.sh

EXPOSE 8080 6891-6991 6891-6991/udp

ENTRYPOINT ["./entrypoint-backend.sh"]