FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:1c6327c1cc8f64374b495065d6863e925d963d4daee445172ad1fd108c0078d9

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
