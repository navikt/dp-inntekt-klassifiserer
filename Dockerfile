FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:69327839824e4e144a8e1c3070cc0d7498cc20ea3f3261bc80ce5283565943bc

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt"]
