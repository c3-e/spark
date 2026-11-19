/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.ui

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.http.HttpURI
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.HandlerWrapper

import org.apache.spark.internal.Logging

/**
 * A Jetty handler that strips a configured base path prefix from incoming requests
 * before they are dispatched to downstream handlers, and advertises the base path on
 * the request so link-generation code can re-prepend it when producing HTML and
 * redirect targets.
 *
 * This lets Spark sit behind a reverse proxy that forwards the full external URL
 * (e.g. "/my-spark-cluster/jobs/") without requiring any downstream handler
 * (JobsTab, StagesTab, ApiRootResource, static handler, redirect handlers, etc.)
 * to know about the base path -- by the time a request reaches them, the prefix has
 * been removed and they see the same "/jobs/" they would see in a direct-access
 * deployment.
 *
 * Installed once at server level, as an outer wrapper around the
 * [[org.eclipse.jetty.server.handler.ContextHandlerCollection]] (and, when configured,
 * inside [[ProxyRedirectHandler]]). Equivalent in effect to a Jetty
 * `RewriteHandler` with a `RewriteRegexRule`, implemented inline to avoid adding a
 * new `jetty-rewrite` artifact to Spark's shaded distribution.
 *
 * @param rawBasePath the configured base path, with or without leading/trailing slashes
 */
private[spark] class BasePathHandler(rawBasePath: String)
  extends HandlerWrapper with Logging {

  import BasePathHandler._

  /** Normalized base path: always starts with "/", never ends with "/". Never empty. */
  val basePath: String = "/" + rawBasePath.stripPrefix("/").stripSuffix("/")

  require(basePath.length > 1, s"basePath must be non-empty; got '$rawBasePath'")

  override def handle(
      target: String,
      baseRequest: Request,
      request: HttpServletRequest,
      response: HttpServletResponse): Unit = {

    // Advertise basePath on the request so UIUtils.uiRoot() / prependBaseUri() can
    // re-prepend it when rendering links and redirect targets. Using a request
    // attribute (rather than a header) keeps this invisible to application code
    // that inspects headers.
    baseRequest.setAttribute(BASE_PATH_ATTR, basePath)

    val uri = baseRequest.getHttpURI
    val path = if (uri != null) uri.getPath else null

    if (path != null && (path == basePath || path.startsWith(basePath + "/"))) {
      val stripped = path.substring(basePath.length) match {
        case ""    => "/"
        case other => other
      }
      val rewritten = new HttpURI(uri)
      rewritten.setPath(stripped)
      baseRequest.setHttpURI(rewritten)
      // `target` is the value Jetty uses for handler matching; keep it in sync.
      super.handle(stripped, baseRequest, request, response)
    } else {
      super.handle(target, baseRequest, request, response)
    }
  }
}

private[spark] object BasePathHandler {
  /**
   * Request attribute name under which the normalized base path is published,
   * read by [[UIUtils.uiRoot]] to re-prepend it during link generation.
   */
  val BASE_PATH_ATTR = "org.apache.spark.ui.basePath"
}
