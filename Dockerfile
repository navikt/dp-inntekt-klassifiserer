FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:e1c77a25d37dfe969cad21a213cff8fab4ad76c42c73cefa0e700420b8656a00

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
