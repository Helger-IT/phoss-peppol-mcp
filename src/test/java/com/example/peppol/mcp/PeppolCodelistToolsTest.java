package com.example.peppol.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import com.example.peppol.mcp.tools.PeppolCodelistTools;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Unit tests for {@link PeppolCodelistTools}. All tests are local (no network needed).
 */
class PeppolCodelistToolsTest
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

  // -----------------------------------------------------------------------
  // Codelist version
  // -----------------------------------------------------------------------

  @Test
  void testGetCodelistVersion ()
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
  void testParticipantSchemeKnownGLN ()
  {
    final var aResult = _callParticipantScheme ("0088:4012345678901");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"iso6523Code\" : \"0088\""));
    assertTrue (sContent.contains ("\"schemeID\" : \"GLN\""));
  }

  @Test
  void testParticipantSchemeKnownNorway ()
  {
    final var aResult = _callParticipantScheme ("0192:991825827");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"iso6523Code\" : \"0192\""));
    assertTrue (sContent.contains ("\"countryCode\" : \"NO\""));
  }

  @Test
  void testParticipantSchemeKnownNorwayFull ()
  {
    final var aResult = _callParticipantScheme ("iso6523-actorid-upis::0192:991825827");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"iso6523Code\" : \"0192\""));
    assertTrue (sContent.contains ("\"countryCode\" : \"NO\""));
  }

  @Test
  void testParticipantSchemeUnknown ()
  {
    final var aResult = _callParticipantScheme ("9999:does-not-matter");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : false"));
  }

  @Test
  void testParticipantSchemeSyntaxError ()
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
  void testDocumentTypeKnownBIS3Invoice ()
  {
    // URI-encoded key: scheme::value
    final var aResult = _callDocumentType ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"commonName\""));
    assertTrue (sContent.contains ("\"domainCommunity\""));
    assertTrue (sContent.contains ("\"processIDs\""));
  }

  @Test
  void testDocumentTypeKnownBIS3InvoiceFull ()
  {
    // URI-encoded key: scheme::value
    final var aResult = _callDocumentType ("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"commonName\""));
    assertTrue (sContent.contains ("\"domainCommunity\""));
    assertTrue (sContent.contains ("\"processIDs\""));
  }

  @Test
  void testDocumentTypeUnknown ()
  {
    final var aResult = _callDocumentType ("busdox-docid-qns::urn:not:a:real##document:type::1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : false"));
  }

  @Test
  void testDocumentTypeSyntaxError ()
  {
    final var aResult = _callDocumentType ("busdox-docid-qns::urn:not:a:real:document:type::1.0");

    assertTrue (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("Invalid Peppol Document Type ID format"), sContent);
  }

  // -----------------------------------------------------------------------
  // Process identifier codelist
  // -----------------------------------------------------------------------

  @Test
  void testProcessIdKnownBIS3Billing ()
  {
    final var aResult = _callProcessId ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"state\" : \"act\""));
    assertTrue (sContent.contains ("\"scheme\" : \"cenbii-procid-ubl\""));
  }

  @Test
  void testProcessIdKnownBIS3BillingFull ()
  {
    final var aResult = _callProcessId ("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"state\" : \"act\""));
    assertTrue (sContent.contains ("\"scheme\" : \"cenbii-procid-ubl\""));
  }

  @Test
  void testProcessIdUnknown ()
  {
    final var aResult = _callProcessId ("urn:not:a:real:process:1.0");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : false"));
  }

  // -----------------------------------------------------------------------
  // SPIS Use Case identifier codelist
  // -----------------------------------------------------------------------

  @Test
  void testSPISUseCaseKnownMLS ()
  {
    final var aResult = _callSPISUseCase ("MLS");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : true"));
    assertTrue (sContent.contains ("\"state\" : \"act\""));
    assertTrue (sContent.contains ("\"initialRelease\""));
  }

  @Test
  void testSPISUseCaseUnknown ()
  {
    final var aResult = _callSPISUseCase ("NONEXISTENT");

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"inCodelist\" : false"));
  }

  // -----------------------------------------------------------------------
  // List participant identifier schemes
  // -----------------------------------------------------------------------

  @Test
  void testListParticipantSchemesAll ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertTrue (sContent.contains ("\"entries\""));
    assertTrue (sContent.contains ("\"iso6523Code\""));
  }

  @Test
  void testListParticipantSchemesFilterByState ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    // All returned entries should be active
    assertFalse (sContent.contains ("\"state\" : \"dep\""));
    assertFalse (sContent.contains ("\"state\" : \"rem\""));
  }

  @Test
  void testListParticipantSchemesFilterByCountry ()
  {
    final var aResult = m_aTools.listParticipantIdSchemesTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_participant_id_schemes",
                                                                       Map.of ("countryCode", "NO")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertTrue (sContent.contains ("\"countryCode\" : \"NO\""));
  }

  // -----------------------------------------------------------------------
  // List document type identifiers
  // -----------------------------------------------------------------------

  @Test
  void testListDocumentTypeIdsAll ()
  {
    final var aResult = m_aTools.listDocumentTypeIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_document_type_ids", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertTrue (sContent.contains ("\"entries\""));
    assertTrue (sContent.contains ("\"commonName\""));
  }

  @Test
  void testListDocumentTypeIdsFilterByState ()
  {
    final var aResult = m_aTools.listDocumentTypeIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_document_type_ids",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertFalse (sContent.contains ("\"state\" : \"dep\""));
    assertFalse (sContent.contains ("\"state\" : \"rem\""));
  }

  // -----------------------------------------------------------------------
  // List process identifiers
  // -----------------------------------------------------------------------

  @Test
  void testListProcessIdsAll ()
  {
    final var aResult = m_aTools.listProcessIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_process_ids", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertTrue (sContent.contains ("\"entries\""));
    assertTrue (sContent.contains ("\"scheme\""));
  }

  @Test
  void testListProcessIdsFilterByState ()
  {
    final var aResult = m_aTools.listProcessIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_process_ids",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertFalse (sContent.contains ("\"state\" : \"dep\""));
    assertFalse (sContent.contains ("\"state\" : \"rem\""));
  }

  // -----------------------------------------------------------------------
  // List SPIS Use Case identifiers
  // -----------------------------------------------------------------------

  @Test
  void testListSPISUseCaseIdsAll ()
  {
    final var aResult = m_aTools.listSPISUseCaseIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_spis_use_case_ids", Map.of ()));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertTrue (sContent.contains ("\"entries\""));
    assertTrue (sContent.contains ("\"useCaseId\" : \"MLS\""));
  }

  @Test
  void testListSPISUseCaseIdsFilterByState ()
  {
    final var aResult = m_aTools.listSPISUseCaseIdsTool ()
                                .callHandler ()
                                .apply (null,
                                        new McpSchema.CallToolRequest ("list_spis_use_case_ids",
                                                                       Map.of ("state", "act")));

    assertFalse (aResult.isError ().booleanValue ());
    final String sContent = _text (aResult);
    assertTrue (sContent.contains ("\"totalEntries\""));
    assertTrue (sContent.contains ("\"useCaseId\" : \"MLS\""));
  }

  // -----------------------------------------------------------------------
  // Invalid state filter
  // -----------------------------------------------------------------------

  @Test
  void testInvalidStateFilter ()
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
