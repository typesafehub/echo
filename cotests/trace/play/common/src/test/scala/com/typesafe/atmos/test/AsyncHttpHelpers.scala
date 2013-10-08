/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.test

object AsyncHttpHelpers {
  import java.net.URLEncoder

  def urlEncodeForm(data: Map[String, Seq[String]]): String =
    data.map(item ⇒ item._2.map(c ⇒ item._1 + "=" + URLEncoder.encode(c, "UTF-8"))).flatten.mkString("&")

  import com.ning.http.client.{
    AsyncHttpClient,
    AsyncHttpClientConfig,
    RequestBuilder,
    FluentCaseInsensitiveStringsMap,
    HttpResponseBodyPart,
    HttpResponseHeaders,
    HttpResponseStatus,
    Response ⇒ AHCResponse,
    PerRequestConfig
  }
  import com.ning.http.util.AsyncHttpProviderUtils

  def config = {
    val bc = new AsyncHttpClientConfig.Builder
    bc.setCompressionEnabled(false)
    bc.setAllowPoolingConnection(true)
    bc.setMaximumConnectionsPerHost(1)
    bc.setMaximumConnectionsTotal(1)
    bc.setConnectionTimeoutInMs(10000)
    bc.setRequestTimeoutInMs(10000)
    bc.setFollowRedirects(true)
    bc.build();
  }

  def client = new AsyncHttpClient(config)

  def requestBuilder(verb: String, url: String): RequestBuilder = {
    val builder = new RequestBuilder(verb)
    builder.setUrl(url)
    builder
  }
}