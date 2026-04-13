package com.example.peppol.mcp.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.helger.peppol.servicedomain.EPeppolNetwork;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * MCP tools wrapping the Peppol Directory REST API (directory.peppol.eu). The Peppol Directory
 * allows searching for registered participants by company name, country, and document type. It is
 * the "yellow pages" of the Peppol network.
 */
public class PeppolDirectoryTools
{
  private static final Logger LOG = LoggerFactory.getLogger (PeppolDirectoryTools.class);
  private static final ObjectMapper MAPPER = new ObjectMapper ();

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
  private List <Map <String, Object>> _parseMatches (@NonNull final JsonNode aRoot)
  {
    final List <Map <String, Object>> aResults = new ArrayList <> ();
    if (!aRoot.has ("matches"))
      return aResults;

    aRoot.get ("matches").forEach (match -> {
      final Map <String, Object> aEntry = new LinkedHashMap <> ();

      if (match.has ("participantID"))
      {
        final JsonNode aPIDNode = match.get ("participantID");
        if (aPIDNode.isObject ())
          aEntry.put ("participantId",
                      aPIDNode.path ("scheme").asString ("") + "::" + aPIDNode.path ("value").asString (""));
        else
          aEntry.put ("participantId", aPIDNode.asString ());
      }

      if (match.has ("entities") && match.get ("entities").isArray ())
      {
        match.get ("entities").forEach (entity -> {
          if (entity.has ("name") && entity.get ("name").isArray ())
            entity.get ("name").forEach (n -> aEntry.put ("companyName", n.get ("name").asString ()));
          if (entity.has ("countryCode"))
            aEntry.put ("country", entity.get ("countryCode").asString ());
        });
      }

      if (match.has ("docTypes") && match.get ("docTypes").isArray ())
      {
        final List <String> aDocTypes = new ArrayList <> ();
        match.get ("docTypes").forEach (dt -> {
          if (dt.isObject ())
            aDocTypes.add (dt.path ("scheme").asString ("") + "::" + dt.path ("value").asString (""));
          else
            aDocTypes.add (dt.asString ());
        });
        aEntry.put ("supportedDocumentTypes", aDocTypes);
      }

      aResults.add (aEntry);
    });

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
      final JsonNode aRoot = MAPPER.readTree (aResponse.body ());
      final var aResults = _parseMatches (aRoot);

      final Map <String, Object> aResponseMap = new LinkedHashMap <> ();
      aResponseMap.put ("query", sQuery);
      aResponseMap.put ("network", m_eNetwork.name ());
      aResponseMap.put ("country", sCountryCode != null ? sCountryCode : "all");
      if (nTotalResultCount >= 0)
        aResponseMap.put ("totalResultCount", Long.valueOf (nTotalResultCount));
      aResponseMap.put ("returnedMatches", Integer.valueOf (aResults.size ()));
      aResponseMap.put ("results", aResults);

      return McpSchema.CallToolResult.builder ()
                                     .addTextContent (MAPPER.writerWithDefaultPrettyPrinter ()
                                                            .writeValueAsString (aResponseMap))
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
