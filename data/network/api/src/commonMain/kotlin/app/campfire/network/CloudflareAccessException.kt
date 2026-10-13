// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

/**
 * The server sits behind Cloudflare Access, which answered with its own login challenge instead
 * of letting the request through: the request carried no service token, or one Access rejected.
 *
 * @param teamDomain the Access team domain the challenge redirected to (e.g.
 * `myteam.cloudflareaccess.com`), when the challenge was a redirect.
 */
class CloudflareAccessException(
  val teamDomain: String?,
) : Exception("The server is protected by Cloudflare Access")
