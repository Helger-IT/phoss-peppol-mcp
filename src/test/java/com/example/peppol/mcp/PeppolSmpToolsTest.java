package com.example.peppol.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.peppol.mcp.tools.PeppolSmpTools;
import com.helger.peppol.servicedomain.EPeppolNetwork;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Unit tests for Peppol MCP tools. Level 1 tests: call the tool handler directly, no MCP protocol
 * involved. Use these to verify business logic and error handling quickly. For Level 2 (MCP
 * protocol) testing, run the MCP Inspector: npx @modelcontextprotocol/inspector java -jar
 * target/peppol-mcp-server.jar For Level 3 (end-to-end with Claude), configure Claude Desktop's
 * config.json.
 */
class PeppolSmpToolsTest
{
  private PeppolSmpTools tools;

  @BeforeEach
  void setUp ()
  {
    tools = new PeppolSmpTools (EPeppolNetwork.TEST.getSMLInfo ());
  }

  // -----------------------------------------------------------------------
  // Participant ID validation tests (pure local, no network needed)
  // -----------------------------------------------------------------------

  @Test
  void testValidParticipantIdFormat ()
  {
    final McpServerFeatures.SyncToolSpecification spec = tools.validateParticipantIdTool ();
    final McpSchema.CallToolResult result = spec.callHandler ()
                                                .apply (null,
                                                        new McpSchema.CallToolRequest ("validate_peppol_participant_id",
                                                                                       Map.of ("participantId",
                                                                                               "0088:4012345678901")));

    assertFalse (result.isError ().booleanValue ());
    final String content = ((McpSchema.TextContent) result.content ().get (0)).text ();
    assertTrue (content.contains ("\"valid\" : true"), "Expected valid=true for valid participant ID");
  }

  @Test
  void testInvalidParticipantIdFormat ()
  {
    final McpServerFeatures.SyncToolSpecification spec = tools.validateParticipantIdTool ();
    final McpSchema.CallToolResult result = spec.callHandler ()
                                                .apply (null,
                                                        new McpSchema.CallToolRequest ("validate_peppol_participant_id",
                                                                                       Map.of ("participantId",
                                                                                               "not-a-valid-id")));

    // Should return an error result but not throw
    assertNotNull (result);
    final String content = ((McpSchema.TextContent) result.content ().get (0)).text ();
    assertTrue (content.contains ("valid") && content.contains ("false"),
                "Expected valid=false for invalid participant ID");
  }

  @Test
  void testValidParticipantIdSchemeExtracted ()
  {
    final McpServerFeatures.SyncToolSpecification spec = tools.validateParticipantIdTool ();
    final McpSchema.CallToolResult result = spec.callHandler ()
                                                .apply (null,
                                                        new McpSchema.CallToolRequest ("validate_peppol_participant_id",
                                                                                       Map.of ("participantId",
                                                                                               "0192:123456789")));

    assertFalse (result.isError ().booleanValue ());
    final String content = ((McpSchema.TextContent) result.content ().get (0)).text ();
    assertTrue (content.contains ("0192"), "Expected scheme 0192 (Norwegian org) in response");
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
    final McpServerFeatures.SyncToolSpecification spec = tools.lookupParticipantTool ();
    // example Norwegian participant
    final McpSchema.CallToolResult result = spec.callHandler ()
                                                .apply (null,
                                                        new McpSchema.CallToolRequest ("lookup_peppol_participant",
                                                                                       Map.of ("participantId",
                                                                                               "0192:991825827")));

    assertNotNull (result);
    // If registered: isError=false and body contains smpUrl
    // If not registered: isError=true with a meaningful message
    // Either way, the tool must not throw an exception
    assertNotNull (result.content ());
    assertFalse (result.content ().isEmpty ());
  }

  @Test
  void testLookupNonExistentParticipant ()
  {
    final McpServerFeatures.SyncToolSpecification spec = tools.lookupParticipantTool ();
    // unlikely to be registered
    final McpSchema.CallToolResult result = spec.callHandler ()
                                                .apply (null,
                                                        new McpSchema.CallToolRequest ("lookup_peppol_participant",
                                                                                       Map.of ("participantId",
                                                                                               "0088:0000000000000")));

    assertNotNull (result);
    // Should return isError=true with a user-friendly message, not a stack trace
    assertTrue (result.isError ().booleanValue ());
    final String content = ((McpSchema.TextContent) result.content ().get (0)).text ();
    assertFalse (content.contains ("Exception"), "Error message should be user-friendly, not a raw Java exception");
  }
}
