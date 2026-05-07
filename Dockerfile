FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:4b11bb71da47b75facc4c87ba5cbe6fa0c43dc7b43e9b3400ca3ca1290333622

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
