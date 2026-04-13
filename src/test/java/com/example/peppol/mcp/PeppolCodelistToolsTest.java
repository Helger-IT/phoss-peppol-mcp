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
}
