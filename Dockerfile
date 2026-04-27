FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:016ff9aa423e34d546187627c9edf0fc569c39f258eed9834ee99383d649a9d1

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
