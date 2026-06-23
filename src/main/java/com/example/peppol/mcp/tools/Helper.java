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
package com.example.peppol.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.json.IJson;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

@Immutable
final class Helper
{
  private static final Logger LOG = LoggerFactory.getLogger (Helper.class);
  static final JsonWriter JSON_WRITER = new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);

  private Helper ()
  {}

  /**
   * Build an MCP tool input schema as a plain Map, replacing the deprecated
   * McpSchema.JsonSchema type. All Peppol tools use an object schema with a fixed
   * set of properties, the listed required properties and no additional properties.
   *
   * @param aProperties
   *        The JSON schema properties. May not be <code>null</code>.
   * @param aRequired
   *        The list of required property names. May not be <code>null</code> but may be empty.
   * @return The assembled input schema Map. Never <code>null</code>.
   */
  @NonNull
  static Map <String, Object> inputSchema (@NonNull final Map <String, Object> aProperties,
                                           @NonNull final List <String> aRequired)
  {
    final Map <String, Object> ret = new LinkedHashMap <> ();
    ret.put ("type", "object");
    ret.put ("properties", aProperties);
    ret.put ("required", aRequired);
    ret.put ("additionalProperties", Boolean.FALSE);
    return ret;
  }

  /**
   * Get the arguments of a tool call request in a null-safe way. As of MCP SDK 2.0.0,
   * CallToolRequest.arguments () returns <code>null</code> when the caller provides no arguments at
   * all, so tools with only optional parameters must guard against it.
   *
   * @param aRequest
   *        The tool call request. May not be <code>null</code>.
   * @return The request arguments, or an empty Map if none were provided. Never <code>null</code>.
   */
  @NonNull
  static Map <String, Object> getArguments (@NonNull final CallToolRequest aRequest)
  {
    final Map <String, Object> aArgs = aRequest.arguments ();
    return aArgs != null ? aArgs : Map.of ();
  }

  @NonNull
  static CallToolResult executeWithErrorHandling (@NonNull final IThrowingSupplier <IJson, Exception> aSupplier)
  {
    try
    {
      final IJson aResult = aSupplier.get ();
      final String sJson = JSON_WRITER.writeAsString (aResult);
      return McpSchema.CallToolResult.builder ().addTextContent (sJson).isError (Boolean.FALSE).build ();
    }
    catch (final Exception ex)
    {
      LOG.error ("Tool execution failed", ex);
      return McpSchema.CallToolResult.builder ()
                                     .addTextContent ("Error: " + ex.getMessage ())
                                     .isError (Boolean.TRUE)
                                     .build ();
    }
  }

  @Nullable
  static IParticipantIdentifier parseParticipantId (@NonNull final String sPID, final boolean bThrow)
  {
    var aPID = PeppolIdentifierFactory.INSTANCE.parseParticipantIdentifier (sPID);
    if (aPID == null)
      aPID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sPID);
    if (aPID == null && bThrow)
      throw new IllegalArgumentException ("Invalid Peppol Participant ID format '" +
                                          sPID +
                                          "'. Expected format is scheme:value, e.g. 0088:4012345678901");
    return aPID;
  }

  @Nullable
  static IDocumentTypeIdentifier parseDocTypeID (@NonNull final String sDTID, final boolean bThrow)
  {
    var aDTID = PeppolIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (sDTID);
    if (aDTID == null)
      aDTID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme (sDTID);
    if (aDTID == null && bThrow)
      throw new IllegalArgumentException ("Invalid Peppol Document Type ID format '" +
                                          sDTID +
                                          "'. Expected format is scheme::value, e.g. busdox-docid-qns::xyz");
    return aDTID;
  }

  @Nullable
  static IProcessIdentifier parseProcessID (@NonNull final String sPRID, final boolean bThrow)
  {
    var aPRID = PeppolIdentifierFactory.INSTANCE.parseProcessIdentifier (sPRID);
    if (aPRID == null)
      aPRID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme (sPRID);
    if (aPRID == null && bThrow)
      throw new IllegalArgumentException ("Invalid Peppol Process ID format '" +
                                          sPRID +
                                          "'. Expected format is scheme::value, e.g. cenbii-procid-ubl::xyz");
    return aPRID;
  }
}
