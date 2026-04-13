package com.example.peppol.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PredefinedDocumentTypeIdentifierManager;
import com.helger.peppolid.peppol.pidscheme.PeppolParticipantIdentifierSchemeManager;
import com.helger.peppolid.peppol.process.IPeppolPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PredefinedProcessIdentifierManager;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.ObjectMapper;

/**
 * MCP tools for checking whether Peppol identifiers are present in the official Peppol codelists.
 * These tools go beyond syntactic validation: they verify that a given identifier is an officially
 * registered value in the Peppol code lists.
 */
public final class PeppolCodelistTools
{
  private static final ObjectMapper MAPPER = new ObjectMapper ();

  // -------------------------------------------------------------------------
  // Tool 1: Check participant identifier scheme in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private CallToolResult _checkParticipantIdSchemeInCodelist (@NonNull final String sInput)
  {
    try
    {
      // Try to parse as a full participant identifier
      final var aPID = Helper.parseParticipantId (sInput, true);
      final var aScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aPID);
      final String sValue = aPID.getValue ();
      final int nColon = sValue.indexOf (':');
      final var sISO6523Code = nColon > 0 ? sValue.substring (0, nColon) : sValue;

      final Map <String, Object> aResult = new LinkedHashMap <> ();
      aResult.put ("iso6523Code", sISO6523Code);
      aResult.put ("inCodelist", Boolean.valueOf (aScheme != null));

      if (aScheme != null)
      {
        aResult.put ("schemeID", aScheme.getSchemeID ());
        aResult.put ("schemeName", aScheme.getSchemeName ());
        aResult.put ("schemeAgency", aScheme.getSchemeAgency ());
        aResult.put ("countryCode", aScheme.getCountryCode ());
        aResult.put ("state", aScheme.getState ().getID ());
        if (aScheme.getRemovalDate () != null)
          aResult.put ("removalDate", aScheme.getRemovalDate ().toString ());
      }

      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ().writeValueAsString (aResult);
      return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
    }
    catch (final Exception ex)
    {
      return McpSchema.CallToolResult.builder ()
                                     .addTextContent ("Error: " + ex.getMessage ())
                                     .isError (Boolean.TRUE)
                                     .build ();
    }
  }

  @NonNull
  public SyncToolSpecification checkParticipantIdSchemeInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_participant_id_scheme_in_codelist")
                                    .description ("""
                                        Checks whether the identifier scheme (ISO 6523 code) used in a Peppol \
                                        Participant identifier is present in the official Peppol Participant \
                                        identifier scheme codelist. For example, checks whether '0088' (GLN) or \
                                        '0192' (Norwegian org number) is a recognized Peppol scheme. \
                                        You can pass either a full Participant identifier like '0088:4012345678901' \
                                        or just the ISO 6523 scheme code like '0088'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("participantId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Full Participant identifier (e.g. '0088:4012345678901') or just the ISO 6523 scheme code (e.g. '0088')")),
                                                                            List.of ("participantId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      return _checkParticipantIdSchemeInCodelist (sPID);
    });
  }

  // -------------------------------------------------------------------------
  // Tool 2: Check document type identifier in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private CallToolResult _checkDocumentTypeIdInCodelist (@NonNull final String sDTID)
  {
    try
    {
      final IPeppolPredefinedDocumentTypeIdentifier aDocTypeID = PredefinedDocumentTypeIdentifierManager.getDocumentTypeIdentifierOfID (sDTID);

      final Map <String, Object> aResult = new LinkedHashMap <> ();
      aResult.put ("documentTypeId", sDTID);
      aResult.put ("inCodelist", Boolean.valueOf (aDocTypeID != null));

      if (aDocTypeID != null)
      {
        aResult.put ("commonName", aDocTypeID.getCommonName ());
        aResult.put ("scheme", aDocTypeID.getScheme ());
        aResult.put ("state", aDocTypeID.getState ().getID ());
        if (aDocTypeID.getRemovalDate () != null)
          aResult.put ("removalDate", aDocTypeID.getRemovalDate ().toString ());
        aResult.put ("bisVersion", Integer.valueOf (aDocTypeID.getBISVersion ()));
        aResult.put ("domainCommunity", aDocTypeID.getDomainCommunity ());
        aResult.put ("issuedByOpenPeppol", Boolean.valueOf (aDocTypeID.isIssuedByOpenPeppol ()));

        final var aProcessIDs = aDocTypeID.getAllProcessIDs ();
        if (aProcessIDs != null && aProcessIDs.isNotEmpty ())
          aResult.put ("processIDs", aProcessIDs.getAllMapped (IProcessIdentifier::getURIEncoded));
      }

      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ().writeValueAsString (aResult);
      return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
    }
    catch (final Exception ex)
    {
      return McpSchema.CallToolResult.builder ()
                                     .addTextContent ("Error: " + ex.getMessage ())
                                     .isError (Boolean.TRUE)
                                     .build ();
    }
  }

  @NonNull
  public SyncToolSpecification checkDocumentTypeIdInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_document_type_id_in_codelist")
                                    .description ("""
                                        Checks whether a Peppol Document Type identifier is present in the \
                                        official Peppol Document Type codelist. Returns detailed information \
                                        including common name, state (active/deprecated/removed), BIS version, \
                                        domain community, and associated Process IDs if found. \
                                        The value should be the Document Type identifier value, e.g. \
                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##...'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("documentTypeId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol Document Type identifier value to look up in the codelist")),
                                                                            List.of ("documentTypeId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sDTID = (String) request.arguments ().get ("documentTypeId");
      return _checkDocumentTypeIdInCodelist (sDTID);
    });
  }

  // -------------------------------------------------------------------------
  // Tool 3: Check process identifier in codelist
  // -------------------------------------------------------------------------

  private @NonNull CallToolResult _checkProcessIdInCodelist (@NonNull final String sPRID)
  {
    try
    {
      final IPeppolPredefinedProcessIdentifier aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sPRID);

      final Map <String, Object> aResult = new LinkedHashMap <> ();
      aResult.put ("processId", sPRID);
      aResult.put ("inCodelist", Boolean.valueOf (aProcID != null));

      if (aProcID != null)
      {
        aResult.put ("scheme", aProcID.getScheme ());
        aResult.put ("value", aProcID.getValue ());
        aResult.put ("state", aProcID.getState ().getID ());
      }

      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ().writeValueAsString (aResult);
      return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
    }
    catch (final Exception ex)
    {
      return McpSchema.CallToolResult.builder ()
                                     .addTextContent ("Error: " + ex.getMessage ())
                                     .isError (Boolean.TRUE)
                                     .build ();
    }
  }

  @NonNull
  public SyncToolSpecification checkProcessIdInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_process_id_in_codelist")
                                    .description ("""
                                        Checks whether a Peppol Process identifier is present in the official \
                                        Peppol Process identifier codelist. Returns the state \
                                        (active/deprecated/removed) if found. \
                                        The value should be a Process identifier string, e.g. \
                                        'urn:fdc:peppol.eu:2017:poacc:billing:01:1.0'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("processId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol Process identifier value to look up in the codelist")),
                                                                            List.of ("processId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPRID = (String) request.arguments ().get ("processId");
      return _checkProcessIdInCodelist (sPRID);
    });
  }
}
