package app.campfire.network.oidc.crypto

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import org.publicvalue.multiplatform.oidc.encodeForPKCE
import org.publicvalue.multiplatform.oidc.s256
import org.publicvalue.multiplatform.oidc.secureRandomBytes

/**
 * Proof Key for Code Exchange [RFC7636](https://datatracker.ietf.org/doc/html/rfc7636) implementation.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "PKCE", name = "PKCE", exact = true)
class Pkce(
  /** For token request **/
  val codeVerifier: String = verifier(),
  /** For authorization **/
  val codeChallenge: String = challenge(codeVerifier),
) {
  private companion object {
    fun verifier(): String {
      val bytes = secureRandomBytes()
      return bytes.encodeForPKCE()
    }

    fun challenge(codeVerifier: String): String {
      return codeVerifier.s256().encodeForPKCE()
    }
  }
}
