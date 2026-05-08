FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:a7a5d0c4f0ae0ad920ccc9b984877bb9a0a806d50fc666ae9f21bab4ef0a14b1

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
