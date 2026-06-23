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
package com.example.peppol.mcp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.example.peppol.mcp.tools.PeppolSmpTools;
import com.helger.peppol.servicedomain.EPeppolNetwork;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Unit tests for phoss Peppol MCP tools.<br>
 * Level 1 tests: call the tool handler directly, no MCP protocol involved. Use these to verify
 * business logic and error handling quickly.<br>
 * For Level 2 (MCP protocol) testing, run the MCP Inspector: "npx @modelcontextprotocol/inspector
 * java -jar target/peppol-mcp-server.jar".<br>
 * For Level 3 (end-to-end with Claude), configure Claude Desktop's config.json.
 */
public final class PeppolSmpToolsTest
{
  private PeppolSmpTools m_aTools;

  @Before
  public void setUp ()
  {
    m_aTools = new PeppolSmpTools (EPeppolNetwork.TEST);
  }

  // -----------------------------------------------------------------------
  // Network tests — these hit the live Peppol network.
  // Tag with @Tag("integration") and exclude from CI if needed.
  // -----------------------------------------------------------------------

  @Test
  public void testLookupKnownParticipant ()
  {
    // This uses a well-known Peppol test participant — replace with a real one
    // from your network if this is not registered in production SML.
    final var aSpec = m_aTools.lookupParticipantTool ();
    // example Norwegian participant
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     McpSchema.CallToolRequest.builder ("lookup_peppol_participant")
                                                              .arguments (Map.of ("participantId", "0192:991825827"))
                                                              .build ());

    assertNotNull (aResult);
    // If registered: isError=false and body contains smpUrl
    // If not registered: isError=true with a meaningful message
    // Either way, the tool must not throw an exception
    assertNotNull (aResult.content ());
    assertFalse (aResult.content ().isEmpty ());
  }

  @Test
  public void testLookupNonExistentParticipant ()
  {
    final var aSpec = m_aTools.lookupParticipantTool ();
    // unlikely to be registered
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     McpSchema.CallToolRequest.builder ("lookup_peppol_participant")
                                                              .arguments (Map.of ("participantId",
                                                                                  "9997:surely-not-existing"))
                                                              .build ());

    assertNotNull (aResult);
    // The tool gracefully handles non-existent participants: isError=false, registered=false
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue ("Expected registered=false for non-existent participant", sContent.contains ("\"registered\":false"));
  }

  @Test
  public void testGetServiceGroupsKnownParticipant ()
  {
    final var aResult = m_aTools.getSmpServiceGroupsTool ()
                                .callHandler ()
                                .apply (null,
                                        McpSchema.CallToolRequest.builder ("get_smp_service_groups")
                                                                 .arguments (Map.of ("participantId", "0192:991825827"))
                                                                 .build ());
    assertNotNull (aResult);
    assertNotNull (aResult.content ());
    assertFalse (aResult.content ().isEmpty ());
    // Don't assert specific document types — just that the tool produced a structured response
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue ("Expected participantId in response: " + sContent, sContent.contains ("\"participantId\":"));
  }

  @Test
  public void testGetServiceGroupsInvalidParticipantId ()
  {
    final var aResult = m_aTools.getSmpServiceGroupsTool ()
                                .callHandler ()
                                .apply (null,
                                        McpSchema.CallToolRequest.builder ("get_smp_service_groups")
                                                                 .arguments (Map.of ("participantId",
                                                                                     "not-a-valid-pid"))
                                                                 .build ());
    assertNotNull (aResult);
    assertTrue ("Expected isError=true for malformed participant ID", aResult.isError ().booleanValue ());
  }

  @Test
  public void testGetSmpSignatureInfoInvalidParticipant ()
  {
    final var aResult = m_aTools.getSmpSignatureInfoTool ()
                                .callHandler ()
                                .apply (null,
                                        McpSchema.CallToolRequest.builder ("get_smp_signature_info")
                                                                 .arguments (Map.of ("participantId",
                                                                                     "not-a-valid-pid",
                                                                                     "documentTypeId",
                                                                                     "busdox-docid-qns::dummy"))
                                                                 .build ());
    assertNotNull (aResult);
    assertTrue ("Expected isError=true for malformed participant ID", aResult.isError ().booleanValue ());
  }

  @Test
  public void testGetSmpSignatureInfoUnregisteredParticipant ()
  {
    // Syntactically valid PID + valid-shaped doc type ID, but unregistered → tool should
    // either return found=false or surface an error; either way it must not throw.
    final var aResult = m_aTools.getSmpSignatureInfoTool ()
                                .callHandler ()
                                .apply (null,
                                        McpSchema.CallToolRequest.builder ("get_smp_signature_info")
                                                                 .arguments (Map.of ("participantId",
                                                                                     "9999:does-not-exist-1234567890",
                                                                                     "documentTypeId",
                                                                                     "busdox-docid-qns::dummy"))
                                                                 .build ());
    assertNotNull (aResult);
    assertNotNull (aResult.content ());
    assertFalse (aResult.content ().isEmpty ());
  }
}
