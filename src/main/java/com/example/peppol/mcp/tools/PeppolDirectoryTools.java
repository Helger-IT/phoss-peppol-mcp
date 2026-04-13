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

  // -------------------------------------------------------------------------
  // Tool: Search participants by company name
  // -------------------------------------------------------------------------

  private @NonNull CallToolResult _executeSearch (@NonNull final String sCompanyName,
                                                  @Nullable final String sCountryCode,
                                                  final int nMaxResults)
  {
    try
    {
      final var aSB = new StringBuilder (m_eNetwork.getDirectoryURL () + "/search/1.0/json");
      aSB.append ("?name=").append (URLEncoder.encode (sCompanyName, StandardCharsets.UTF_8));
      if (nMaxResults > 0)
        aSB.append ("&resultPageCount=").append (nMaxResults);
      if (StringHelper.isNotEmpty (sCountryCode))
        aSB.append ("&country=")
           .append (URLEncoder.encode (sCountryCode.toUpperCase (Locale.US), StandardCharsets.UTF_8));

      final var aRequest = HttpRequest.newBuilder ()
                                      .uri (URI.create (aSB.toString ()))
                                      .header (CHttpHeader.ACCEPT, "application/json")
                                      .header (CHttpHeader.USER_AGENT, CPhossPeppolMcp.USER_AGENT_PART)
                                      .GET ()
                                      .build ();

      final var aResponse = m_aHttpClient.send (aRequest, HttpResponse.BodyHandlers.ofString ());

      if (aResponse.statusCode () != 200)
        throw new RuntimeException ("Peppol Directory returned HTTP " + aResponse.statusCode ());

      final JsonNode aRoot = MAPPER.readTree (aResponse.body ());
      final List <Map <String, Object>> aResults = new ArrayList <> ();

      if (aRoot.has ("matches"))
      {
        aRoot.get ("matches").forEach (match -> {
          final Map <String, Object> aEntry = new LinkedHashMap <> ();

          if (match.has ("participantID"))
            aEntry.put ("participantId", match.get ("participantID").asString ());

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
            match.get ("docTypes").forEach (dt -> aDocTypes.add (dt.asString ()));
            aEntry.put ("supportedDocumentTypes", aDocTypes);
          }

          aResults.add (aEntry);
        });
      }

      final var aResponseMap = Map.of ("query",
                                       sCompanyName,
                                       "network",
                                       m_eNetwork.name (),
                                       "country",
                                       sCountryCode != null ? sCountryCode : "all",
                                       "totalMatches",
                                       Integer.valueOf (aResults.size ()),
                                       "results",
                                       aResults);

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
                                        Searches the Peppol Directory (the public registry of all Peppol participants)
                                        by company name and optional country code. Use this when a user knows a company
                                        name but not their Peppol participant ID, or wants to discover which companies
                                        in a given country are registered on the Peppol network.
                                        Returns a list of matching participants with their participant IDs and supported
                                        document types. Country codes follow ISO 3166-1 alpha-2, e.g. DE, FR, AT, NO.
                                        """)
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("companyName",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Company name or partial name to search for"),
                                                                                    "countryCode",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional ISO 3166-1 alpha-2 country code to narrow the search, e.g. DE, AT, FR"),
                                                                                    "maxResults",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Maximum number of results to return (default 10, max 100)")),
                                                                            List.of ("companyName"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sCompanyName = (String) aArgs.get ("companyName");
      final String sCountryCode = (String) aArgs.getOrDefault ("countryCode", null);
      final int nMaxResults = ((Number) aArgs.getOrDefault ("maxResults", Integer.valueOf (10))).intValue ();
      return _executeSearch (sCompanyName, sCountryCode, nMaxResults);
    });
  }
}
