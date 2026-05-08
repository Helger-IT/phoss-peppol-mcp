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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tool exposing the Peppol U-NAPTR DNS resolution chain. Useful for diagnosing why a
 * Participant cannot be looked up: shows the DNS hostname queried (hash + SML zone) and the
 * resolved SMP URL.
 */
public final class PeppolDnsTools
{
  private final EPeppolNetwork m_eNetwork;

  public PeppolDnsTools (@NonNull final EPeppolNetwork eNetwork)
  {
    ValueEnforcer.notNull (eNetwork, "Network");
    m_eNetwork = eNetwork;
  }

  @NonNull
  private IJsonObject _resolveDns (@NonNull final String sPID) throws Exception
  {
    final var aPID = Helper.parseParticipantId (sPID, true);
    final ISMLInfo aSMLInfo = m_eNetwork.getSMLInfo ();
    final var aProvider = PeppolNaptrURLProvider.MUTABLE_INSTANCE;

    final JsonObject aResult = new JsonObject ();
    aResult.add ("participantId", aPID.getURIEncoded ());
    aResult.add ("network", m_eNetwork.name ());
    aResult.add ("smlDnsZone", aSMLInfo.getDNSZone ());

    final String sDnsName = aProvider.getDNSNameOfParticipant (aPID, aSMLInfo);
    aResult.add ("queryDnsName", sDnsName);
    aResult.add ("naptrServiceName", aProvider.getNAPTRServiceName ());

    try
    {
      final URI aSmpUri = aProvider.getSMPURIOfParticipant (aPID, aSMLInfo.getDNSZone ());
      aResult.add ("resolved", true);
      aResult.add ("smpUri", aSmpUri.toString ());
    }
    catch (final SMPDNSResolutionException ex)
    {
      aResult.add ("resolved", false);
      aResult.add ("errorCode", ex.getErrorCode ().getID ());
      aResult.add ("errorMessage", ex.getMessage ());
    }
    return aResult;
  }

  @NonNull
  public SyncToolSpecification resolvePeppolDnsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("resolve_peppol_dns")
                                    .description ("""
                                        Resolves the Peppol U-NAPTR DNS chain for a Participant on the configured \
                                        network. Returns the DNS hostname queried (a SHA-256 hash of the participant \
                                        identifier prefixed onto the SML DNS zone) and — if a NAPTR record is found — \
                                        the resolved SMP base URL. Use this to diagnose why a Participant cannot be \
                                        looked up: missing DNS entries, wrong SML zone, or NAPTR misconfiguration. \
                                        The participantId must be in the format <scheme>:<value>, e.g. \
                                        '0088:4012345678901'.""")
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
      return Helper.executeWithErrorHandling ( () -> _resolveDns (sPID));
    });
  }
}
