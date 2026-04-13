/*
 * Copyright (C) 2026 Philip Helger
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.peppol.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.rt.NonBlockingProperties;
import com.helger.base.rt.PropertiesHelper;
import com.helger.io.resource.ClassPathResource;

/**
 * Contains application wide constants.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPhossPeppolMcp
{
  public static final String APP_NAME = "phoss-peppol-mcp-server";
  public static final String APP_TITLE = "phoss Peppol MCP Server";

  /** Current version - from properties file */
  public static final String BUILD_VERSION;
  /** Build timestamp - from properties file */
  public static final String BUILD_TIMESTAMP;

  private static final Logger LOGGER = LoggerFactory.getLogger (CPhossPeppolMcp.class);

  static
  {
    String sProjectVersion = null;
    String sProjectTimestamp = null;
    final NonBlockingProperties aProps = PropertiesHelper.loadProperties (ClassPathResource.getInputStream ("phoss-peppol-mcp-version.properties",
                                                                                                            CPhossPeppolMcp.class.getClassLoader ()));
    if (aProps != null)
    {
      sProjectVersion = aProps.get ("version");
      sProjectTimestamp = aProps.get ("timestamp");
    }
    if (sProjectVersion == null)
    {
      sProjectVersion = "undefined";
      LOGGER.error ("Failed to load phoss Peppol MCP version number. If that happens during development, please rebuild the project.");
    }
    BUILD_VERSION = sProjectVersion;
    if (sProjectTimestamp == null)
    {
      sProjectTimestamp = "undefined";
      LOGGER.error ("Failed to load phoss Peppol MCP timestamp. If that happens during development, please rebuild the project.");
    }
    BUILD_TIMESTAMP = sProjectTimestamp;
  }

  public static final String USER_AGENT_PART = APP_NAME + "/" + BUILD_VERSION + " (" + APP_TITLE + ")";

  private CPhossPeppolMcp ()
  {}
}
