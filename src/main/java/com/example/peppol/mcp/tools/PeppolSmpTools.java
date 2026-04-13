package com.example.peppol.mcp.tools;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.peppol.mcp.model.DocumentTypeSupportResult;
import com.example.peppol.mcp.model.EndpointInfo;
import com.example.peppol.mcp.model.ParticipantInfo;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.ObjectMapper;

/**
 * MCP tools wrapping the phax peppol-smp-client library. Each method returns a
 * {@link SyncToolSpecification}, which bundles the tool definition (name, description, input
 * schema) with its handler implementation. Tool descriptions are deliberately written in natural
 * language so the AI can decide correctly when and how to invoke each tool.
 */
public class PeppolSmpTools
{
  private static final Logger LOG = LoggerFactory.getLogger (PeppolSmpTools.class);
  private static final ObjectMapper MAPPER = new ObjectMapper ();

  private final ISMLInfo m_aSmlInfo;

  public PeppolSmpTools (@NonNull final ISMLInfo aSmlInfo)
  {
    ValueEnforcer.notNull (aSmlInfo, "SmlInfo");
    m_aSmlInfo = aSmlInfo;
  }

  private McpSchema.@NonNull CallToolResult _executeWithErrorHandling (@NonNull final IThrowingSupplier <?, Exception> supplier)
  {
    try
    {
      final Object aResult = supplier.get ();
      final String sJson = MAPPER.writerWithDefaultPrettyPrinter ().writeValueAsString (aResult);
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

  // -------------------------------------------------------------------------
  // Tool 1: Look up a participant
  // -------------------------------------------------------------------------

  @NonNull
  static IParticipantIdentifier parseParticipantId (@NonNull final String sPID)
  {
    var aPID = PeppolIdentifierFactory.INSTANCE.parseParticipantIdentifier (sPID);
    if (aPID == null)
      aPID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sPID);
    if (aPID == null)
      throw new IllegalArgumentException ("Invalid Peppol Participant ID format '" +
                                          sPID +
                                          "'. Expected format is scheme:value, e.g. 0088:4012345678901");
    return aPID;
  }

  @NonNull
  static IDocumentTypeIdentifier parseDocTypeID (@NonNull final String sDTID)
  {
    var aDTID = PeppolIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (sDTID);
    if (aDTID == null)
      aDTID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme (sDTID);
    if (aDTID == null)
      throw new IllegalArgumentException ("Invalid Peppol Document Type ID format '" +
                                          sDTID +
                                          "'. Expected format is scheme::value, e.g. busdox-docid-qns::xyz");
    return aDTID;
  }

  @NonNull
  static IProcessIdentifier parseProcessID (@NonNull final String sPRID)
  {
    var aPRID = PeppolIdentifierFactory.INSTANCE.parseProcessIdentifier (sPRID);
    if (aPRID == null)
      aPRID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme (sPRID);
    if (aPRID == null)
      throw new IllegalArgumentException ("Invalid Peppol Process ID format '" +
                                          sPRID +
                                          "'. Expected format is scheme::value, e.g. cenbii-procid-ubl::xyz");
    return aPRID;
  }

  @NonNull
  private ParticipantInfo _lookupParticipant (@NonNull final String sPID) throws Exception
  {
    final var ret = new ParticipantInfo ();
    ret.setParticipantId (sPID);

    try
    {
      final var aPID = parseParticipantId (sPID);
      final var aSmpClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, aPID, m_aSmlInfo);
      ret.setSmpUrl (aSmpClient.getSMPHostURI ());

      // Actually query the SMP to verify the participant is registered
      final var aServiceGroup = aSmpClient.getServiceGroupOrNull (aPID);
      ret.setRegistered (aServiceGroup != null);
    }
    catch (final SMPDNSResolutionException ex)
    {
      ret.setSmpUrl (null);
      ret.setRegistered (false);
    }
    return ret;
  }

  @NonNull
  public SyncToolSpecification lookupParticipantTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("lookup_peppol_participant")
                                    .description ("""
                                        Looks up whether a company is registered in the Peppol Network and retrieves
                                        their registration details. Use this when a user asks whether a company can
                                        receive Peppol documents, or wants to know their Peppol registration status.
                                        The participantId must be in the format <scheme>:<value>, for example
                                        '0088:4012345678901' for a GLN, or '0184:DK12345678' for a Danish CVR number.
                                        Common schemes: 0184 (DK CVR), 0192 (NO org), 9906 (IT VAT), 0060 (DUNS).
                                        """)
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("participantId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol participant identifier in format scheme:value, e.g. 0088:4012345678901")),
                                                                            List.of ("participantId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      return _executeWithErrorHandling ( () -> _lookupParticipant (sPID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 2: Check document type support
  // -------------------------------------------------------------------------

  @NonNull
  private DocumentTypeSupportResult _checkDocumentTypeSupport (@NonNull final String sPID, @NonNull final String sDTID)
                                                                                                                        throws Exception
  {
    final var ret = new DocumentTypeSupportResult ();
    ret.setParticipantId (sPID);
    ret.setDocumentTypeId (sDTID);

    final var aPID = parseParticipantId (sPID);
    final var aSmpClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, aPID, m_aSmlInfo);

    final var aDTID = parseDocTypeID (sDTID);

    final var aSignedSM = aSmpClient.getServiceMetadataOrNull (aPID, aDTID, null);
    ret.setSupported (aSignedSM != null);

    if (aSignedSM != null)
    {
      final var aSI = aSignedSM.getServiceMetadata ().getServiceInformation ();
      if (aSI != null && aSI.getProcessList () != null)
        for (final var aProcess : aSI.getProcessList ().getProcess ())
          if (aProcess.getServiceEndpointList () != null)
            for (final var aEndpoint : aProcess.getServiceEndpointList ().getEndpoint ())
              ret.addEndpoint (aEndpoint.getTransportProfile (), SMPClientReadOnly.getEndpointAddress (aEndpoint));
    }

    return ret;
  }

  @NonNull
  public SyncToolSpecification checkDocumentTypeSupportTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_peppol_document_type_support")
                                    .description ("""
                                        Checks whether a Peppol participant supports receiving a specific document type,
                                        such as an invoice, credit note, or order. Use this when a user asks whether
                                        they can send a particular document type to a specific company.
                                        The documentTypeId should be a Peppol document type identifier, for example
                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017...'
                                        for a Peppol BIS Billing 3.0 invoice. If unsure of the exact ID, use the
                                        lookup_peppol_participant tool first to discover supported document types.
                                        """)
                                    .inputSchema (new McpSchema.JsonSchema ("object",
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
                                                                            List.of ("participantId", "documentTypeId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      final String sDTID = (String) request.arguments ().get ("documentTypeId");
      return _executeWithErrorHandling ( () -> _checkDocumentTypeSupport (sPID, sDTID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 3: Get endpoint URL
  // -------------------------------------------------------------------------

  @NonNull
  private EndpointInfo _getEndpointUrl (@NonNull final String sPID,
                                        @NonNull final String sDTID,
                                        @NonNull final String sPRID) throws Exception
  {
    final var aPID = parseParticipantId (sPID);
    final var aSmpClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, aPID, m_aSmlInfo);
    final var aDTID = parseDocTypeID (sDTID);
    final var aPRID = parseProcessID (sPRID);
    final var aTP = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;
    final var aSignedSM = aSmpClient.getServiceMetadataOrNull (aPID, aDTID, null);

    final var ret = new EndpointInfo ();
    ret.setParticipantID (sPID);
    ret.setDocumentTypeID (sDTID);
    ret.setProcessID (sPRID);
    ret.setFound (false);

    if (aSignedSM != null)
    {
      final var aEndpoint = SMPClientReadOnly.getEndpoint (aSignedSM, aPRID, aTP);
      if (aEndpoint != null)
      {
        ret.setFound (true);
        ret.setEndpointUrl (SMPClientReadOnly.getEndpointAddress (aEndpoint));
        ret.setTransportProfile (aTP.getID ());
        final var aCert = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
        if (aCert != null)
        {
          ret.setCertificateIssuer (aCert.getIssuerX500Principal ().getName ());
          ret.setCertificateSubject (aCert.getSubjectX500Principal ().getName ());
        }
      }
    }
    return ret;
  }

  @NonNull
  public SyncToolSpecification getEndpointUrlTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("get_peppol_endpoint_url")
                                    .description ("""
                                        Retrieves the AS4 endpoint URL for a Peppol participant for a specific document type.
                                        Use this when you need the actual technical URL to send a document to a company,
                                        for example to configure an access point or diagnose connectivity issues.
                                        Returns the endpoint URL, transport profile, and certificate information.
                                        """)
                                    .inputSchema (new McpSchema.JsonSchema ("object",
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
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      final String sDTID = (String) request.arguments ().get ("documentTypeId");
      final String sPRID = (String) request.arguments ().get ("processId");
      return _executeWithErrorHandling ( () -> _getEndpointUrl (sPID, sDTID, sPRID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 4: Validate participant ID format
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _validateParticipantId (@NonNull final String sPID)
  {
    try
    {
      final IParticipantIdentifier pid = parseParticipantId (sPID);
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

  @NonNull
  public SyncToolSpecification validateParticipantIdTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("validate_peppol_participant_id")
                                    .description ("""
                                        Validates whether a string is a correctly formatted Peppol Participant identifier,
                                        without performing any network lookup. Use this when a user provides a participant ID
                                        and you want to check the format before doing a live lookup, or when helping a user
                                        construct a valid participant ID from a company number or VAT number.
                                        """)
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("participantId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "String to validate as a Peppol Participant identifier")),
                                                                            List.of ("participantId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      return _executeWithErrorHandling ( () -> _validateParticipantId (sPID));
    });
  }
}
