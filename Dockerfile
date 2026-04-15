FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:920d72a81533dfbc333273ee61963221e0fa19dbaae8d4a47760ce4e9b3b149f

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
