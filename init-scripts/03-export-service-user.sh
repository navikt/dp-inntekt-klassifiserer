#!/usr/bin/env bash

if test -f /secrets/serviceuser/srvdp-inntekt-klas/username;
then
    export  SRVDP_INNTEKT_KLASSIFISERER_USERNAME=$(cat /var/run/secrets/nais.io/service_user/username)
fi
if test -f /secrets/serviceuser/srvdp-inntekt-klas/password;
then
    export  SRVDP_INNTEKT_KLASSIFISERER_PASSWORD=$(cat /var/run/secrets/nais.io/service_user/password)
fi


