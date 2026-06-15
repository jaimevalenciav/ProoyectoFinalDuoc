package cl.truckmanager.android.auth

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import cl.truckmanager.android.R

class GestorAutenticacion(private val contexto: Context) {

    private var clientePublico: ISingleAccountPublicClientApplication? = null

    suspend fun inicializar(): ISingleAccountPublicClientApplication {
        if (clientePublico != null) return clientePublico!!
        return suspendCancellableCoroutine { cont ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                contexto,
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(app: ISingleAccountPublicClientApplication) {
                        clientePublico = app
                        cont.resume(app)
                    }
                    override fun onError(ex: MsalException) { cont.resumeWithException(ex) }
                }
            )
        }
    }

    suspend fun iniciarSesion(actividad: Activity): IAuthenticationResult {
        val app = inicializar()
        return suspendCancellableCoroutine { cont ->
            app.signIn(
                actividad,
                null,
                // openid + offline_access siempre funcionan en B2C.
                // Agrega tu scope de API cuando lo configures en Azure:
                // arrayOf("https://trackmanager.onmicrosoft.com/api/read")
                arrayOf("https://trackmanager.onmicrosoft.com/9c042f66-5b72-4dcf-9f65-7fd1bf196bb6/access_as_user"),
                object : AuthenticationCallback {
                    override fun onSuccess(resultado: IAuthenticationResult) { cont.resume(resultado) }
                    override fun onError(ex: MsalException) { cont.resumeWithException(ex) }
                    override fun onCancel() { cont.resumeWithException(Exception("Cancelado por el usuario")) }
                }
            )
        }
    }

    /** Devuelve el resultado si ya hay una cuenta en caché, null si no hay sesión. */
    suspend fun obtenerSesionExistente(): IAuthenticationResult? {
        val app = clientePublico ?: inicializar()
        return suspendCancellableCoroutine { cont ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(cuenta: IAccount?) {
                    if (cuenta == null) { cont.resume(null); return }
                    app.acquireTokenSilentAsync(
                        arrayOf("https://trackmanager.onmicrosoft.com/9c042f66-5b72-4dcf-9f65-7fd1bf196bb6/access_as_user"),
                        "https://trackmanager.b2clogin.com/trackmanager.onmicrosoft.com/B2C_1_susi",
                        object : SilentAuthenticationCallback {
                            override fun onSuccess(resultado: IAuthenticationResult) { cont.resume(resultado) }
                            override fun onError(ex: MsalException) { cont.resume(null) }
                        }
                    )
                }
                override fun onAccountChanged(anterior: IAccount?, actual: IAccount?) { cont.resume(null) }
                override fun onError(ex: MsalException) { cont.resume(null) }
            })
        }
    }

    suspend fun obtenerTokenSilencioso(): String? {
        val app = clientePublico ?: return null
        return suspendCancellableCoroutine { cont ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(cuenta: IAccount?) {
                    if (cuenta == null) { cont.resume(null); return }
                    app.acquireTokenSilentAsync(
                        arrayOf("https://trackmanager.onmicrosoft.com/9c042f66-5b72-4dcf-9f65-7fd1bf196bb6/access_as_user"),
                        "https://trackmanager.b2clogin.com/trackmanager.onmicrosoft.com/B2C_1_susi",
                        object : SilentAuthenticationCallback {
                            override fun onSuccess(resultado: IAuthenticationResult) { cont.resume(resultado.accessToken) }
                            override fun onError(ex: MsalException) { cont.resume(null) }
                        }
                    )
                }
                override fun onAccountChanged(anterior: IAccount?, actual: IAccount?) { cont.resume(null) }
                override fun onError(ex: MsalException) { cont.resume(null) }
            })
        }
    }

    fun cerrarSesion() {
        clientePublico?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {}
            override fun onError(ex: MsalException) {}
        })
    }
}
