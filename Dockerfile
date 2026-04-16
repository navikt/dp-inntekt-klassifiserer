FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:0381a298c3c6acd8916ec055f950365e19d640926a70176616b3d00f46e9d860

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
