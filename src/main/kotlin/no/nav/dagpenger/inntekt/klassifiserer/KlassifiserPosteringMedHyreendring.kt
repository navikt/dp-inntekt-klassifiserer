package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.Postering
import no.nav.dagpenger.events.inntekt.v1.PosteringsType

fun klassifiserPosteringerMedHyreendring(posteringer: List<Postering>): List<KlassifisertPostering> {
    return posteringer.map { KlassifisertPostering(it, klassifiserPosteringsType(it.posteringsType)) }
}

private fun klassifiserPosteringsType(posteringsType: PosteringsType): InntektKlasse {
    return when {
        isArbeidsInntekt(posteringsType) -> InntektKlasse.ARBEIDSINNTEKT
        isFangstFiske(posteringsType) -> InntektKlasse.FANGST_FISKE
        isDagpenger(posteringsType) -> InntektKlasse.DAGPENGER
        isDagpengerFangstFiske(posteringsType) -> InntektKlasse.DAGPENGER_FANGST_FISKE
        isSykepengerFangstFiske(posteringsType) -> InntektKlasse.SYKEPENGER_FANGST_FISKE
        isSykepenger(posteringsType) -> InntektKlasse.SYKEPENGER
        isTiltakslønn(posteringsType) -> InntektKlasse.TILTAKSLØNN
        else -> throw KlassifiseringsException("Unknown inntektklasse for $posteringsType")
    }
}

private fun isArbeidsInntekt(posteringsType: PosteringsType): Boolean {
    val arbeidsPosteringsTyper = listOf(
        PosteringsType.L_SKATTEPLIKTIG_PERSONALRABATT,
        PosteringsType.L_TIPS,
        PosteringsType.L_AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
        PosteringsType.L_ANNET,
        PosteringsType.L_ARBEIDSOPPHOLD_KOST,
        PosteringsType.L_ARBEIDSOPPHOLD_LOSJI,
        PosteringsType.L_BEREGNET_SKATT,
        PosteringsType.L_BESØKSREISER_HJEMMET_ANNET,
        PosteringsType.L_BESØKSREISER_HJEMMET_KILOMETERGODTGJØRELSE_BIL,
        PosteringsType.L_BETALT_UTENLANDSK_SKATT,
        PosteringsType.L_BIL,
        PosteringsType.L_BOLIG,
        PosteringsType.L_BONUS,
        PosteringsType.L_BONUS_FRA_FORSVARET,
        PosteringsType.L_ELEKTRONISK_KOMMUNIKASJON,
        PosteringsType.L_FAST_BILGODTGJØRELSE,
        PosteringsType.L_FAST_TILLEGG,
        PosteringsType.L_FASTLØNN,
        PosteringsType.L_FERIEPENGER,
        PosteringsType.L_FOND_FOR_IDRETTSUTØVERE,
        PosteringsType.Y_FORELDREPENGER,
        PosteringsType.L_HELLIGDAGSTILLEGG,
        PosteringsType.L_HONORAR_AKKORD_PROSENT_PROVISJON,
        PosteringsType.L_HYRETILLEGG,
        PosteringsType.L_INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING,
        PosteringsType.L_KILOMETERGODTGJØRELSE_BIL,
        PosteringsType.L_KOMMUNAL_OMSORGSLØNN_OG_FOSTERHJEMSGODTGJØRELSE,
        PosteringsType.L_KOST_DAGER,
        PosteringsType.L_KOST_DØGN,
        PosteringsType.L_KOSTBESPARELSE_I_HJEMMET,
        PosteringsType.L_LOSJI,
        PosteringsType.L_IKKE_SKATTEPLIKTIG_LØNN_FRA_UTENLANDSK_DIPLOM_KONSUL_STASJON,
        PosteringsType.L_LØNN_FOR_BARNEPASS_I_BARNETS_HJEM,
        PosteringsType.L_LØNN_TIL_PRIVATPERSONER_FOR_ARBEID_I_HJEMMET,
        PosteringsType.L_LØNN_UTBETALT_AV_VELDEDIG_ELLER_ALLMENNYTTIG_INSTITUSJON_ELLER_ORGANISASJON,
        PosteringsType.L_LØNN_TIL_VERGE_FRA_FYLKESMANNEN,
        PosteringsType.L_OPSJONER,
        PosteringsType.L_OVERTIDSGODTGJØRELSE,
        PosteringsType.L_REISE_ANNET,
        PosteringsType.L_REISE_KOST,
        PosteringsType.L_REISE_LOSJI,
        PosteringsType.L_RENTEFORDEL_LÅN,
        PosteringsType.L_SKATTEPLIKTIG_DEL_FORSIKRINGER,
        PosteringsType.L_SLUTTVEDERLAG,
        PosteringsType.L_SMUSSTILLEGG,
        PosteringsType.L_STIPEND,
        PosteringsType.L_STYREHONORAR_OG_GODTGJØRELSE_VERV,
        PosteringsType.Y_SVANGERSKAPSPENGER,
        PosteringsType.L_TIMELØNN,
        PosteringsType.L_TREKK_I_LØNN_FOR_FERIE,
        PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_ARBEIDET_TID,
        PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_IKKE_ARBEIDET_TID,
        PosteringsType.L_YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
        PosteringsType.L_YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
        PosteringsType.L_ANNET_H,
        PosteringsType.L_BONUS_H,
        PosteringsType.L_FAST_TILLEGG_H,
        PosteringsType.L_FASTLØNN_H,
        PosteringsType.L_FERIEPENGER_H,
        PosteringsType.L_HELLIGDAGSTILLEGG_H,
        PosteringsType.L_OVERTIDSGODTGJØRELSE_H,
        PosteringsType.L_SLUTTVEDERLAG_H,
        PosteringsType.L_TIMELØNN_H,
        PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_ARBEIDET_TID_H,
        PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_IKKE_ARBEIDET_TID_H,
        PosteringsType.L_TREKK_I_LØNN_FOR_FERIE_H
    )
    return arbeidsPosteringsTyper.contains(posteringsType)
}

private fun isDagpenger(posteringsType: PosteringsType): Boolean {
    val dagpengerPosteringsTyper = listOf(
        PosteringsType.Y_DAGPENGER_VED_ARBEIDSLØSHET,
        PosteringsType.Y_DAGPENGER_TIL_FISKER_SOM_BARE_HAR_HYRE
    )
    return dagpengerPosteringsTyper.contains(posteringsType)
}

private fun isSykepenger(posteringsType: PosteringsType): Boolean {
    val sykepengerPosteringsTyper = listOf(
        PosteringsType.Y_SYKEPENGER,
        PosteringsType.Y_SYKEPENGER_TIL_FISKER_SOM_BARE_HAR_HYRE
    )
    return sykepengerPosteringsTyper.contains(posteringsType)
}

private fun isFangstFiske(posteringsType: PosteringsType): Boolean {
    val fangstFiskePosteringsTyper = listOf(
        PosteringsType.N_LOTT_KUN_TRYGDEAVGIFT,
        PosteringsType.N_VEDERLAG
    )
    return fangstFiskePosteringsTyper.contains(posteringsType)
}

private fun isDagpengerFangstFiske(posteringsType: PosteringsType): Boolean {
    val dagpengerFangstFiskePosteringsTyper = listOf(
        PosteringsType.N_DAGPENGER_TIL_FISKER
    )
    return dagpengerFangstFiskePosteringsTyper.contains(posteringsType)
}

private fun isSykepengerFangstFiske(posteringsType: PosteringsType): Boolean {
    val sykepengerFangstFiskePosteringsTyper = listOf(
        PosteringsType.N_SYKEPENGER_TIL_FISKER
    )
    return sykepengerFangstFiskePosteringsTyper.contains(posteringsType)
}

private fun isTiltakslønn(posteringsType: PosteringsType): Boolean {
    val tiltakslønnPosteringsTyper = listOf(
        PosteringsType.L_ANNET_T,
        PosteringsType.L_BONUS_T,
        PosteringsType.L_FAST_TILLEGG_T,
        PosteringsType.L_FASTLØNN_T,
        PosteringsType.L_FERIEPENGER_T,
        PosteringsType.L_HELLIGDAGSTILLEGG_T,
        PosteringsType.L_OVERTIDSGODTGJØRELSE_T,
        PosteringsType.L_SLUTTVEDERLAG_T,
        PosteringsType.L_TIMELØNN_T,
        PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_ARBEIDET_TID_T,
        PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_IKKE_ARBEIDET_TID_T,
        PosteringsType.L_TREKK_I_LØNN_FOR_FERIE_T
    )
    return tiltakslønnPosteringsTyper.contains(posteringsType)
}
