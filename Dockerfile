FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:2f40393da39a95c2c7ac1212a68bda7cc0362d7bfc4f6cabb2fb6cec880f8f19

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
