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

import com.example.peppol.mcp.CPhossPeppolMcp;
import com.example.peppol.mcp.model.DocumentTypeSupportResult;
import com.example.peppol.mcp.model.EndpointInfo;
import com.example.peppol.mcp.model.ParticipantInfo;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppol.security.PeppolTrustStores;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tools wrapping the phax peppol-smp-client library. Each method returns a
 * {@link SyncToolSpecification}, which bundles the tool definition (name, description, input
 * schema) with its handler implementation. Tool descriptions are deliberately written in natural
 * language so the AI can decide correctly when and how to invoke each tool.
 */
public class PeppolSmpTools
{
  private final EPeppolNetwork m_eNetwork;

  public PeppolSmpTools (@NonNull final EPeppolNetwork eNetwork)
  {
    ValueEnforcer.notNull (eNetwork, "Network");
    m_eNetwork = eNetwork;
  }

  @NonNull
  private SMPClientReadOnly _createSmpClient (@NonNull final IParticipantIdentifier aPID) throws SMPDNSResolutionException
  {
    final var aClient = new SMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, aPID, m_eNetwork.getSMLInfo ());
    // Extend default user agent
    aClient.withHttpClientSettings (hcs -> hcs.setUserAgent (CPhossPeppolMcp.USER_AGENT_PART));
    aClient.setTrustStore (m_eNetwork.isProduction () ? PeppolTrustStores.Config2025.TRUSTSTORE_SMP_PRODUCTION
                                                      : PeppolTrustStores.Config2025.TRUSTSTORE_SMP_TEST);
    return aClient;
  }

  // -------------------------------------------------------------------------
  // Tool 1: Look up a participant
  // -------------------------------------------------------------------------

  @NonNull
  private ParticipantInfo _lookupParticipant (@NonNull final String sPID) throws Exception
  {
    final var ret = new ParticipantInfo ();
    ret.setNetwork (m_eNetwork.name ());

    try
    {
      final var aPID = Helper.parseParticipantId (sPID, true);
      ret.setParticipantId (aPID.getURIEncoded ());

      final var aSmpClient = _createSmpClient (aPID);
      ret.setSmpUrl (aSmpClient.getSMPHostURI ());
      ret.setRegistered (true);
      ret.setMessage ("Participant " +
                      aPID.getURIEncoded () +
                      " is registered on the Peppol " +
                      m_eNetwork.name () +
                      " network");
    }
    catch (final SMPDNSResolutionException ex)
    {
      ret.setParticipantId (sPID);
      ret.setRegistered (false);
      ret.setMessage ("Participant " +
                      sPID +
                      " is not registered on the Peppol " +
                      m_eNetwork.name () +
                      " network (DNS lookup failed)");
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
                                        '0088:4012345678901' for a GLN, '0184:DK12345678' for a Danish CVR number,
                                        or '9915:AT123456789' for an Austrian UID.
                                        Common schemes: 0184 (DK CVR), 0192 (NO org), 9906 (IT VAT), 9915 (AT UID), 0060 (DUNS).
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
      return Helper.executeWithErrorHandling ( () -> _lookupParticipant (sPID));
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

    final var aPID = Helper.parseParticipantId (sPID, true);
    ret.setParticipantId (aPID.getURIEncoded ());
    final var aSmpClient = _createSmpClient (aPID);
    final var aDTID = Helper.parseDocTypeID (sDTID, true);
    ret.setDocumentTypeId (aDTID.getURIEncoded ());

    final var aSignedSM = aSmpClient.getServiceMetadataOrNull (aPID, aDTID, null);
    ret.setSupported (aSignedSM != null);

    if (aSignedSM != null)
    {
      final var aSI = aSignedSM.getServiceMetadata ().getServiceInformation ();
      if (aSI != null && aSI.getProcessList () != null)
        for (final var aProcess : aSI.getProcessList ().getProcess ())
          if (aProcess.getServiceEndpointList () != null)
            for (final var aEndpoint : aProcess.getServiceEndpointList ().getEndpoint ())
              ret.addEndpoint (CIdentifier.getURIEncoded (aProcess.getProcessIdentifier ()), aEndpoint);
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
                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1'
                                        for a Peppol BIS Billing 3.0 invoice.
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
      return Helper.executeWithErrorHandling ( () -> _checkDocumentTypeSupport (sPID, sDTID));
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
    final var aPID = Helper.parseParticipantId (sPID, true);
    final var aSmpClient = _createSmpClient (aPID);
    final var aDTID = Helper.parseDocTypeID (sDTID, true);
    final var aPRID = Helper.parseProcessID (sPRID, true);
    final var aTP = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;
    final var aSignedSM = aSmpClient.getServiceMetadataOrNull (aPID, aDTID, null);

    final var ret = new EndpointInfo ();
    ret.setParticipantID (aPID.getURIEncoded ());
    ret.setDocumentTypeID (aDTID.getURIEncoded ());
    ret.setProcessID (aPRID.getURIEncoded ());
    ret.setFound (false);

    if (aSignedSM != null)
    {
      final var aEndpoint = SMPClientReadOnly.getEndpoint (aSignedSM, aPRID, aTP);
      if (aEndpoint != null)
      {
        ret.setFound (true);
        ret.init (aEndpoint);
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
                                        Retrieves the AS4 endpoint URL for a Peppol Participant for a specific document type.
                                        Use this when you need the actual technical URL to send a document to a company,
                                        for example to configure an access point or diagnose connectivity issues.
                                        Returns the endpoint URL, transport profile, and certificate information.
                                        Note: this currently only checks the Peppol AS4 v2 transport profile.
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
                                                                                            "Peppol process identifier URN, e.g. 'urn:fdc:peppol.eu:2017:poacc:billing:01:1.0'")),
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
      return Helper.executeWithErrorHandling ( () -> _getEndpointUrl (sPID, sDTID, sPRID));
    });
  }
}
