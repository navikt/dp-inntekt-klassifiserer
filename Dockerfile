FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:0826e325c1d97b4795414cd91bc71d025e445ab8d1e8791e6e24d680f18f17d3

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
