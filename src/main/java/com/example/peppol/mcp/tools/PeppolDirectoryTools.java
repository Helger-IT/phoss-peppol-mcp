package com.example.peppol.mcp.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tools wrapping the Peppol Directory REST API (directory.peppol.eu). The Peppol Directory
 * allows searching for registered participants by company name, country, and document type. It is
 * the "yellow pages" of the Peppol network.
 */
public class PeppolDirectoryTools
{
  private static final Logger LOG = LoggerFactory.getLogger (PeppolDirectoryTools.class);
  private static final ObjectMapper MAPPER = new ObjectMapper ();
  private static final String DIRECTORY_API = "https://directory.peppol.eu/search/1.0/json";
  private final HttpClient httpClient = HttpClient.newHttpClient ();

  // -------------------------------------------------------------------------
  // Tool: Search participants by company name
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification searchParticipantsByNameTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("search_peppol_directory",
                                                    """
                                                        Searches the Peppol Directory (the public registry of all Peppol participants)
                                                        by company name and optional country code. Use this when a user knows a company
                                                        name but not their Peppol participant ID, or wants to discover which companies
                                                        in a given country are registered on the Peppol network.
                                                        Returns a list of matching participants with their participant IDs and supported
                                                        document types. Country codes follow ISO 3166-1 alpha-2, e.g. DE, FR, AT, NO.
                                                        """,
                                                    new McpSchema.JsonSchema ("object",
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
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String companyName = (String) args.get ("companyName");
      final String countryCode = (String) args.getOrDefault ("countryCode", null);
      final int maxResults = ((Number) args.getOrDefault ("maxResults", Integer.valueOf (10))).intValue ();
      return _executeSearch (companyName, countryCode, maxResults);
    });
  }

  private McpSchema.CallToolResult _executeSearch (final String companyName,
                                                   final String countryCode,
                                                   final int maxResults)
  {
    try
    {
      // Build the Peppol Directory query URL
      final StringBuilder url = new StringBuilder (DIRECTORY_API);
      url.append ("?name=").append (URLEncoder.encode (companyName, StandardCharsets.UTF_8));
      url.append ("&resultPageCount=").append (Math.min (maxResults, 100));

      if (countryCode != null && !countryCode.isBlank ())
        url.append ("&country=").append (URLEncoder.encode (countryCode.toUpperCase (), StandardCharsets.UTF_8));

      final HttpRequest request = HttpRequest.newBuilder ()
                                             .uri (URI.create (url.toString ()))
                                             .header ("Accept", "application/json")
                                             .GET ()
                                             .build ();

      final HttpResponse <String> response = httpClient.send (request, HttpResponse.BodyHandlers.ofString ());

      if (response.statusCode () != 200)
        throw new RuntimeException ("Peppol Directory returned HTTP " + response.statusCode ());

      final JsonNode root = MAPPER.readTree (response.body ());
      final List <Map <String, Object>> results = new ArrayList <> ();

      if (root.has ("matches"))
      {
        root.get ("matches").forEach (match -> {
          final Map <String, Object> entry = new java.util.LinkedHashMap <> ();

          // Participant ID
          if (match.has ("participantID"))
            entry.put ("participantId", match.get ("participantID").asText ());

          // Company names
          if (match.has ("entities") && match.get ("entities").isArray ())
          {
            match.get ("entities").forEach (entity -> {
              if (entity.has ("name") && entity.get ("name").isArray ())
                entity.get ("name").forEach (n -> entry.put ("companyName", n.get ("name").asText ()));
              if (entity.has ("countryCode"))
                entry.put ("country", entity.get ("countryCode").asText ());
            });
          }

          // Document types
          if (match.has ("docTypes") && match.get ("docTypes").isArray ())
          {
            final List <String> docTypes = new ArrayList <> ();
            match.get ("docTypes").forEach (dt -> docTypes.add (dt.asText ()));
            entry.put ("supportedDocumentTypes", docTypes);
          }

          results.add (entry);
        });
      }

      final Map <String, Object> responseMap = Map.of ("query",
                                                       companyName,
                                                       "country",
                                                       countryCode != null ? countryCode : "all",
                                                       "totalMatches",
                                                       Integer.valueOf (results.size ()),
                                                       "results",
                                                       results);

      return McpSchema.CallToolResult.builder ()
                                     .addTextContent (MAPPER.writerWithDefaultPrettyPrinter ()
                                                            .writeValueAsString (responseMap))
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
}
