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

import com.example.peppol.mcp.tools.PeppolCodelistTools;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Unit tests for {@link PeppolCodelistTools}. All tests are local (no network needed).
 */
public final class PeppolCodelistToolsTest
{
  private final PeppolCodelistTools m_aTools = new PeppolCodelistTools ();

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  @NonNull
  private CallToolResult _callParticipantScheme (@NonNull final String sInput)
  {
    return m_aTools.checkParticipantIdSchemeInCodelistTool ()
                   .callHandler ()
                   .apply (null,
                           new McpSchema.CallToolRequest ("check_participant_id_scheme_in_codelist",
                                                          Map.of ("participantId", sInput)));
  }

  @NonNull
  private CallToolResult _callDocumentType (@NonNull final String sInput)
  {
    return m_aTools.checkDocumentTypeIdInCodelistTool ()
                   .callHandler ()
                   .apply (null,
                           new McpSchema.CallToolRequest ("check_document_type_id_in_codelist",
                                                          Map.of ("documentTypeId", sInput)));
  }

  @NonNull
  private CallToolResult _callProcessId (@NonNull final String sInput)
  {
    return m_aTools.checkProcessIdInCodelistTool ()
                   .callHandler ()
                   .apply (null,
                           new McpSchema.CallToolRequest ("check_process_id_in_codelist",
                                                          Map.of ("processId", sInput)));
  }

  @NonNull
  private CallToolResult _callSPISUseCase (@NonNull final String sInput)
  {
    return m_aTools.checkSPISUseCaseIdInCodelistTool ()
                   .callHandler ()
                   .apply (null,
                           new McpSchema.CallToolRequest ("check_spis_use_case_id_in_codelist",
                                                          Map.of ("useCaseId", sInput)));
  }

  @NonNull
  private static String _text (@NonNull final CallToolResult aResult)
  {
    assertNotNull (aResult.content ());
    assertFalse (aResult.content ().isEmpty ());
    return ((McpSchema.TextContent) aResult.content ().get (0)).text ();
  }

  /**
   * Build a mutable args map — needed because Map.of () does not allow null values.
   */
  @NonNull
  private static Map <String, Object> _args (@NonNull final Object... aPairs)
  {
    final var aMap = new HashMap <String, Object> ();
    for (int i = 0; i < aPairs.length; i += 2)
      aMap.put ((String) aPairs[i], aPairs[i + 1]);
    return aMap;
  }

  // -----------------------------------------------------------------------
  // Codelist version
  // -----------------------------------------------------------------------

  @Test
  public void testGetCodelistVersion ()
  {
    final var aResult = m_aTools.getCodelistVersionTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("get_peppol_codelist_version", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"participantIdentifierSchemeCodelistVersion\""));
    assertTrue (sContent.contains ("\"documentTypeCodelistVersion\""));
    assertTrue (sContent.contains ("\"processCodelistVersion\""));
    assertTrue (sContent.contains ("\"spisUseCaseCodelistVersion\""));
  }

  // -----------------------------------------------------------------------
  // Participant identifier scheme codelist
  // -----------------------------------------------------------------------

  @Test
  public void testParticipantSchemeKnownGLN ()
  {
    final var aResult = _callParticipantScheme ("0088:4012345678901");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"iso6523Code\":\"0088\""));
    assertTrue (sContent.contains ("\"schemeID\":\"GLN\""));
  }

  @Test
  public void testParticipantSchemeKnownNorway ()
  {
    final var aResult = _callParticipantScheme ("0192:991825827");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"iso6523Code\":\"0192\""));
    assertTrue (sContent.contains ("\"countryCode\":\"NO\""));
  }

  @Test
  public void testParticipantSchemeKnownNorwayFull ()
  {
    final var aResult = _callParticipantScheme ("iso6523-actorid-upis::0192:991825827");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"iso6523Code\":\"0192\""));
    assertTrue (sContent.contains ("\"countryCode\":\"NO\""));
  }

  @Test
  public void testParticipantSchemeUnknown ()
  {
    final var aResult = _callParticipantScheme ("9999:does-not-matter");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":false"));
  }

  @Test
  public void testParticipantSchemeSyntaxError ()
  {
    final var aResult = _callParticipantScheme ("somethingCrappy");

    assertTrue (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("Invalid Peppol Participant ID format"));
  }

  // -----------------------------------------------------------------------
  // Document type identifier codelist
  // -----------------------------------------------------------------------

  @Test
  public void testDocumentTypeKnownBIS3Invoice ()
  {
    final var aResult = _callDocumentType ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"commonName\""));
    assertTrue (sContent.contains ("\"domainCommunity\""));
    assertTrue (sContent.contains ("\"processIDs\""));
  }

  @Test
  public void testDocumentTypeKnownBIS3InvoiceFull ()
  {
    final var aResult = _callDocumentType ("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"commonName\""));
    assertTrue (sContent.contains ("\"domainCommunity\""));
    assertTrue (sContent.contains ("\"processIDs\""));
  }

  @Test
  public void testDocumentTypeUnknown ()
  {
    final var aResult = _callDocumentType ("busdox-docid-qns::urn:not:a:real##document:type::1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":false"));
  }

  @Test
  public void testDocumentTypeSyntaxError ()
  {
    final var aResult = _callDocumentType ("busdox-docid-qns::urn:not:a:real:document:type::1.0");

    assertTrue (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent, sContent.contains ("Invalid Peppol Document Type ID format"));
  }

  // -----------------------------------------------------------------------
  // Process identifier codelist
  // -----------------------------------------------------------------------

  @Test
  public void testProcessIdKnownBIS3Billing ()
  {
    final var aResult = _callProcessId ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"state\":\"act\""));
    assertTrue (sContent.contains ("\"scheme\":\"cenbii-procid-ubl\""));
  }

  @Test
  public void testProcessIdKnownBIS3BillingFull ()
  {
    final var aResult = _callProcessId ("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"state\":\"act\""));
    assertTrue (sContent.contains ("\"scheme\":\"cenbii-procid-ubl\""));
  }

  @Test
  public void testProcessIdUnknown ()
  {
    final var aResult = _callProcessId ("urn:not:a:real:process:1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":false"));
  }

  // -----------------------------------------------------------------------
  // SPIS Use Case identifier codelist
  // -----------------------------------------------------------------------

  @Test
  public void testSPISUseCaseKnownMLS ()
  {
    final var aResult = _callSPISUseCase ("MLS");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":true"));
    assertTrue (sContent.contains ("\"state\":\"act\""));
    assertTrue (sContent.contains ("\"initialRelease\""));
  }

  @Test
  public void testSPISUseCaseUnknown ()
  {
    final var aResult = _callSPISUseCase ("NONEXISTENT");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\":false"));
  }

  // -----------------------------------------------------------------------
  // List participant identifier schemes
  // -----------------------------------------------------------------------

  @Test
  public void testListParticipantSchemesDefaultLimit ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"returnedEntries\""));
    assertTrue (sContent.contains ("\"limit\":" + 50));
    assertTrue (sContent.contains ("\"entries\""));
  }

  @Test
  public void testListParticipantSchemesFilterByState ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertFalse (sContent.contains ("\"state\":\"dep\""));
    assertFalse (sContent.contains ("\"state\":\"rem\""));
  }

  @Test
  public void testListParticipantSchemesFilterByCountry ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       Map.of ("countryCode", "NO")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"countryCode\":\"NO\""));
  }

  @Test
  public void testListParticipantSchemesQuery ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       Map.of ("query", "GLN")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"schemeID\":\"GLN\""));
  }

  @Test
  public void testListParticipantSchemesOffsetAndLimit ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       _args ("offset",
                                                                              Integer.valueOf (2),
                                                                              "limit",
                                                                              Integer.valueOf (3))));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"offset\":2"));
    assertTrue (sContent.contains ("\"limit\":3"));
    assertTrue (sContent.contains ("\"returnedEntries\":3"));
  }

  // -----------------------------------------------------------------------
  // List document type identifiers
  // -----------------------------------------------------------------------

  @Test
  public void testListDocumentTypeIdsDefaultLimit ()
  {
    final var aResult = m_aTools.listDocumentTypeIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_document_type_ids", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"limit\":" + 50));
  }

  @Test
  public void testListDocumentTypeIdsFilterByState ()
  {
    final var aResult = m_aTools.listDocumentTypeIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_document_type_ids",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertFalse (sContent.contains ("\"state\":\"dep\""));
    assertFalse (sContent.contains ("\"state\":\"rem\""));
  }

  @Test
  public void testListDocumentTypeIdsQuery ()
  {
    final var aResult = m_aTools.listDocumentTypeIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_document_type_ids",
                                                                       Map.of ("query", "UBL.BE")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("UBL.BE"));
  }

  @Test
  public void testListDocumentTypeIdsDomainCommunity ()
  {
    final var aResult = m_aTools.listDocumentTypeIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_document_type_ids",
                                                                       Map.of ("domainCommunity", "PRAC")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"domainCommunity\":\"PRAC\""));
    assertFalse (sContent.contains ("\"domainCommunity\":\"POAC\""));
  }

  // -----------------------------------------------------------------------
  // List process identifiers
  // -----------------------------------------------------------------------

  @Test
  public void testListProcessIdsDefaultLimit ()
  {
    final var aResult = m_aTools.listProcessIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_process_ids", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"limit\":" + 50));
  }

  @Test
  public void testListProcessIdsFilterByState ()
  {
    final var aResult = m_aTools.listProcessIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_process_ids",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertFalse (sContent.contains ("\"state\":\"dep\""));
    assertFalse (sContent.contains ("\"state\":\"rem\""));
  }

  @Test
  public void testListProcessIdsQuery ()
  {
    final var aResult = m_aTools.listProcessIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_process_ids",
                                                                       Map.of ("query", "billing")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("billing"));
  }

  // -----------------------------------------------------------------------
  // List SPIS Use Case identifiers
  // -----------------------------------------------------------------------

  @Test
  public void testListSPISUseCaseIdsAll ()
  {
    final var aResult = m_aTools.listSPISUseCaseIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_spis_use_case_ids", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"entries\""));
    assertTrue (sContent.contains ("\"useCaseId\":\"MLS\""));
  }

  @Test
  public void testListSPISUseCaseIdsFilterByState ()
  {
    final var aResult = m_aTools.listSPISUseCaseIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_spis_use_case_ids",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalMatchingEntries\""));
    assertTrue (sContent.contains ("\"useCaseId\":\"MLS\""));
  }

  // -----------------------------------------------------------------------
  // Invalid state filter
  // -----------------------------------------------------------------------

  @Test
  public void testInvalidStateFilter ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       Map.of ("state", "invalid")));

    assertTrue (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("Invalid state filter"));
  }
}
