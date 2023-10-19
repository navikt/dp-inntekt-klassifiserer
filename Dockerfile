FROM ghcr.io/navikt/baseimages/temurin:21

COPY build/libs/*-all.jar app.jar
COPY init-scripts /init-scripts
