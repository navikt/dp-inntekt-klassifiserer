#!/usr/bin/env bash

if test -f /var/run/secrets/nais.io/service_user/username;
then
    export  SRVDP_INNTEKT_KLASSIFISERER_USERNAME=$(cat /var/run/secrets/nais.io/service_user/username)
fi
if test -f /var/run/secrets/nais.io/service_user/password;
then
    export  SRVDP_INNTEKT_KLASSIFISERER_PASSWORD=$(cat /var/run/secrets/nais.io/service_user/password)
fi


