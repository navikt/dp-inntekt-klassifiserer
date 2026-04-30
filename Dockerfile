FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:a3a77961037730f28fd5176743e973aa003d8e165f591d7463db3c720e200783

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
