package cl.fleetmanager.android.auth

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import cl.fleetmanager.android.R

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
                arrayOf("https://TENANT.onmicrosoft.com/api/read"),
                object : AuthenticationCallback {
                    override fun onSuccess(resultado: IAuthenticationResult) { cont.resume(resultado) }
                    override fun onError(ex: MsalException) { cont.resumeWithException(ex) }
                    override fun onCancel() { cont.resumeWithException(Exception("Cancelado por el usuario")) }
                }
            )
        }
    }

    suspend fun obtenerTokenSilencioso(): String? {
        val app = clientePublico ?: return null
        return suspendCancellableCoroutine { cont ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(cuenta: IAccount?) {
                    if (cuenta == null) { cont.resume(null); return }
                    app.acquireTokenSilentAsync(
                        arrayOf("https://TENANT.onmicrosoft.com/api/read"),
                        cuenta,
                        "https://TENANT.b2clogin.com/TENANT.onmicrosoft.com/B2C_1_signin",
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
