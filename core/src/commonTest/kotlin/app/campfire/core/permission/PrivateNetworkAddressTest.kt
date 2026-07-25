package app.campfire.core.permission

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class PrivateNetworkAddressTest {

  @Test
  fun rfc1918_ipv4_ranges_are_private() {
    assertThat(isPrivateNetworkAddress("http://192.168.1.202:13378")).isTrue()
    assertThat(isPrivateNetworkAddress("http://10.0.0.5:8080")).isTrue()
    assertThat(isPrivateNetworkAddress("http://172.16.0.1")).isTrue()
    assertThat(isPrivateNetworkAddress("http://172.31.255.254/status")).isTrue()
  }

  @Test
  fun loopback_and_link_local_are_private() {
    assertThat(isPrivateNetworkAddress("http://127.0.0.1:13378")).isTrue()
    assertThat(isPrivateNetworkAddress("http://169.254.10.10")).isTrue()
    assertThat(isPrivateNetworkAddress("https://localhost:3000")).isTrue()
  }

  @Test
  fun local_hostnames_are_private() {
    assertThat(isPrivateNetworkAddress("http://nas.local:13378")).isTrue()
    assertThat(isPrivateNetworkAddress("http://server.lan")).isTrue()
  }

  @Test
  fun ipv6_loopback_ula_and_link_local_are_private() {
    assertThat(isPrivateNetworkAddress("http://[::1]:13378")).isTrue()
    assertThat(isPrivateNetworkAddress("http://[fd00::1]:8080")).isTrue()
    assertThat(isPrivateNetworkAddress("http://[fe80::1]")).isTrue()
  }

  @Test
  fun public_hosts_and_ips_are_not_private() {
    assertThat(isPrivateNetworkAddress("https://audiobooks.example.com")).isFalse()
    assertThat(isPrivateNetworkAddress("https://abs.example.com:13378/status")).isFalse()
    assertThat(isPrivateNetworkAddress("http://8.8.8.8")).isFalse()
    assertThat(isPrivateNetworkAddress("http://172.32.0.1")).isFalse() // just outside 172.16/12
    assertThat(isPrivateNetworkAddress("http://11.0.0.1")).isFalse()
  }

  @Test
  fun credentials_in_url_do_not_confuse_host_parsing() {
    assertThat(isPrivateNetworkAddress("http://user:pass@192.168.1.5:13378")).isTrue()
    assertThat(isPrivateNetworkAddress("http://user:pass@example.com")).isFalse()
  }

  @Test
  fun blank_or_garbage_is_not_private() {
    assertThat(isPrivateNetworkAddress("")).isFalse()
    assertThat(isPrivateNetworkAddress("   ")).isFalse()
  }

  @Test
  fun extractUrlHost_pulls_bare_host_from_url_forms() {
    assertThat(extractUrlHost("https://abs.example.com")).isEqualTo("abs.example.com")
    assertThat(extractUrlHost("https://abs.example.com:13378/status?x=1")).isEqualTo("abs.example.com")
    assertThat(extractUrlHost("http://user:pass@abs.example.com:80/path")).isEqualTo("abs.example.com")
    assertThat(extractUrlHost("abs.example.com:13378")).isEqualTo("abs.example.com")
    assertThat(extractUrlHost("http://[fd00::1]:8080")).isEqualTo("fd00::1")
    assertThat(extractUrlHost("")).isNull()
  }
}
