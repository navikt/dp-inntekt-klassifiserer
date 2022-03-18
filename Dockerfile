FROM navikt/java:17

COPY build/libs/*-all.jar app.jar
COPY init-scripts /init-scripts
