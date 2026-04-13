package com.example.peppol.mcp.tools;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.peppol.mcp.model.DocumentTypeSupportResult;
import com.example.peppol.mcp.model.EndpointInfo;
import com.example.peppol.mcp.model.ParticipantInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ProcessType;
import com.helger.xsds.peppol.smp1.ServiceInformationType;
import com.helger.xsds.peppol.smp1.SignedServiceMetadataType;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tools wrapping the phax peppol-smp-client library. Each method returns a
 * {@link McpServerFeatures.SyncToolSpecification}, which bundles the tool definition (name,
 * description, input schema) with its handler implementation. Tool descriptions are deliberately
 * written in natural language so the AI can decide correctly when and how to invoke each tool.
 */
public class PeppolSmpTools
{
  private static final Logger LOG = LoggerFactory.getLogger (PeppolSmpTools.class);
  private static final ObjectMapper MAPPER = new ObjectMapper ();

  private static final ISMLInfo SML_INFO = ESML.PEPPOL_PRODUCTION;

  // -------------------------------------------------------------------------
  // Tool 1: Look up a participant
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification lookupParticipantTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("lookup_peppol_participant",
                                                    """
                                                        Looks up whether a company is registered in the Peppol network and retrieves
                                                        their registration details. Use this when a user asks whether a company can
                                                        receive Peppol documents, or wants to know their Peppol registration status.
                                                        The participantId must be in the format <scheme>:<value>, for example
                                                        '0088:4012345678901' for a GLN, or '0184:DK12345678' for a Danish CVR number.
                                                        Common schemes: 0184 (DK CVR), 0192 (NO org), 9906 (IT VAT), 0060 (DUNS).
                                                        """,
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("participantId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "Peppol participant identifier in format scheme:value, e.g. 0088:4012345678901")),
                                                                              List.of ("participantId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String participantId = (String) args.get ("participantId");
      return _executeWithErrorHandling ( () -> _lookupParticipant (participantId));
    });
  }

  @NonNull
  private ParticipantInfo _lookupParticipant (final String participantId) throws Exception
  {
    final IParticipantIdentifier pid = _parseParticipantId (participantId);
    final SMPClientReadOnly smpClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, pid, SML_INFO);
    final String sSMPUrl = smpClient.getSMPHostURI ();

    // Actually query the SMP to verify the participant is registered
    final var aServiceGroup = smpClient.getServiceGroupOrNull (pid);

    final ParticipantInfo info = new ParticipantInfo ();
    info.setParticipantId (participantId);
    info.setSmpUrl (sSMPUrl);
    info.setRegistered (aServiceGroup != null);

    if (aServiceGroup == null)
      throw new IllegalStateException ("Participant '" + participantId + "' is not registered in the Peppol Network");

    return info;
  }

  // -------------------------------------------------------------------------
  // Tool 2: Check document type support
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification checkDocumentTypeSupportTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("check_peppol_document_type_support",
                                                    """
                                                        Checks whether a Peppol participant supports receiving a specific document type,
                                                        such as an invoice, credit note, or order. Use this when a user asks whether
                                                        they can send a particular document type to a specific company.
                                                        The documentTypeId should be a Peppol document type identifier, for example
                                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017...'
                                                        for a Peppol BIS Billing 3.0 invoice. If unsure of the exact ID, use the
                                                        lookup_peppol_participant tool first to discover supported document types.
                                                        """,
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("participantId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "Peppol participant identifier in format scheme:value"),
                                                                                      "documentTypeId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "Peppol document type identifier URN")),
                                                                              List.of ("participantId",
                                                                                       "documentTypeId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String participantId = (String) args.get ("participantId");
      final String documentTypeId = (String) args.get ("documentTypeId");
      return _executeWithErrorHandling ( () -> _checkDocumentTypeSupport (participantId, documentTypeId));
    });
  }

  private DocumentTypeSupportResult _checkDocumentTypeSupport (final String participantId, final String documentTypeId)
                                                                                                                        throws Exception
  {
    final IParticipantIdentifier pid = _parseParticipantId (participantId);
    final SMPClientReadOnly smpClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, pid, SML_INFO);

    final var docTypeId = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme (documentTypeId);
    final SignedServiceMetadataType aSignedSM = smpClient.getServiceMetadataOrNull (pid, docTypeId, null);

    final DocumentTypeSupportResult result = new DocumentTypeSupportResult ();
    result.setParticipantId (participantId);
    result.setDocumentTypeId (documentTypeId);
    result.setSupported (aSignedSM != null);

    if (aSignedSM != null)
    {
      final ServiceInformationType aSI = aSignedSM.getServiceMetadata ().getServiceInformation ();
      if (aSI != null && aSI.getProcessList () != null)
      {
        for (final ProcessType aProcess : aSI.getProcessList ().getProcess ())
        {
          if (aProcess.getServiceEndpointList () != null)
          {
            for (final EndpointType aEndpoint : aProcess.getServiceEndpointList ().getEndpoint ())
            {
              result.addEndpoint (aEndpoint.getTransportProfile (), SMPClientReadOnly.getEndpointAddress (aEndpoint));
            }
          }
        }
      }
    }

    return result;
  }

  // -------------------------------------------------------------------------
  // Tool 3: Get endpoint URL
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification getEndpointUrlTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("get_peppol_endpoint_url",
                                                    """
                                                        Retrieves the AS4 endpoint URL for a Peppol participant for a specific document type.
                                                        Use this when you need the actual technical URL to send a document to a company,
                                                        for example to configure an access point or diagnose connectivity issues.
                                                        Returns the endpoint URL, transport profile, and certificate information.
                                                        """,
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("participantId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "Peppol participant identifier in format scheme:value"),
                                                                                      "documentTypeId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "Peppol document type identifier URN"),
                                                                                      "processId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "Peppol process identifier URN, e.g. urn:fdc:peppol.eu:2017:poacc:billing:01:1.0")),
                                                                              List.of ("participantId",
                                                                                       "documentTypeId",
                                                                                       "processId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String participantId = (String) args.get ("participantId");
      final String documentTypeId = (String) args.get ("documentTypeId");
      final String processId = (String) args.get ("processId");
      return _executeWithErrorHandling ( () -> _getEndpointUrl (participantId, documentTypeId, processId));
    });
  }

  private EndpointInfo _getEndpointUrl (final String participantId, final String documentTypeId, final String processId)
                                                                                                                         throws Exception
  {
    final IParticipantIdentifier pid = _parseParticipantId (participantId);
    final SMPClientReadOnly smpClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, pid, SML_INFO);

    final var docTypeId = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme (documentTypeId);
    final var procId = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme (processId);
    final ISMPTransportProfile transportProfile = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;

    final SignedServiceMetadataType aSignedSM = smpClient.getServiceMetadataOrNull (pid, docTypeId, null);

    final EndpointInfo info = new EndpointInfo ();
    info.setParticipantId (participantId);

    if (aSignedSM != null)
    {
      final EndpointType aEndpoint = SMPClientReadOnly.getEndpoint (aSignedSM, procId, transportProfile);

      if (aEndpoint != null)
      {
        info.setEndpointUrl (SMPClientReadOnly.getEndpointAddress (aEndpoint));
        info.setTransportProfile (transportProfile.getID ());
        final var aCert = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
        info.setCertificateSubject (aCert != null ? aCert.getSubjectX500Principal ().getName () : "not available");
        info.setFound (true);
      }
      else
      {
        info.setFound (false);
      }
    }
    else
    {
      info.setFound (false);
    }

    return info;
  }

  // -------------------------------------------------------------------------
  // Tool 4: Validate participant ID format
  // -------------------------------------------------------------------------

  public McpServerFeatures.SyncToolSpecification validateParticipantIdTool ()
  {
    final McpSchema.Tool tool = new McpSchema.Tool ("validate_peppol_participant_id",
                                                    """
                                                        Validates whether a string is a correctly formatted Peppol participant identifier,
                                                        without performing any network lookup. Use this when a user provides a participant ID
                                                        and you want to check the format before doing a live lookup, or when helping a user
                                                        construct a valid participant ID from a company number or VAT number.
                                                        """,
                                                    new McpSchema.JsonSchema ("object",
                                                                              Map.of ("participantId",
                                                                                      Map.of ("type",
                                                                                              "string",
                                                                                              "description",
                                                                                              "String to validate as a Peppol participant identifier")),
                                                                              List.of ("participantId"),
                                                                              Boolean.FALSE));

    return new McpServerFeatures.SyncToolSpecification (tool, (exchange, args) -> {
      final String participantId = (String) args.get ("participantId");
      return _executeWithErrorHandling ( () -> _validateParticipantId (participantId));
    });
  }

  private Map <String, Object> _validateParticipantId (final String participantId)
  {
    try
    {
      final IParticipantIdentifier pid = _parseParticipantId (participantId);
      return Map.of ("valid",
                     Boolean.TRUE,
                     "scheme",
                     pid.getScheme (),
                     "value",
                     pid.getValue (),
                     "normalized",
                     pid.getURIEncoded ());
    }
    catch (final Exception ex)
    {
      return Map.of ("valid", Boolean.FALSE, "error", ex.getMessage ());
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private IParticipantIdentifier _parseParticipantId (final String participantId)
  {
    final IParticipantIdentifier pid = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (participantId);
    if (pid == null)
      throw new IllegalArgumentException ("Invalid Peppol participant ID format: " +
                                          participantId +
                                          ". Expected format is scheme:value, e.g. 0088:4012345678901");
    return pid;
  }

  @FunctionalInterface
  private interface ThrowingSupplier <T>
  {
    T get () throws Exception;
  }

  private McpSchema.CallToolResult _executeWithErrorHandling (final ThrowingSupplier <?> supplier)
  {
    try
    {
      final Object result = supplier.get ();
      final String json = MAPPER.writerWithDefaultPrettyPrinter ().writeValueAsString (result);
      return McpSchema.CallToolResult.builder ().addTextContent (json).isError (Boolean.FALSE).build ();
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
}
