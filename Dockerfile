FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:99d034005ebf7b415640e70e7677f9e63e67ec7164779a2c80e8ea4e9171f865

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
