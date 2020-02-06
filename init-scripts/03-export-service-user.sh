#!/usr/bin/env bash

if test -f /secrets/serviceuser/srvdp-inntekt-klas/username;
then
    export  SRVDP_INNTEKT_KLASSIFISERER_USERNAME=$(cat /secrets/serviceuser/srvdp-inntekt-klas/username)
fi
if test -f /secrets/serviceuser/srvdp-inntekt-klas/password;
then
    export  SRVDP_INNTEKT_KLASSIFISERER_PASSWORD=$(cat /secrets/serviceuser/srvdp-inntekt-klas/password)
fi


