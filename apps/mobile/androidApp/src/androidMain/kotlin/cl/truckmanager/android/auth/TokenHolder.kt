package cl.truckmanager.android.auth

/**
 * Almacén en memoria del access token de Azure B2C.
 * El cliente HTTP lo lee al construir cada request.
 */
object TokenHolder {
    @Volatile var accessToken: String = ""
}
