package com.example.peppol.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.EPeppolCodeListItemState;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PredefinedDocumentTypeIdentifierManager;
import com.helger.peppolid.peppol.pidscheme.EPredefinedParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.IPeppolParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.PeppolParticipantIdentifierSchemeManager;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.IPeppolPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PredefinedProcessIdentifierManager;
import com.helger.peppolid.peppol.spisusecase.EPredefinedSPISUseCaseIdentifier;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tools for checking whether Peppol identifiers are present in the official Peppol codelists,
 * and for listing all codelist entries. These tools go beyond syntactic validation: they verify
 * that a given identifier is an officially registered value in the Peppol code lists.
 */
public final class PeppolCodelistTools
{
  @Nullable
  private static EPeppolCodeListItemState _parseStateFilter (@Nullable final String sState)
  {
    if (sState == null || sState.isBlank ())
      return null;

    final var eState = EPeppolCodeListItemState.getFromIDOrNull (sState);
    if (eState != null)
      return eState;

    // Also accept the enum name directly (ACTIVE, DEPRECATED, REMOVED)
    try
    {
      return EPeppolCodeListItemState.valueOf (sState.toUpperCase (Locale.US));
    }
    catch (final IllegalArgumentException ex)
    {
      throw new IllegalArgumentException ("Invalid state filter '" +
                                          sState +
                                          "'. Valid values: act, dep, rem (or ACTIVE, DEPRECATED, REMOVED)");
    }
  }

  // -------------------------------------------------------------------------
  // Tool 1: Check participant identifier scheme in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _checkParticipantIdSchemeInCodelist (@NonNull final String sInput)
  {
    // Try to parse as a full participant identifier
    final var aPID = Helper.parseParticipantId (sInput, true);
    final var aScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aPID);
    final String sValue = aPID.getValue ();
    final int nColon = sValue.indexOf (':');
    final var sISO6523Code = nColon > 0 ? sValue.substring (0, nColon) : sValue;

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("iso6523Code", sISO6523Code);
    aResult.put ("inCodelist", Boolean.valueOf (aScheme != null));
    aResult.put ("codeListVersion", EPredefinedParticipantIdentifierScheme.CODE_LIST_VERSION);

    if (aScheme != null)
    {
      aResult.put ("schemeID", aScheme.getSchemeID ());
      aResult.put ("schemeName", aScheme.getSchemeName ());
      aResult.put ("schemeAgency", aScheme.getSchemeAgency ());
      aResult.put ("countryCode", aScheme.getCountryCode ());
      aResult.put ("state", aScheme.getState ().getID ());
      if (aScheme.getRemovalDate () != null)
        aResult.put ("removalDate", aScheme.getRemovalDate ().toString ());
    }

    return aResult;
  }

  @NonNull
  public SyncToolSpecification checkParticipantIdSchemeInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_participant_id_scheme_in_codelist")
                                    .description ("""
                                        Checks whether the identifier scheme (ISO 6523 code) used in a Peppol \
                                        Participant identifier is present in the official Peppol Participant \
                                        identifier scheme codelist. For example, checks whether '0088' (GLN) or \
                                        '0192' (Norwegian org number) is a recognized Peppol scheme. \
                                        You can pass either a full Participant identifier like '0088:4012345678901' \
                                        or just the ISO 6523 scheme code like '0088'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("participantId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Full Participant identifier (e.g. '0088:4012345678901') or just the ISO 6523 scheme code (e.g. '0088')")),
                                                                            List.of ("participantId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPID = (String) request.arguments ().get ("participantId");
      return Helper.executeWithErrorHandling ( () -> _checkParticipantIdSchemeInCodelist (sPID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 2: Check document type identifier in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _checkDocumentTypeIdInCodelist (@NonNull final String sDTID)
  {
    final var aDTID = Helper.parseDocTypeID (sDTID, true);
    final IPeppolPredefinedDocumentTypeIdentifier aDocTypeID = PredefinedDocumentTypeIdentifierManager.getDocumentTypeIdentifierOfID (aDTID.getURIEncoded ());

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("documentTypeId", aDTID.getURIEncoded ());
    aResult.put ("inCodelist", Boolean.valueOf (aDocTypeID != null));
    aResult.put ("codeListVersion", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

    if (aDocTypeID != null)
    {
      aResult.put ("commonName", aDocTypeID.getCommonName ());
      aResult.put ("scheme", aDocTypeID.getScheme ());
      aResult.put ("state", aDocTypeID.getState ().getID ());
      if (aDocTypeID.getRemovalDate () != null)
        aResult.put ("removalDate", aDocTypeID.getRemovalDate ().toString ());
      aResult.put ("bisVersion", Integer.valueOf (aDocTypeID.getBISVersion ()));
      aResult.put ("domainCommunity", aDocTypeID.getDomainCommunity ());
      aResult.put ("issuedByOpenPeppol", Boolean.valueOf (aDocTypeID.isIssuedByOpenPeppol ()));

      final var aProcessIDs = aDocTypeID.getAllProcessIDs ();
      if (aProcessIDs != null && aProcessIDs.isNotEmpty ())
        aResult.put ("processIDs", aProcessIDs.getAllMapped (IProcessIdentifier::getURIEncoded));
    }

    return aResult;
  }

  @NonNull
  public SyncToolSpecification checkDocumentTypeIdInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_document_type_id_in_codelist")
                                    .description ("""
                                        Checks whether a Peppol Document Type identifier is present in the \
                                        official Peppol Document Type codelist. Returns detailed information \
                                        including common name, state (active/deprecated/removed), BIS version, \
                                        domain community, and associated Process IDs if found. \
                                        The value should be the Document Type identifier value, e.g. \
                                        'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##...'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("documentTypeId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol Document Type identifier value to look up in the codelist")),
                                                                            List.of ("documentTypeId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sDTID = (String) request.arguments ().get ("documentTypeId");
      return Helper.executeWithErrorHandling ( () -> _checkDocumentTypeIdInCodelist (sDTID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 3: Check process identifier in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _checkProcessIdInCodelist (@NonNull final String sPRID)
  {
    final var aPRID = Helper.parseProcessID (sPRID, true);
    final IPeppolPredefinedProcessIdentifier aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (aPRID.getURIEncoded ());

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("processId", aPRID.getURIEncoded ());
    aResult.put ("inCodelist", Boolean.valueOf (aProcID != null));
    aResult.put ("codeListVersion", EPredefinedProcessIdentifier.CODE_LIST_VERSION);

    if (aProcID != null)
    {
      aResult.put ("scheme", aProcID.getScheme ());
      aResult.put ("value", aProcID.getValue ());
      aResult.put ("state", aProcID.getState ().getID ());
    }

    return aResult;
  }

  @NonNull
  public SyncToolSpecification checkProcessIdInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_process_id_in_codelist")
                                    .description ("""
                                        Checks whether a Peppol Process identifier is present in the official \
                                        Peppol Process identifier codelist. Returns the state \
                                        (active/deprecated/removed) if found. \
                                        The value should be a Process identifier string, e.g. \
                                        'urn:fdc:peppol.eu:2017:poacc:billing:01:1.0'.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("processId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Peppol Process identifier value to look up in the codelist")),
                                                                            List.of ("processId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sPRID = (String) request.arguments ().get ("processId");
      return Helper.executeWithErrorHandling ( () -> _checkProcessIdInCodelist (sPRID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 4: Check SPIS Use Case identifier in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _checkSPISUseCaseIdInCodelist (@NonNull final String sUseCaseID)
  {
    EPredefinedSPISUseCaseIdentifier aFound = null;
    for (final var e : EPredefinedSPISUseCaseIdentifier.values ())
      if (e.getUseCaseID ().equals (sUseCaseID))
      {
        aFound = e;
        break;
      }

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("useCaseId", sUseCaseID);
    aResult.put ("inCodelist", Boolean.valueOf (aFound != null));
    aResult.put ("codeListVersion", EPredefinedSPISUseCaseIdentifier.CODE_LIST_VERSION);

    if (aFound != null)
    {
      aResult.put ("state", aFound.getState ().getID ());
      aResult.put ("initialRelease", aFound.getInitialRelease ().toString ());
      if (aFound.getDeprecationRelease () != null)
        aResult.put ("deprecationRelease", aFound.getDeprecationRelease ().toString ());
      if (aFound.getRemovalDate () != null)
        aResult.put ("removalDate", aFound.getRemovalDate ().toString ());
    }

    return aResult;
  }

  @NonNull
  public SyncToolSpecification checkSPISUseCaseIdInCodelistTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("check_spis_use_case_id_in_codelist")
                                    .description ("""
                                        Checks whether a SPIS (Service Provider Information Service) Use Case \
                                        identifier is present in the official Peppol SPIS Use Case codelist. \
                                        Returns the state (active/deprecated/removed) if found. \
                                        Example: 'MLS' for Message Level Status.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("useCaseId",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "SPIS Use Case identifier to look up, e.g. 'MLS'")),
                                                                            List.of ("useCaseId"),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sUseCaseID = (String) request.arguments ().get ("useCaseId");
      return Helper.executeWithErrorHandling ( () -> _checkSPISUseCaseIdInCodelist (sUseCaseID));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 5: Get codelist version
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _getCodelistVersion ()
  {
    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("participantIdentifierSchemeCodelistVersion",
                 EPredefinedParticipantIdentifierScheme.CODE_LIST_VERSION);
    aResult.put ("documentTypeCodelistVersion", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);
    aResult.put ("processCodelistVersion", EPredefinedProcessIdentifier.CODE_LIST_VERSION);
    aResult.put ("spisUseCaseCodelistVersion", EPredefinedSPISUseCaseIdentifier.CODE_LIST_VERSION);
    return aResult;
  }

  @NonNull
  public SyncToolSpecification getCodelistVersionTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("get_peppol_codelist_version")
                                    .description ("""
                                        Returns the version of the Peppol codelists currently in use. \
                                        This includes the version of the Participant identifier scheme, \
                                        Document Type, Process identifier, and SPIS Use Case codelists. \
                                        No input parameters required.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of (),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool,
                                      (exchange, request) -> Helper.executeWithErrorHandling (
                                                                                              this::_getCodelistVersion));
  }

  // -------------------------------------------------------------------------
  // Tool 6: List all participant identifier schemes
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _listParticipantIdSchemes (@Nullable final String sState,
                                                          @Nullable final String sCountryCode)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aEntries = new ArrayList <Map <String, Object>> ();

    for (final IPeppolParticipantIdentifierScheme aScheme : PeppolParticipantIdentifierSchemeManager.getAllSchemes ())
    {
      if (eStateFilter != null && aScheme.getState () != eStateFilter)
        continue;
      if (StringHelper.isNotEmpty (sCountryCode) && !sCountryCode.equalsIgnoreCase (aScheme.getCountryCode ()))
        continue;

      final Map <String, Object> aEntry = new LinkedHashMap <> ();
      aEntry.put ("iso6523Code", aScheme.getISO6523Code ());
      aEntry.put ("schemeID", aScheme.getSchemeID ());
      aEntry.put ("schemeName", aScheme.getSchemeName ());
      aEntry.put ("schemeAgency", aScheme.getSchemeAgency ());
      aEntry.put ("countryCode", aScheme.getCountryCode ());
      aEntry.put ("state", aScheme.getState ().getID ());
      if (aScheme.getRemovalDate () != null)
        aEntry.put ("removalDate", aScheme.getRemovalDate ().toString ());
      aEntries.add (aEntry);
    }

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("codeListVersion", EPredefinedParticipantIdentifierScheme.CODE_LIST_VERSION);
    aResult.put ("totalEntries", Integer.valueOf (aEntries.size ()));
    aResult.put ("entries", aEntries);
    return aResult;
  }

  @NonNull
  public SyncToolSpecification listParticipantIdSchemesTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_participant_id_schemes")
                                    .description ("""
                                        Lists all Participant identifier schemes (ISO 6523 codes) from the \
                                        official Peppol codelist. Can be filtered by state and/or country code. \
                                        Use this to discover which identifier schemes are available for a \
                                        specific country, or to see all active/deprecated/removed schemes.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all entries are returned."),
                                                                                    "countryCode",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional ISO 3166-1 alpha-2 country code to filter by, e.g. 'DE', 'AT', 'NO'")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sState = (String) aArgs.get ("state");
      final String sCountryCode = (String) aArgs.get ("countryCode");
      return Helper.executeWithErrorHandling ( () -> _listParticipantIdSchemes (sState, sCountryCode));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 7: List all document type identifiers
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _listDocumentTypeIds (@Nullable final String sState)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aEntries = new ArrayList <Map <String, Object>> ();

    for (final IPeppolPredefinedDocumentTypeIdentifier aDT : PredefinedDocumentTypeIdentifierManager.getAllDocumentTypeIdentifiers ())
    {
      if (eStateFilter != null && aDT.getState () != eStateFilter)
        continue;

      final Map <String, Object> aEntry = new LinkedHashMap <> ();
      aEntry.put ("documentTypeId", aDT.getScheme () + "::" + aDT.getValue ());
      aEntry.put ("commonName", aDT.getCommonName ());
      aEntry.put ("state", aDT.getState ().getID ());
      aEntry.put ("bisVersion", Integer.valueOf (aDT.getBISVersion ()));
      aEntry.put ("domainCommunity", aDT.getDomainCommunity ());
      if (aDT.getRemovalDate () != null)
        aEntry.put ("removalDate", aDT.getRemovalDate ().toString ());
      aEntries.add (aEntry);
    }

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("codeListVersion", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);
    aResult.put ("totalEntries", Integer.valueOf (aEntries.size ()));
    aResult.put ("entries", aEntries);
    return aResult;
  }

  @NonNull
  public SyncToolSpecification listDocumentTypeIdsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_document_type_ids")
                                    .description ("""
                                        Lists all Document Type identifiers from the official Peppol codelist. \
                                        Can be filtered by state. Use this to discover which document types \
                                        (invoices, credit notes, orders, etc.) are defined in the Peppol network. \
                                        Returns common name, BIS version, domain community, and state for each entry.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all entries are returned.")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sState = (String) request.arguments ().get ("state");
      return Helper.executeWithErrorHandling ( () -> _listDocumentTypeIds (sState));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 8: List all process identifiers
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _listProcessIds (@Nullable final String sState)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aEntries = new ArrayList <Map <String, Object>> ();

    for (final IPeppolPredefinedProcessIdentifier aProc : PredefinedProcessIdentifierManager.getAllProcessIdentifiers ())
    {
      if (eStateFilter != null && aProc.getState () != eStateFilter)
        continue;

      final Map <String, Object> aEntry = new LinkedHashMap <> ();
      aEntry.put ("processId", aProc.getScheme () + "::" + aProc.getValue ());
      aEntry.put ("scheme", aProc.getScheme ());
      aEntry.put ("value", aProc.getValue ());
      aEntry.put ("state", aProc.getState ().getID ());
      aEntries.add (aEntry);
    }

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("codeListVersion", EPredefinedProcessIdentifier.CODE_LIST_VERSION);
    aResult.put ("totalEntries", Integer.valueOf (aEntries.size ()));
    aResult.put ("entries", aEntries);
    return aResult;
  }

  @NonNull
  public SyncToolSpecification listProcessIdsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_process_ids")
                                    .description ("""
                                        Lists all Process identifiers from the official Peppol codelist. \
                                        Can be filtered by state. Use this to discover which business \
                                        processes (billing, ordering, despatch advice, etc.) are defined \
                                        in the Peppol network.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all entries are returned.")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sState = (String) request.arguments ().get ("state");
      return Helper.executeWithErrorHandling ( () -> _listProcessIds (sState));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 9: List all SPIS Use Case identifiers
  // -------------------------------------------------------------------------

  @NonNull
  private Map <String, Object> _listSPISUseCaseIds (@Nullable final String sState)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aEntries = new ArrayList <Map <String, Object>> ();

    for (final var aUseCase : EPredefinedSPISUseCaseIdentifier.values ())
    {
      if (eStateFilter != null && aUseCase.getState () != eStateFilter)
        continue;

      final Map <String, Object> aEntry = new LinkedHashMap <> ();
      aEntry.put ("useCaseId", aUseCase.getUseCaseID ());
      aEntry.put ("state", aUseCase.getState ().getID ());
      aEntry.put ("initialRelease", aUseCase.getInitialRelease ().toString ());
      if (aUseCase.getDeprecationRelease () != null)
        aEntry.put ("deprecationRelease", aUseCase.getDeprecationRelease ().toString ());
      if (aUseCase.getRemovalDate () != null)
        aEntry.put ("removalDate", aUseCase.getRemovalDate ().toString ());
      aEntries.add (aEntry);
    }

    final Map <String, Object> aResult = new LinkedHashMap <> ();
    aResult.put ("codeListVersion", EPredefinedSPISUseCaseIdentifier.CODE_LIST_VERSION);
    aResult.put ("totalEntries", Integer.valueOf (aEntries.size ()));
    aResult.put ("entries", aEntries);
    return aResult;
  }

  @NonNull
  public SyncToolSpecification listSPISUseCaseIdsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_spis_use_case_ids")
                                    .description ("""
                                        Lists all SPIS (Service Provider Information Service) Use Case \
                                        identifiers from the official Peppol codelist. Can be filtered by state. \
                                        Use this to discover which SPIS use cases (e.g. MLS - Mandatory Log \
                                        Service) are defined in the Peppol network.""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all entries are returned.")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final String sState = (String) request.arguments ().get ("state");
      return Helper.executeWithErrorHandling ( () -> _listSPISUseCaseIds (sState));
    });
  }
}
