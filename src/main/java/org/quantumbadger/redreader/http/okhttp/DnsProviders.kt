/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.http.okhttp

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

object DnsProviders {

	const val PROVIDER_SYSTEM = "system"
	const val PROVIDER_CLOUDFLARE = "cloudflare"
	const val PROVIDER_GOOGLE = "google"
	const val PROVIDER_QUAD9 = "quad9"
	const val PROVIDER_ADGUARD = "adguard"
	const val PROVIDER_OPENDNS = "opendns"
	const val PROVIDER_CUSTOM = "custom"

	private data class KnownProvider(
		val dohUrl: String,
		val bootstrapIps: List<String>
	)

	private val KNOWN_PROVIDERS = mapOf(
		PROVIDER_CLOUDFLARE to KnownProvider(
			"https://cloudflare-dns.com/dns-query",
			listOf("1.1.1.1", "1.0.0.1")),
		PROVIDER_GOOGLE to KnownProvider(
			"https://dns.google/dns-query",
			listOf("8.8.8.8", "8.8.4.4")),
		PROVIDER_QUAD9 to KnownProvider(
			"https://dns.quad9.net/dns-query",
			listOf("9.9.9.9", "149.112.112.112")),
		PROVIDER_ADGUARD to KnownProvider(
			"https://dns.adguard-dns.com/dns-query",
			listOf("94.140.14.14", "94.140.15.15")),
		PROVIDER_OPENDNS to KnownProvider(
			"https://doh.opendns.com/dns-query",
			listOf("208.67.222.222", "208.67.220.220"))
	)

	fun dohUrlFor(provider: String, customUrl: String?): String? {

		if(provider == PROVIDER_CUSTOM) {
			val trimmed = customUrl?.trim().orEmpty()
			if(trimmed.isEmpty()) {
				return null
			}
			return trimmed
		}

		return KNOWN_PROVIDERS[provider]?.dohUrl
	}

	fun bootstrapHostsFor(provider: String): List<InetAddress> {
		return KNOWN_PROVIDERS[provider]?.bootstrapIps?.mapNotNull {
			try {
				InetAddress.getByName(it)
			} catch(e: Exception) {
				null
			}
		} ?: emptyList()
	}

	fun newDns(dohUrl: String, bootstrapHosts: List<InetAddress> = emptyList()): Dns? {

		val url = dohUrl.trim().toHttpUrlOrNull() ?: return null

		if(url.scheme != "https" && url.scheme != "http") {
			return null
		}

		val builder = DnsOverHttps.Builder()
			.client(OkHttpClient())
			.url(url)

		if(bootstrapHosts.isNotEmpty()) {
			builder.bootstrapDnsHosts(bootstrapHosts)
		}

		return builder.build()
	}

	fun currentDns(provider: String, customUrl: String?): Dns? {

		if(provider == PROVIDER_SYSTEM) {
			return null
		}

		val dohUrl = dohUrlFor(provider, customUrl) ?: return null

		return newDns(dohUrl, bootstrapHostsFor(provider))
	}
}
