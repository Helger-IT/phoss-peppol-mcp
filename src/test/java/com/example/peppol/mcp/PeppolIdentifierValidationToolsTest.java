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

  @Before
  public void setUp ()
  {
    m_aTools = new PeppolIdentifierValidationTools ();
  }

  // -----------------------------------------------------------------------
  // Participant ID validation tests (pure local, no network needed)
  // -----------------------------------------------------------------------

  @Test
  public void testValidParticipantIdFormat ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId", "0088:4012345678901")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue ("Expected valid=true for valid participant ID", sContent.contains ("\"valid\":true"));
  }

  @Test
  public void testInvalidParticipantIdFormat ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId", "not-a-valid-id")));
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue ("Expected valid=false for invalid participant ID", sContent.contains ("\"valid\":false"));
  }

  @Test
  public void testValidParticipantIdSchemeExtracted ()
  {
    final var aSpec = m_aTools.validateParticipantIdSyntaxTool ();
    final var aResult = aSpec.callHandler ()
                             .apply (null,
                                     new McpSchema.CallToolRequest ("validate_participant_id_syntax",
                                                                    Map.of ("participantId", "0192:123456789")));
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = ((McpSchema.TextContent) aResult.content ().get (0)).text ();
    assertTrue ("Expected scheme 0192 (Norwegian org) in response", sContent.contains ("0192"));
  }

  @Test
  public void testValidParticipantIdSchemeExtracted2 ()
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
    assertTrue ("Expected scheme 0192 (Norwegian org) in response", sContent.contains ("0192"));
  }
}
