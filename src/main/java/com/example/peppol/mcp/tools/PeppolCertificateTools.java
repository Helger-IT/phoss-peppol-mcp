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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tool for validating an X.509 certificate against the official Peppol trust stores. Supports
 * AP and SMP certificates for both production and pilot/test PKIs.
 */
public final class PeppolCertificateTools
{
  @NonNull
  private static TrustedCAChecker _resolveChecker (@NonNull final String sCertType, @NonNull final String sNetwork)
  {
    final String sType = sCertType.toUpperCase (Locale.US);
    final String sNet = sNetwork.toLowerCase (Locale.US);
    if ("AP".equals (sType))
    {
      if ("production".equals (sNet) || "prod".equals (sNet))
        return PeppolTrustedCA.peppolProductionAP ();
      if ("test".equals (sNet) || "pilot".equals (sNet))
        return PeppolTrustedCA.peppolTestAP ();
      if ("all".equals (sNet) || "any".equals (sNet))
        return PeppolTrustedCA.peppolAllAP ();
      throw new IllegalArgumentException ("Invalid network '" +
                                          sNetwork +
                                          "'. Expected 'production', 'test', or 'all'.");
    }
    if ("SMP".equals (sType))
    {
      if ("production".equals (sNet) || "prod".equals (sNet))
        return PeppolTrustedCA.peppolProductionSMP ();
      if ("test".equals (sNet) || "pilot".equals (sNet))
        return PeppolTrustedCA.peppolTestSMP ();
      if ("all".equals (sNet) || "any".equals (sNet))
        return PeppolTrustedCA.peppolAllSMP ();
      throw new IllegalArgumentException ("Invalid network '" +
                                          sNetwork +
                                          "'. Expected 'production', 'test', or 'all'.");
    }
    throw new IllegalArgumentException ("Invalid certificateType '" + sCertType + "'. Expected 'AP' or 'SMP'.");
  }

  @NonNull
  private IJsonObject _checkCertificateChain (@NonNull final String sCertPem,
                                              @NonNull final String sCertType,
                                              @NonNull final String sNetwork) throws Exception
  {
    final TrustedCAChecker aChecker = _resolveChecker (sCertType, sNetwork);
    final X509Certificate aCert = new CertificateDecodeHelper ().source (sCertPem)
                                                                .pemEncoded (true)
                                                                .getDecodedOrNull ();

    final JsonObject aResult = new JsonObject ();
    aResult.add ("certificateType", sCertType.toUpperCase (Locale.US));
    aResult.add ("network", sNetwork.toLowerCase (Locale.US));

    if (aCert == null)
    {
      aResult.add ("valid", false);
      aResult.add ("checkResult", "no-certificate");
      aResult.add ("reason",
                   "Failed to parse the provided certificate string. Provide a PEM-encoded X.509 certificate.");
      return aResult;
    }

    aResult.add ("subject", aCert.getSubjectX500Principal ().getName ());
    aResult.add ("issuer", aCert.getIssuerX500Principal ().getName ());
    aResult.add ("serialNumber", aCert.getSerialNumber ().toString ());
    aResult.add ("notBefore", aCert.getNotBefore ().toInstant ().toString ());
    aResult.add ("notAfter", aCert.getNotAfter ().toInstant ().toString ());

    final ECertificateCheckResult eRes = aChecker.checkCertificate (aCert);
    aResult.add ("valid", eRes.isValid ());
    aResult.add ("checkResult", eRes.getID ());
    aResult.add ("reason", eRes.getReason ());
    return aResult;
  }

  @NonNull
  public SyncToolSpecification checkCertificateChainTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_certificate_chain")
                                    .description ("""
                                        Validates an X.509 certificate against the official Peppol trust stores. \
                                        Checks issuer trust, validity period, and revocation status (CRL/OCSP). \
                                        Both Access Point (AP) and SMP certificates are supported, on both \
                                        Production and Pilot/Test PKIs (G3). Provide the certificate as a \
                                        PEM-encoded string (with or without the BEGIN/END markers). \
                                        Use certificateType='AP' for Access Point certificates and 'SMP' for SMP \
                                        signing certificates. Use network='production', 'test', or 'all' to choose \
                                        which Peppol root CAs to check against.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("certificate",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "PEM-encoded X.509 certificate (with or without BEGIN/END markers)"),
                                                                                    "certificateType",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol certificate role: 'AP' (Access Point) or 'SMP' (SMP signing)"),
                                                                                    "network",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol network: 'production', 'test' (alias 'pilot'), or 'all' to accept either")),
                                                                            List.of ("certificate",
                                                                                     "certificateType",
                                                                                     "network"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sCert = (String) request.arguments ().get ("certificate");
      final String sCertType = (String) request.arguments ().get ("certificateType");
      final String sNetwork = (String) request.arguments ().get ("network");
      return Helper.executeWithErrorHandling (() -> _checkCertificateChain (sCert, sCertType, sNetwork));
    });
  }
}
