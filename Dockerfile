FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:aee0a920d3f8e77c94e303e5958d4e639883fdb16c6098a53154088d72234f8b

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
