FROM navikt/java:12

COPY build/libs/*-all.jar app.jar
COPY init-scripts /init-scripts
