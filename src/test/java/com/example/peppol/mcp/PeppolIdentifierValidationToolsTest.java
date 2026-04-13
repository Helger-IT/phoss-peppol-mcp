package com.example.peppol.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.peppol.mcp.tools.PeppolIdentifierValidationTools;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Test class for class {@link PeppolIdentifierValidationTools}.
 *
 * @author Philip Helger
 */
public final class PeppolIdentifierValidationToolsTest
{
  private PeppolIdentifierValidationTools m_aTools;

  @BeforeEach
  void setUp ()
  {
    m_aTools = new PeppolIdentifierValidationTools ();
  }

  // -----------------------------------------------------------------------
  // Participant ID validation tests (pure local, no network needed)
  // -----------------------------------------------------------------------

  @Test
  void testValidParticipantIdFormat ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId", "0088:4012345678901")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue (sContent.contains ("\"valid\" : true"), "Expected valid=true for valid participant ID");
  }

  @Test
  void testInvalidParticipantIdFormat ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId", "not-a-valid-id")));
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue (sContent.contains ("\"valid\" : false"), "Expected valid=false for invalid participant ID");
  }

  @Test
  void testValidParticipantIdSchemeExtracted ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId", "0192:123456789")));
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue (sContent.contains ("0192"), "Expected scheme 0192 (Norwegian org) in response");
  }

  @Test
  void testValidParticipantIdSchemeExtracted2 ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId",
                                                                            "iso6523-actorid-upis::0192:123456789")));
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue (sContent.contains ("0192"), "Expected scheme 0192 (Norwegian org) in response");
  }
}
