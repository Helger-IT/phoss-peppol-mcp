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

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.ObjectMapper;

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

  @NonNull
  private CallToolResult _validateParticipantIdSyntax (@NonNull final String sPID)
  {
    try
    {
      final var aPID = Helper.parseParticipantId (sPID, false);
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
                                                              "Not a valid Peppol Participant identifier. Expected format: scheme:value (e.g. 0088:4012345678901)"));
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
  public SyncToolSpecification validateParticipantIdSyntaxTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("validate_participant_id_syntax")
                                    .description ("""
                                        Validates whether a string is a syntactically correct Peppol Participant \
                                        identifier. This checks format only — it does not verify whether the \
                                        identifier exists in any codelist or is registered on the Peppol network. \
                                        The expected format is scheme:value, for example '0088:4012345678901'. \
                                        Use this before performing a live SMP lookup to catch format errors early.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("participantId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "String to validate as a Peppol Participant identifier, e.g. 0088:4012345678901")),
                                                                            List.of ("participantId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      return _validateParticipantIdSyntax (sPID);
    });
  }

  // -------------------------------------------------------------------------
  // Tool 2: Validate document type identifier syntax
  // -------------------------------------------------------------------------

  @NonNull
  private CallToolResult _validateDocumentTypeIdSyntax (@NonNull final String sDTID)
  {
    try
    {
      final var aDTID = Helper.parseDocTypeID (sDTID, false);
      if (aDTID != null)
      {
        final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                   .writeValueAsString (Map.of ("valid",
                                                                Boolean.TRUE,
                                                                "scheme",
                                                                aDTID.getScheme (),
                                                                "value",
                                                                aDTID.getValue (),
                                                                "uriEncoded",
                                                                aDTID.getURIEncoded ()));
        return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
      }
      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                 .writeValueAsString (Map.of ("valid",
                                                              Boolean.FALSE,
                                                              "error",
                                                              "Not a valid Peppol Document Type identifier"));
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
  public SyncToolSpecification validateDocumentTypeIdSyntaxTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("validate_document_type_id_syntax")
                                    .description ("""
                                        Validates whether a string is a syntactically correct Peppol Document Type \
                                        identifier. This checks format only — it does not verify whether the \
                                        Document Type is known in the official Peppol codelist. \
                                        The value should be a Document Type identifier string, e.g. \
                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##...'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("documentTypeId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "String to validate as a Peppol Document Type identifier")),
                                                                            List.of ("documentTypeId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sDTID = (String) request.arguments ().get ("documentTypeId");
      return _validateDocumentTypeIdSyntax (sDTID);
    });
  }

  // -------------------------------------------------------------------------
  // Tool 3: Validate process identifier syntax
  // -------------------------------------------------------------------------

  @NonNull
  private CallToolResult _validateProcessIdSyntax (@NonNull final String sPRID)
  {
    try
    {
      final var aPRID = Helper.parseProcessID (sPRID, false);
      if (aPRID != null)
      {
        final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                   .writeValueAsString (Map.of ("valid",
                                                                Boolean.TRUE,
                                                                "scheme",
                                                                aPRID.getScheme (),
                                                                "value",
                                                                aPRID.getValue (),
                                                                "uriEncoded",
                                                                aPRID.getURIEncoded ()));
        return McpSchema.CallToolResult.builder ().addTextContent (sJSON).isError (Boolean.FALSE).build ();
      }
      final String sJSON = MAPPER.writerWithDefaultPrettyPrinter ()
                                 .writeValueAsString (Map.of ("valid",
                                                              Boolean.FALSE,
                                                              "error",
                                                              "Not a valid Peppol Process identifier"));
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
  public SyncToolSpecification validateProcessIdSyntaxTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("validate_process_id_syntax")
                                    .description ("""
                                        Validates whether a string is a syntactically correct Peppol Process \
                                        identifier. This checks format only — it does not verify whether the \
                                        Process is known in the official Peppol codelist. \
                                        The value should be a Process identifier string, e.g. \
                                        'urn:fdc:peppol.eu:2017:poacc:billing:01:1.0'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("processId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "String to validate as a Peppol Process identifier")),
                                                                            List.of ("processId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPRID = (String) request.arguments ().get ("processId");
      return _validateProcessIdSyntax (sPRID);
    });
  }
}
