FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:bf4e59c1797f1201a829f86c6c7667076395aade3a927308d6333bc7e4019f3f

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
