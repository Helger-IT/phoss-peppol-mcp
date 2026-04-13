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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
class PeppolSmpToolsTest
{
  private PeppolSmpTools m_aTools;

  @BeforeEach
  void setUp ()
  {
    m_aTools = new PeppolSmpTools (EPeppolNetwork.TEST);
  }

  // -----------------------------------------------------------------------
  // Network tests — these hit the live Peppol network.
  // Tag with @Tag("integration") and exclude from CI if needed.
  // -----------------------------------------------------------------------

  @Test
  void testLookupKnownParticipant ()
  {
    // This uses a well-known Peppol test participant — replace with a real one
    // from your network if this is not registered in production SML.
    final var aSpec = m_aTools.lookupParticipantTool ();
    // example Norwegian participant
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("lookup_peppol_participant",
                                                                    Map.of ("participantId", "0192:991825827")));

    assertNotNull (aResult);
    // If registered: isError=false and body contains smpUrl
    // If not registered: isError=true with a meaningful message
    // Either way, the tool must not throw an exception
    assertNotNull (aResult.content ());
    assertFalse (aResult.content ().isEmpty ());
  }

  @Test
  void testLookupNonExistentParticipant ()
  {
    final var aSpec = m_aTools.lookupParticipantTool ();
    // unlikely to be registered
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("lookup_peppol_participant",
                                                                    Map.of ("participantId",
                                                                            "9997:surely-not-existing")));

    assertNotNull (aResult);
    // The tool gracefully handles non-existent participants: isError=false, registered=false
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue (sContent.contains ("\"registered\" : false"), "Expected registered=false for non-existent participant");
  }
}
