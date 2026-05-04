FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:c131bc857972e10af8fd4ddcd1cf8bc502a55f9b48d2214b5fe5495ecb2c2f22

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
