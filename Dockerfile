FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:40c7f3dadeb9e0b0bdd0e0d802b34186b9f0b63d1c7442ecefef9900e1cb31d4

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
