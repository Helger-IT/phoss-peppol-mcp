package com.example.peppol.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PredefinedDocumentTypeIdentifierManager;
import com.helger.peppolid.peppol.pidscheme.IPeppolParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.PeppolParticipantIdentifierSchemeManager;
import com.helger.peppolid.peppol.process.IPeppolPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PredefinedProcessIdentifierManager;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.ObjectMapper;

/**
 * MCP tools for checking whether Peppol identifiers are present in the official Peppol codelists.
 * These tools go beyond syntactic validation: they verify that a given identifier is an officially
 * registered value in the Peppol code lists.
 */
public class PeppolCodelistTools
{
  private static final ObjectMapper MAPPER = new ObjectMapper ();

  // -------------------------------------------------------------------------
  // Tool 1: Check participant identifier scheme in codelist
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification checkParticipantIdSchemeInCodelistTool ()
  {
    final McpSchema.Tool tool = McpSchema.Tool.builder ()
                                              .name ("check_participant_id_scheme_in_codelist")
                                              .description ("""
                                                  Checks whether the identifier scheme (ISO 6523 code) used in a Peppol \
                                                  participant identifier is present in the official Peppol participant \
                                                  identifier scheme codelist. For example, checks whether '0088' (GLN) or \
                                                  '0192' (Norwegian org number) is a recognized Peppol scheme. \
                                                  You can pass either a full participant identifier like '0088:4012345678901' \
                                                  or just the ISO 6523 scheme code like '0088'.""")
                                              .inputSchema (new McpSchema.JsonSchema ("object",
                                                                                      Map.of ("participantId",
                                                                                              Map.of ("type",
                                                                                                      "string",
                                                                                                      "description",
                                                                                                      "Full participant identifier (e.g. '0088:4012345678901') or just the ISO 6523 scheme code (e.g. '0088')")),
                                                                                      List.of ("participantId"),
                                                                                      Boolean.FALSE,
                                                                                      null,
                                                                                      null))
                                              .build ();

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, request) -> {
      final String sInput = (String) request.arguments ().get ("participantId");
      return _checkParticipantIdSchemeInCodelist (sInput);
    });
  }

  private McpSchema.CallToolResult _checkParticipantIdSchemeInCodelist (final String sInput)
  {
    try
    {
      // Try to parse as a full participant identifier first; fall back to treating as ISO 6523 code
      IPeppolParticipantIdentifierScheme aScheme = null;
      String sISO6523Code;

      final var aPID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sInput);
      if (aPID != null)
      {
        aScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aPID);
        // Extract the ISO 6523 prefix from the value (part before the first colon)
        final String sValue = aPID.getValue ();
        final int nColon = sValue.indexOf (':');
        sISO6523Code = nColon > 0 ? sValue.substring (0, nColon) : sValue;
      }
      else
      {
        // Treat as raw ISO 6523 code
        sISO6523Code = sInput;
        aScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfISO6523Code (sISO6523Code);
      }

      final Map <String, Object> aResult = new LinkedHashMap <> ();
      aResult.put ("iso6523Code", sISO6523Code);
      aResult.put ("inCodelist", Boolean.valueOf (aScheme != null));

      if (aScheme != null)
      {
        aResult.put ("schemeID", aScheme.getSchemeID ());
        aResult.put ("schemeName", aScheme.getSchemeName ());
        aResult.put ("countryCode", aScheme.getCountryCode ());
        aResult.put ("state", aScheme.getState ().getID ());
        aResult.put ("deprecated", Boolean.valueOf (aScheme.isDeprecated ()));
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

  // -------------------------------------------------------------------------
  // Tool 2: Check document type identifier in codelist
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification checkDocumentTypeIdInCodelistTool ()
  {
    final McpSchema.Tool tool = McpSchema.Tool.builder ()
                                              .name ("check_document_type_id_in_codelist")
                                              .description ("""
                                                  Checks whether a Peppol document type identifier is present in the \
                                                  official Peppol document type codelist. Returns detailed information \
                                                  including common name, state (active/deprecated/removed), BIS version, \
                                                  domain community, and associated process IDs if found. \
                                                  The value should be the document type identifier value, e.g. \
                                                  'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##...'.""")
                                              .inputSchema (new McpSchema.JsonSchema ("object",
                                                                                      Map.of ("documentTypeId",
                                                                                              Map.of ("type",
                                                                                                      "string",
                                                                                                      "description",
                                                                                                      "Peppol document type identifier value to look up in the codelist")),
                                                                                      List.of ("documentTypeId"),
                                                                                      Boolean.FALSE,
                                                                                      null,
                                                                                      null))
                                              .build ();

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, request) -> {
      final String sDocTypeId = (String) request.arguments ().get ("documentTypeId");
      return _checkDocumentTypeIdInCodelist (sDocTypeId);
    });
  }

  private McpSchema.CallToolResult _checkDocumentTypeIdInCodelist (final String sDocTypeId)
  {
    try
    {
      final IPeppolPredefinedDocumentTypeIdentifier aDocType = PredefinedDocumentTypeIdentifierManager.getDocumentTypeIdentifierOfID (sDocTypeId);

      final Map <String, Object> aResult = new LinkedHashMap <> ();
      aResult.put ("documentTypeId", sDocTypeId);
      aResult.put ("inCodelist", Boolean.valueOf (aDocType != null));

      if (aDocType != null)
      {
        aResult.put ("commonName", aDocType.getCommonName ());
        aResult.put ("scheme", aDocType.getScheme ());
        aResult.put ("state", aDocType.getState ().getID ());
        aResult.put ("deprecated", Boolean.valueOf (aDocType.isDeprecated ()));
        if (aDocType.getRemovalDate () != null)
          aResult.put ("removalDate", aDocType.getRemovalDate ().toString ());
        aResult.put ("bisVersion", Integer.valueOf (aDocType.getBISVersion ()));
        aResult.put ("domainCommunity", aDocType.getDomainCommunity ());
        aResult.put ("issuedByOpenPeppol", Boolean.valueOf (aDocType.isIssuedByOpenPeppol ()));

        final var aProcessIDs = aDocType.getAllProcessIDs ();
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

  // -------------------------------------------------------------------------
  // Tool 3: Check process identifier in codelist
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification checkProcessIdInCodelistTool ()
  {
    final McpSchema.Tool tool = McpSchema.Tool.builder ()
                                              .name ("check_process_id_in_codelist")
                                              .description ("""
                                                  Checks whether a Peppol process identifier is present in the official \
                                                  Peppol process identifier codelist. Returns the state \
                                                  (active/deprecated/removed) if found. \
                                                  The value should be a process identifier string, e.g. \
                                                  'urn:fdc:peppol.eu:2017:poacc:billing:01:1.0'.""")
                                              .inputSchema (new McpSchema.JsonSchema ("object",
                                                                                      Map.of ("processId",
                                                                                              Map.of ("type",
                                                                                                      "string",
                                                                                                      "description",
                                                                                                      "Peppol process identifier value to look up in the codelist")),
                                                                                      List.of ("processId"),
                                                                                      Boolean.FALSE,
                                                                                      null,
                                                                                      null))
                                              .build ();

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, request) -> {
      final String sProcessId = (String) request.arguments ().get ("processId");
      return _checkProcessIdInCodelist (sProcessId);
    });
  }

  private McpSchema.CallToolResult _checkProcessIdInCodelist (final String sProcessId)
  {
    try
    {
      final IPeppolPredefinedProcessIdentifier aProcId = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProcessId);

      final Map <String, Object> aResult = new LinkedHashMap <> ();
      aResult.put ("processId", sProcessId);
      aResult.put ("inCodelist", Boolean.valueOf (aProcId != null));

      if (aProcId != null)
      {
        aResult.put ("scheme", aProcId.getScheme ());
        aResult.put ("value", aProcId.getValue ());
        aResult.put ("state", aProcId.getState ().getID ());
        aResult.put ("deprecated", Boolean.valueOf (aProcId.isDeprecated ()));
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
}
