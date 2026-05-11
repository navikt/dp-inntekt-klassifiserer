FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:3028ce82a2ef99abc16611f879b8182e6dd73ece8118737810d7d6200ebecf08

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
