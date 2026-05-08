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

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.example.peppol.mcp.tools.PeppolCertificateTools;
import com.helger.peppol.security.PeppolTrustStores;
import com.helger.security.certificate.CertificateHelper;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Negative-path unit tests for {@link PeppolCertificateTools}. No live network needed.
 */
public final class PeppolCertificateToolsTest
{
  private final PeppolCertificateTools m_aTools = new PeppolCertificateTools ();

  @NonNull
  private CallToolResult _call (final String sCert, final String sCertType, final String sNetwork)
  {
    final Map <String, Object> aArgs = new HashMap <> ();
    if (sCert != null)
      aArgs.put ("certificate", sCert);
    if (sCertType != null)
      aArgs.put ("certificateType", sCertType);
    if (sNetwork != null)
      aArgs.put ("network", sNetwork);
    return m_aTools.checkCertificateChainTool ()
                   .callHandler ()
                   .apply (null, new McpSchema.CallToolRequest ("check_certificate_chain", aArgs));
  }

  @NonNull
  private static String _textOf (@NonNull final CallToolResult aResult)
  {
    return ((McpSchema.TextContent) aResult.content ().get (0)).text ();
  }

  @Test
  public void testEmptyCertificate ()
  {
    final CallToolResult aResult = _call ("", "AP", "production");
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sBody = _textOf (aResult);
    assertTrue ("Expected valid=false for empty cert: " + sBody, sBody.contains ("\"valid\":false"));
    assertTrue ("Expected checkResult=no-certificate: " + sBody, sBody.contains ("\"checkResult\":\"no-certificate\""));
  }

  @Test
  public void testGarbageCertificate ()
  {
    final CallToolResult aResult = _call ("this is not a certificate", "AP", "test");
    assertNotNull (aResult);
    // CertificateDecodeHelper returns null for garbage, so the tool reports valid=false
    final String sBody = _textOf (aResult);
    assertTrue ("Expected valid=false for garbage: " + sBody, sBody.contains ("\"valid\":false"));
  }

  @Test
  public void testInvalidCertificateType ()
  {
    final CallToolResult aResult = _call ("---dummy---", "BOGUS", "production");
    assertNotNull (aResult);
    assertTrue ("Expected isError=true for invalid certificateType", aResult.isError ().booleanValue ());
    assertTrue ("Error message should mention certificateType", _textOf (aResult).contains ("certificateType"));
  }

  @Test
  public void testInvalidNetwork ()
  {
    final CallToolResult aResult = _call ("---dummy---", "AP", "staging");
    assertNotNull (aResult);
    assertTrue ("Expected isError=true for invalid network", aResult.isError ().booleanValue ());
    assertTrue ("Error message should mention network", _textOf (aResult).contains ("network"));
  }

  @Test
  public void testTestRootAgainstProductionAP () throws Exception
  {
    // The Peppol G3 Test root CA is a valid X.509 cert, but it is self-signed (issuer == subject == TEST root)
    // — so checking it against the PRODUCTION AP CA should yield "unsupportedissuer".
    final String sPem = CertificateHelper.getPEMEncodedCertificate (PeppolTrustStores.Config2025.CERTIFICATE_TEST_ROOT);
    final CallToolResult aResult = _call (sPem, "AP", "production");
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sBody = _textOf (aResult);
    assertTrue ("Expected valid=false: " + sBody, sBody.contains ("\"valid\":false"));
    assertTrue ("Expected checkResult=unsupportedissuer: " + sBody,
                sBody.contains ("\"checkResult\":\"unsupportedissuer\""));
  }

  @Test
  public void testProdRootAgainstTestSMP () throws Exception
  {
    // Same idea, swapped: PRODUCTION root self-signed, checked against TEST SMP CA → unsupportedissuer
    final String sPem = CertificateHelper.getPEMEncodedCertificate (PeppolTrustStores.Config2025.CERTIFICATE_PRODUCTION_ROOT);
    final CallToolResult aResult = _call (sPem, "SMP", "test");
    assertNotNull (aResult);
    assertFalse (aResult.isError ().booleanValue ());
    final String sBody = _textOf (aResult);
    assertTrue ("Expected valid=false: " + sBody, sBody.contains ("\"valid\":false"));
    assertTrue ("Expected checkResult=unsupportedissuer: " + sBody,
                sBody.contains ("\"checkResult\":\"unsupportedissuer\""));
  }
}
