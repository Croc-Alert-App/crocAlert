package crocalert.app.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorInfo
import kotlinx.coroutines.tasks.await

actual object FirebaseAuthClient {

    private var pendingResolver: MultiFactorResolver? = null

    actual suspend fun signIn(email: String, password: String): AuthSignInResult {
        return try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            AuthSignInResult.Success
        } catch (e: FirebaseAuthMultiFactorException) {
            pendingResolver = e.resolver
            AuthSignInResult.MfaRequired
        } catch (e: Exception) {
            AuthSignInResult.Error(e.message ?: "Login failed")
        }
    }

    actual suspend fun verifyTotp(otp: String): AuthSignInResult {
        val resolver = pendingResolver
            ?: return AuthSignInResult.Error("Session expired. Please log in again.")
        return try {
            val hint = resolver.hints.filterIsInstance<TotpMultiFactorInfo>().firstOrNull()
                ?: return AuthSignInResult.Error("No TOTP factor enrolled on this account")
            val assertion = TotpMultiFactorGenerator.getAssertionForSignIn(hint.uid, otp)
            resolver.resolveSignIn(assertion).await()
            pendingResolver = null
            AuthSignInResult.Success
        } catch (e: Exception) {
            AuthSignInResult.Error(e.message ?: "Verification failed")
        }
    }
}
