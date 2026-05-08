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

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.example.peppol.mcp.tools.PeppolDnsTools;
import com.helger.peppol.servicedomain.EPeppolNetwork;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Unit tests for {@link PeppolDnsTools}. The "computes DNS name" assertions are local; the
 * "resolves" assertions hit the Peppol test SML.
 */
public final class PeppolDnsToolsTest
{
  private PeppolDnsTools m_aTools;

  @Before
  public void setUp ()
  {
    m_aTools = new PeppolDnsTools (EPeppolNetwork.TEST);
  }

  @NonNull
  private CallToolResult _call (@NonNull final String sPID)
  {
    return m_aTools.resolvePeppolDnsTool ()
                   .callHandler ()
                   .apply (null, new McpSchema.CallToolRequest ("resolve_peppol_dns", Map.of ("participantId", sPID)));
  }

  @NonNull
  private static String _textOf (@NonNull final CallToolResult aResult)
  {
    return ((McpSchema.TextContent) aResult.content ().get (0)).text ();
  }

  @Test
  public void testValidParticipantId ()
  {
    final CallToolResult aResult = _call ("9915:helger");
    assertNotNull (aResult);
    assertFalse ("Expected isError=false for valid participant ID", aResult.isError ().booleanValue ());
    final String sBody = _textOf (aResult);
    assertTrue ("Expected the queried DNS name to be present: " + sBody, sBody.contains ("\"queryDnsName\":"));
    assertTrue ("Expected the SML zone to be present: " + sBody,
                sBody.contains ("\"smlDnsZone\":\"participant.sml.test.tech.peppol.org.\""));
    assertTrue ("Expected resolved=true for registered participant: " + sBody, sBody.contains ("\"resolved\":true"));
    assertTrue ("Expected SMP URL for registered participant: " + sBody,
                sBody.contains ("\"smpUri\":\"https://smp.helger.com\""));
  }

  @Test
  public void testInvalidParticipantId ()
  {
    final CallToolResult aResult = _call ("not-a-valid-pid");
    assertNotNull (aResult);
    assertTrue ("Expected isError=true for malformed participant ID", aResult.isError ().booleanValue ());
  }

  @Test
  public void testNonExistentParticipantReturnsDnsNameButFailsToResolve ()
  {
    // A syntactically valid participant ID that is almost certainly not registered in the test SML
    final CallToolResult aResult = _call ("9999:does-not-exist-1234567890");
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sBody = _textOf (aResult);
    assertTrue ("Expected the queried DNS name to be present: " + sBody, sBody.contains ("\"queryDnsName\":"));
    assertTrue ("Expected the SML zone to be present: " + sBody, sBody.contains ("\"smlDnsZone\":"));
    assertTrue ("Expected resolved=false for unregistered participant: " + sBody,
                sBody.contains ("\"resolved\":false"));
  }
}
