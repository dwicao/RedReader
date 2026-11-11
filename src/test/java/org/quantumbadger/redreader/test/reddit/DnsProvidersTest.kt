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
package org.quantumbadger.redreader.test.reddit

import okhttp3.dnsoverhttps.DnsOverHttps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.http.okhttp.DnsProviders

class DnsProvidersTest {

	@Test
	fun systemProviderUsesNoCustomDns() {
		assertNull(DnsProviders.dohUrlFor(DnsProviders.PROVIDER_SYSTEM, null))
		assertNull(DnsProviders.currentDns(DnsProviders.PROVIDER_SYSTEM, null))
	}

	@Test
	fun knownProvidersMapToTheirEndpoints() {
		assertEquals(
			"https://cloudflare-dns.com/dns-query",
			DnsProviders.dohUrlFor(DnsProviders.PROVIDER_CLOUDFLARE, null))
		assertEquals(
			"https://dns.google/dns-query",
			DnsProviders.dohUrlFor(DnsProviders.PROVIDER_GOOGLE, null))
		assertEquals(
			"https://dns.quad9.net/dns-query",
			DnsProviders.dohUrlFor(DnsProviders.PROVIDER_QUAD9, null))
		assertEquals(
			"https://dns.adguard-dns.com/dns-query",
			DnsProviders.dohUrlFor(DnsProviders.PROVIDER_ADGUARD, null))
		assertEquals(
			"https://doh.opendns.com/dns-query",
			DnsProviders.dohUrlFor(DnsProviders.PROVIDER_OPENDNS, null))
	}

	@Test
	fun unknownProviderMapsToNothing() {
		assertNull(DnsProviders.dohUrlFor("example", null))
		assertNull(DnsProviders.currentDns("example", null))
		assertTrue(DnsProviders.bootstrapHostsFor("example").isEmpty())
	}

	@Test
	fun knownProvidersHaveBootstrapHosts() {
		assertFalse(
			DnsProviders.bootstrapHostsFor(DnsProviders.PROVIDER_CLOUDFLARE).isEmpty())
		assertFalse(
			DnsProviders.bootstrapHostsFor(DnsProviders.PROVIDER_GOOGLE).isEmpty())
		assertFalse(
			DnsProviders.bootstrapHostsFor(DnsProviders.PROVIDER_QUAD9).isEmpty())
	}

	@Test
	fun customProviderUsesTrimmedCustomUrl() {
		assertEquals(
			"https://dns.example.com/dns-query",
			DnsProviders.dohUrlFor(
				DnsProviders.PROVIDER_CUSTOM,
				"  https://dns.example.com/dns-query  "))
	}

	@Test
	fun customProviderWithBlankUrlMapsToNothing() {
		assertNull(DnsProviders.dohUrlFor(DnsProviders.PROVIDER_CUSTOM, null))
		assertNull(DnsProviders.dohUrlFor(DnsProviders.PROVIDER_CUSTOM, ""))
		assertNull(DnsProviders.dohUrlFor(DnsProviders.PROVIDER_CUSTOM, "   "))
		assertNull(DnsProviders.currentDns(DnsProviders.PROVIDER_CUSTOM, ""))
	}

	@Test
	fun invalidCustomUrlBuildsNoDns() {
		assertNull(DnsProviders.currentDns(DnsProviders.PROVIDER_CUSTOM, "not a url"))
		assertNull(DnsProviders.newDns("not a url"))
		assertNull(DnsProviders.newDns("fake://dns.example.com/dns-query"))
	}

	@Test
	fun validEndpointBuildsDnsOverHttps() {
		val dns = DnsProviders.newDns("https://cloudflare-dns.com/dns-query")
		assertNotNull(dns)
		assertTrue(dns is DnsOverHttps)
	}

	@Test
	fun knownProviderBuildsDns() {
		assertNotNull(
			DnsProviders.currentDns(DnsProviders.PROVIDER_CLOUDFLARE, null))
		assertNotNull(
			DnsProviders.currentDns(
				DnsProviders.PROVIDER_CUSTOM,
				"https://dns.example.com/dns-query"))
	}
}
