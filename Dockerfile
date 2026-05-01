FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:b2c8d728f68bf8e98ef446a19a73eeaa8587eac2ef214d74f8fde02a341271dc

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
