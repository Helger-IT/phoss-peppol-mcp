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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.peppol.mcp.CPhossPeppolMcp;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.http.CHttpHeader;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppol.servicedomain.EPeppolNetwork;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * MCP tools wrapping the Peppol Directory REST API (directory.peppol.eu). The Peppol Directory
 * allows searching for registered participants by company name, country, and document type. It is
 * the "yellow pages" of the Peppol network.
 */
public class PeppolDirectoryTools
{
  private static final Logger LOG = LoggerFactory.getLogger (PeppolDirectoryTools.class);

  private final EPeppolNetwork m_eNetwork;
  private final HttpClient m_aHttpClient = HttpClient.newHttpClient ();

  public PeppolDirectoryTools (@NonNull final EPeppolNetwork eNetwork)
  {
    ValueEnforcer.notNull (eNetwork, "Network");
    m_eNetwork = eNetwork;
  }

  @NonNull
  private static String _enc (@NonNull final String s)
  {
    return URLEncoder.encode (s, StandardCharsets.UTF_8);
  }

  @NonNull
  private IJsonArray _parseMatches (@NonNull final IJsonObject aRoot)
  {
    final JsonArray aResults = new JsonArray ();
    final IJsonArray aMatches = aRoot.getAsArray ("matches");
    if (aMatches == null)
      return aResults;

    for (final IJson aMatchJson : aMatches)
    {
      if (!aMatchJson.isObject ())
        continue;
      final IJsonObject aMatch = aMatchJson.getAsObject ();
      final JsonObject aEntry = new JsonObject ();

      final IJson aPIDJson = aMatch.get ("participantID");
      if (aPIDJson != null)
      {
        if (aPIDJson.isObject ())
        {
          final IJsonObject aPIDObj = aPIDJson.getAsObject ();
          final Object aScheme = aPIDObj.getValue ("scheme");
          final Object aValue = aPIDObj.getValue ("value");
          aEntry.add ("participantId",
                      (aScheme != null ? aScheme.toString () : "") + "::" + (aValue != null ? aValue.toString () : ""));
        }
        else
          if (aPIDJson.isValue ())
            aEntry.add ("participantId", String.valueOf (aPIDJson.getAsValue ().getValue ()));
      }

      final IJsonArray aEntities = aMatch.getAsArray ("entities");
      if (aEntities != null)
      {
        for (final IJson aEntityJson : aEntities)
        {
          if (!aEntityJson.isObject ())
            continue;
          final IJsonObject aEntity = aEntityJson.getAsObject ();
          final IJsonArray aNames = aEntity.getAsArray ("name");
          if (aNames != null)
            for (final IJson aNameJson : aNames)
              if (aNameJson.isObject ())
              {
                final Object aName = aNameJson.getAsObject ().getValue ("name");
                if (aName != null)
                  aEntry.add ("companyName", aName.toString ());
              }
          final IJson aCC = aEntity.get ("countryCode");
          if (aCC != null && aCC.isValue ())
            aEntry.add ("country", String.valueOf (aCC.getAsValue ().getValue ()));
        }
      }

      final IJsonArray aDocTypes = aMatch.getAsArray ("docTypes");
      if (aDocTypes != null)
      {
        final JsonArray aDocTypeList = new JsonArray ();
        for (final IJson aDTJson : aDocTypes)
        {
          if (aDTJson.isObject ())
          {
            final IJsonObject aDTObj = aDTJson.getAsObject ();
            final Object aScheme = aDTObj.getValue ("scheme");
            final Object aValue = aDTObj.getValue ("value");
            aDocTypeList.add ((aScheme != null ? aScheme.toString () : "") +
                              "::" +
                              (aValue != null ? aValue.toString () : ""));
          }
          else
            if (aDTJson.isValue ())
              aDocTypeList.add (String.valueOf (aDTJson.getAsValue ().getValue ()));
        }
        aEntry.add ("supportedDocumentTypes", aDocTypeList);
      }

      aResults.add (aEntry);
    }

    return aResults;
  }

  @NonNull
  private HttpResponse <String> _executeRequest (@NonNull final String sURL) throws Exception
  {
    final var aRequest = HttpRequest.newBuilder ()
                                    .uri (URI.create (sURL))
                                    .header (CHttpHeader.ACCEPT, "application/json")
                                    .header (CHttpHeader.USER_AGENT, CPhossPeppolMcp.USER_AGENT_PART)
                                    .GET ()
                                    .build ();

    final var aResponse = m_aHttpClient.send (aRequest, HttpResponse.BodyHandlers.ofString ());

    final int nStatusCode = aResponse.statusCode ();
    if (nStatusCode == 429)
    {
      final String sRetryAfter = aResponse.headers ()
                                          .firstValue (CHttpHeader.RETRY_AFTER)
                                          .orElse (null);
      throw new RuntimeException ("Peppol Directory rate limit exceeded (HTTP 429)." +
                                  (sRetryAfter != null ? " Retry after " + sRetryAfter + " seconds." : ""));
    }
    if (nStatusCode != 200)
      throw new RuntimeException ("Peppol Directory returned HTTP " + nStatusCode);

    return aResponse;
  }

  // -------------------------------------------------------------------------
  // Tool: Search Peppol Directory
  // -------------------------------------------------------------------------

  private @NonNull CallToolResult _executeSearch (@NonNull final String sQuery,
                                                  @Nullable final String sCountryCode,
                                                  final int nMaxResults)
  {
    try
    {
      final var aSB = new StringBuilder (m_eNetwork.getDirectoryURL () + "/search/1.0/json");
      aSB.append ("?q=").append (_enc (sQuery));
      if (nMaxResults > 0)
        aSB.append ("&resultPageCount=").append (nMaxResults);
      if (StringHelper.isNotEmpty (sCountryCode))
        aSB.append ("&country=").append (_enc (sCountryCode.toUpperCase (Locale.US)));

      final var aResponse = _executeRequest (aSB.toString ());
      final long nTotalResultCount = aResponse.headers ()
                                              .firstValueAsLong ("total-result-count")
                                              .orElse (-1);
      final IJsonObject aRoot = JsonReader.builder ().source (aResponse.body ()).readAsObject ();
      final var aResults = _parseMatches (aRoot);

      final JsonObject aResponseObj = new JsonObject ();
      aResponseObj.add ("query", sQuery);
      aResponseObj.add ("network", m_eNetwork.name ());
      aResponseObj.add ("country", sCountryCode != null ? sCountryCode : "all");
      if (nTotalResultCount >= 0)
        aResponseObj.add ("totalResultCount", nTotalResultCount);
      aResponseObj.add ("returnedMatches", aResults.size ());
      aResponseObj.add ("results", aResults);

      return McpSchema.CallToolResult.builder ()
                                     .addTextContent (Helper.JSON_WRITER.writeAsString (aResponseObj))
                                     .isError (Boolean.FALSE)
                                     .build ();
    }
    catch (final Exception ex)
    {
      LOG.error ("Directory search failed", ex);
      return McpSchema.CallToolResult.builder ()
                                     .addTextContent ("Error searching Peppol Directory: " + ex.getMessage ())
                                     .isError (Boolean.TRUE)
                                     .build ();
    }
  }

  @NonNull
  public SyncToolSpecification searchParticipantsByNameTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("search_peppol_directory")
                                    .description ("""
                                        Searches the Peppol Directory (the public registry of all Peppol participants) \
                                        using a generic query that matches across all fields: company name, participant \
                                        ID, country, identifiers, and more. Use this when a user knows a company name, \
                                        an identifier value (like an ATU number), or any other detail, and wants to find \
                                        matching Peppol participants. The countryCode parameter can be used to narrow \
                                        results to a specific country. Country codes follow ISO 3166-1 alpha-2, e.g. \
                                        DE, FR, AT, NO. Note: the Peppol Directory API is rate-limited to 2 queries \
                                        per second.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("query",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Search query — matches across all fields: company name, participant ID, identifiers, etc."),
                                                                                    "countryCode",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional ISO 3166-1 alpha-2 country code to narrow the search, e.g. DE, AT, FR"),
                                                                                    "maxResults",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Maximum number of results to return (default 10, max 1000)")),
                                                                            List.of ("query"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sQuery = (String) aArgs.get ("query");
      final String sCountryCode = (String) aArgs.getOrDefault ("countryCode", null);
      final int nMaxResults = ((Number) aArgs.getOrDefault ("maxResults", Integer.valueOf (10))).intValue ();
      return _executeSearch (sQuery, sCountryCode, nMaxResults);
    });
  }
}
