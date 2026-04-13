package com.example.peppol.mcp.tools;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tools for syntactic validation of Peppol identifiers. These tools check whether an identifier
 * string is well-formed according to Peppol rules, without checking whether the identifier is known
 * in any codelist or registered on the network.
 */
public class PeppolIdentifierValidationTools
{
  private static final ObjectMapper MAPPER = new ObjectMapper ();

  // -------------------------------------------------------------------------
  // Tool 1: Validate participant identifier syntax
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification validateParticipantIdSyntaxTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("validate_participant_id_syntax",
                                                    """
                                                        Validates whether a string is a syntactically correct Peppol participant \
                                                        identifier. This checks format only — it does not verify whether the \
                                                        identifier exists in any codelist or is registered on the Peppol network. \
                                                        The expected format is scheme:value, for example '0088:4012345678901'. \
                                                        Use this before performing a live SMP lookup to catch format errors early.""",
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("participantId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "String to validate as a Peppol participant identifier, e.g. 0088:4012345678901")),
                                                                              List.of ("participantId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String sParticipantId = (String) args.get ("participantId");
      return _validateParticipantIdSyntax (sParticipantId);
    });
  }

  private McpSchema.CallToolResult _validateParticipantIdSyntax (final String sParticipantId)
  {
    try
    {
      var aPID = PeppolIdentifierFactory.INSTANCE.parseParticipantIdentifier (sParticipantId);
      if (aPID == null)
        aPID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sParticipantId);
      if (aPID != null)
      {
        final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                   .writeValueAsString (Map.of ("valid",
                                                                Boolean.TRUE,
                                                                "scheme",
                                                                aPID.getScheme (),
                                                                "value",
                                                                aPID.getValue (),
                                                                "uriEncoded",
                                                                aPID.getURIEncoded ()));
        return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
      }
      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                 .writeValueAsString (Map.of ("valid",
                                                              Boolean.FALSE,
                                                              "error",
                                                              "Not a valid Peppol participant identifier. Expected format: scheme:value (e.g. 0088:4012345678901)"));
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
  // Tool 2: Validate document type identifier syntax
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification validateDocumentTypeIdSyntaxTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("validate_document_type_id_syntax",
                                                    """
                                                        Validates whether a string is a syntactically correct Peppol document type \
                                                        identifier. This checks format only — it does not verify whether the \
                                                        document type is known in the official Peppol codelist. \
                                                        The value should be a document type identifier string, e.g. \
                                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##...'.""",
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("documentTypeId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "String to validate as a Peppol document type identifier")),
                                                                              List.of ("documentTypeId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String sDocTypeId = (String) args.get ("documentTypeId");
      return _validateDocumentTypeIdSyntax (sDocTypeId);
    });
  }

  private McpSchema.CallToolResult _validateDocumentTypeIdSyntax (final String sDocTypeId)
  {
    try
    {
      final var aDocTypeID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme (sDocTypeId);
      if (aDocTypeID != null)
      {
        final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                   .writeValueAsString (Map.of ("valid",
                                                                Boolean.TRUE,
                                                                "scheme",
                                                                aDocTypeID.getScheme (),
                                                                "value",
                                                                aDocTypeID.getValue (),
                                                                "uriEncoded",
                                                                aDocTypeID.getURIEncoded ()));
        return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
      }
      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                 .writeValueAsString (Map.of ("valid",
                                                              Boolean.FALSE,
                                                              "error",
                                                              "Not a valid Peppol document type identifier"));
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
  // Tool 3: Validate process identifier syntax
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification validateProcessIdSyntaxTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("validate_process_id_syntax",
                                                    """
                                                        Validates whether a string is a syntactically correct Peppol process \
                                                        identifier. This checks format only — it does not verify whether the \
                                                        process is known in the official Peppol codelist. \
                                                        The value should be a process identifier string, e.g. \
                                                        'urn:fdc:peppol.eu:2017:poacc:billing:01:1.0'.""",
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("processId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "String to validate as a Peppol process identifier")),
                                                                              List.of ("processId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String sProcessId = (String) args.get ("processId");
      return _validateProcessIdSyntax (sProcessId);
    });
  }

  private McpSchema.CallToolResult _validateProcessIdSyntax (final String sProcessId)
  {
    try
    {
      final var aProcID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme (sProcessId);
      if (aProcID != null)
      {
        final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                   .writeValueAsString (Map.of ("valid",
                                                                Boolean.TRUE,
                                                                "scheme",
                                                                aProcID.getScheme (),
                                                                "value",
                                                                aProcID.getValue (),
                                                                "uriEncoded",
                                                                aProcID.getURIEncoded ()));
        return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
      }
      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                 .writeValueAsString (Map.of ("valid",
                                                              Boolean.FALSE,
                                                              "error",
                                                              "Not a valid Peppol process identifier"));
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
