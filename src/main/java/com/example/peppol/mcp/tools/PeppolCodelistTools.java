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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
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
  /** Default maximum number of entries returned by listing tools. */
  static final int DEFAULT_LIMIT = 50;

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

  private static boolean _matchesQuery (@Nullable final String sQuery, @NonNull final String... aFields)
  {
    if (sQuery == null || sQuery.isBlank ())
      return true;
    final String sLower = sQuery.toLowerCase (Locale.US);
    for (final String sField : aFields)
      if (sField != null && sField.toLowerCase (Locale.US).contains (sLower))
        return true;
    return false;
  }

  private static int _parseLimit (@Nullable final Object aLimit)
  {
    if (aLimit == null)
      return DEFAULT_LIMIT;
    return ((Number) aLimit).intValue ();
  }

  private static int _parseOffset (@Nullable final Object aOffset)
  {
    if (aOffset == null)
      return 0;
    return ((Number) aOffset).intValue ();
  }

  @NonNull
  private static IJsonObject _buildListResult (@NonNull final String sCodeListVersion,
                                               @NonNull final JsonArray aAllMatching,
                                               final int nOffset,
                                               final int nLimit)
  {
    final int nTotal = aAllMatching.size ();
    final int nEffectiveOffset = Math.min (nOffset, nTotal);
    final int nEnd = Math.min (nEffectiveOffset + nLimit, nTotal);
    final var aPage = aAllMatching.getSubArray (nEffectiveOffset, nEnd);

    return new JsonObject ().add ("codeListVersion", sCodeListVersion)
                            .add ("totalMatchingEntries", nTotal)
                            .add ("offset", nEffectiveOffset)
                            .add ("limit", nLimit)
                            .add ("returnedEntries", aPage.size ())
                            .add ("entries", aPage);
  }

  // -------------------------------------------------------------------------
  // Tool 1: Check participant identifier scheme in codelist
  // -------------------------------------------------------------------------

  @NonNull
  private IJsonObject _checkParticipantIdSchemeInCodelist (@NonNull final String sInput)
  {
    // Try to parse as a full participant identifier
    final var aPID = Helper.parseParticipantId (sInput, true);
    final var aScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aPID);
    final String sValue = aPID.getValue ();
    final int nColon = sValue.indexOf (':');
    final var sISO6523Code = nColon > 0 ? sValue.substring (0, nColon) : sValue;

    final JsonObject aResult = new JsonObject ();
    aResult.add ("iso6523Code", sISO6523Code);
    aResult.add ("inCodelist", aScheme != null);
    aResult.add ("codeListVersion", EPredefinedParticipantIdentifierScheme.CODE_LIST_VERSION);

    if (aScheme != null)
    {
      aResult.add ("schemeID", aScheme.getSchemeID ());
      aResult.add ("schemeName", aScheme.getSchemeName ());
      aResult.add ("schemeAgency", aScheme.getSchemeAgency ());
      aResult.add ("countryCode", aScheme.getCountryCode ());
      aResult.add ("state", aScheme.getState ().getID ());
      if (aScheme.getRemovalDate () != null)
        aResult.add ("removalDate", aScheme.getRemovalDate ().toString ());
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
  private IJsonObject _checkDocumentTypeIdInCodelist (@NonNull final String sDTID)
  {
    final var aDTID = Helper.parseDocTypeID (sDTID, true);
    final IPeppolPredefinedDocumentTypeIdentifier aDocTypeID = PredefinedDocumentTypeIdentifierManager.getDocumentTypeIdentifierOfID (aDTID.getURIEncoded ());

    final JsonObject aResult = new JsonObject ();
    aResult.add ("documentTypeId", aDTID.getURIEncoded ());
    aResult.add ("inCodelist", aDocTypeID != null);
    aResult.add ("codeListVersion", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

    if (aDocTypeID != null)
    {
      aResult.add ("commonName", aDocTypeID.getCommonName ());
      aResult.add ("scheme", aDocTypeID.getScheme ());
      aResult.add ("state", aDocTypeID.getState ().getID ());
      if (aDocTypeID.getRemovalDate () != null)
        aResult.add ("removalDate", aDocTypeID.getRemovalDate ().toString ());
      aResult.add ("bisVersion", aDocTypeID.getBISVersion ());
      aResult.add ("domainCommunity", aDocTypeID.getDomainCommunity ());
      aResult.add ("issuedByOpenPeppol", aDocTypeID.isIssuedByOpenPeppol ());

      final var aProcessIDs = aDocTypeID.getAllProcessIDs ();
      if (aProcessIDs != null && aProcessIDs.isNotEmpty ())
      {
        final JsonArray aProcArray = new JsonArray ();
        aProcessIDs.forEach (aProcID -> aProcArray.add (aProcID.getURIEncoded ()));
        aResult.add ("processIDs", aProcArray);
      }
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
  private IJsonObject _checkProcessIdInCodelist (@NonNull final String sPRID)
  {
    final var aPRID = Helper.parseProcessID (sPRID, true);
    final IPeppolPredefinedProcessIdentifier aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (aPRID.getURIEncoded ());

    final JsonObject aResult = new JsonObject ();
    aResult.add ("processId", aPRID.getURIEncoded ());
    aResult.add ("inCodelist", aProcID != null);
    aResult.add ("codeListVersion", EPredefinedProcessIdentifier.CODE_LIST_VERSION);

    if (aProcID != null)
    {
      aResult.add ("scheme", aProcID.getScheme ());
      aResult.add ("value", aProcID.getValue ());
      aResult.add ("state", aProcID.getState ().getID ());
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
  private IJsonObject _checkSPISUseCaseIdInCodelist (@NonNull final String sUseCaseID)
  {
    EPredefinedSPISUseCaseIdentifier aFound = null;
    for (final var e : EPredefinedSPISUseCaseIdentifier.values ())
      if (e.getUseCaseID ().equals (sUseCaseID))
      {
        aFound = e;
        break;
      }

    final JsonObject aResult = new JsonObject ();
    aResult.add ("useCaseId", sUseCaseID);
    aResult.add ("inCodelist", aFound != null);
    aResult.add ("codeListVersion", EPredefinedSPISUseCaseIdentifier.CODE_LIST_VERSION);

    if (aFound != null)
    {
      aResult.add ("state", aFound.getState ().getID ());
      aResult.add ("initialRelease", aFound.getInitialRelease ().toString ());
      if (aFound.getDeprecationRelease () != null)
        aResult.add ("deprecationRelease", aFound.getDeprecationRelease ().toString ());
      if (aFound.getRemovalDate () != null)
        aResult.add ("removalDate", aFound.getRemovalDate ().toString ());
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
  private IJsonObject _getCodelistVersion ()
  {
    return new JsonObject ().add ("participantIdentifierSchemeCodelistVersion",
                                  EPredefinedParticipantIdentifierScheme.CODE_LIST_VERSION)
                            .add ("documentTypeCodelistVersion", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION)
                            .add ("processCodelistVersion", EPredefinedProcessIdentifier.CODE_LIST_VERSION)
                            .add ("spisUseCaseCodelistVersion", EPredefinedSPISUseCaseIdentifier.CODE_LIST_VERSION);
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
  private IJsonObject _listParticipantIdSchemes (@Nullable final String sState,
                                                 @Nullable final String sCountryCode,
                                                 @Nullable final String sQuery,
                                                 final int nOffset,
                                                 final int nLimit)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aMatching = new JsonArray ();

    for (final IPeppolParticipantIdentifierScheme aScheme : PeppolParticipantIdentifierSchemeManager.getAllSchemes ())
    {
      if (eStateFilter != null && aScheme.getState () != eStateFilter)
        continue;
      if (StringHelper.isNotEmpty (sCountryCode) && !sCountryCode.equalsIgnoreCase (aScheme.getCountryCode ()))
        continue;
      if (!_matchesQuery (sQuery,
                          aScheme.getISO6523Code (),
                          aScheme.getSchemeID (),
                          aScheme.getSchemeName (),
                          aScheme.getSchemeAgency (),
                          aScheme.getCountryCode ()))
        continue;

      final JsonObject aEntry = new JsonObject ();
      aEntry.add ("iso6523Code", aScheme.getISO6523Code ());
      aEntry.add ("schemeID", aScheme.getSchemeID ());
      aEntry.add ("schemeName", aScheme.getSchemeName ());
      aEntry.add ("schemeAgency", aScheme.getSchemeAgency ());
      aEntry.add ("countryCode", aScheme.getCountryCode ());
      aEntry.add ("state", aScheme.getState ().getID ());
      if (aScheme.getRemovalDate () != null)
        aEntry.add ("removalDate", aScheme.getRemovalDate ().toString ());
      aMatching.add (aEntry);
    }

    return _buildListResult (EPredefinedParticipantIdentifierScheme.CODE_LIST_VERSION, aMatching, nOffset, nLimit);
  }

  @NonNull
  public SyncToolSpecification listParticipantIdSchemesTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_participant_id_schemes")
                                    .description ("""
                                        Lists all Participant identifier schemes (ISO 6523 codes) from the \
                                        official Peppol codelist. Can be filtered by state, country code, \
                                        and/or a text query that matches against scheme ID, name, agency, \
                                        ISO 6523 code, or country code. Results are paginated (default limit: \
                                        50).""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all states are included."),
                                                                                    "countryCode",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional ISO 3166-1 alpha-2 country code to filter by, e.g. 'DE', 'AT', 'NO'"),
                                                                                    "query",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional case-insensitive text search across scheme ID, name, agency, ISO 6523 code, and country code"),
                                                                                    "offset",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Number of matching entries to skip (default 0)"),
                                                                                    "limit",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Maximum number of entries to return (default " +
                                                                                                           DEFAULT_LIMIT +
                                                                                                           ")")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sState = (String) aArgs.get ("state");
      final String sCountryCode = (String) aArgs.get ("countryCode");
      final String sQuery = (String) aArgs.get ("query");
      final int nOffset = _parseOffset (aArgs.get ("offset"));
      final int nLimit = _parseLimit (aArgs.get ("limit"));
      return Helper.executeWithErrorHandling ( () -> _listParticipantIdSchemes (sState,
                                                                                sCountryCode,
                                                                                sQuery,
                                                                                nOffset,
                                                                                nLimit));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 7: List all document type identifiers
  // -------------------------------------------------------------------------

  @NonNull
  private IJsonObject _listDocumentTypeIds (@Nullable final String sState,
                                            @Nullable final String sQuery,
                                            @Nullable final String sDomainCommunity,
                                            final int nOffset,
                                            final int nLimit)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aMatching = new JsonArray ();

    for (final IPeppolPredefinedDocumentTypeIdentifier aDT : PredefinedDocumentTypeIdentifierManager.getAllDocumentTypeIdentifiers ())
    {
      if (eStateFilter != null && aDT.getState () != eStateFilter)
        continue;
      if (StringHelper.isNotEmpty (sDomainCommunity) && !sDomainCommunity.equalsIgnoreCase (aDT.getDomainCommunity ()))
        continue;
      if (!_matchesQuery (sQuery, aDT.getCommonName (), aDT.getValue ()))
        continue;

      final JsonObject aEntry = new JsonObject ();
      aEntry.add ("documentTypeId", aDT.getScheme () + "::" + aDT.getValue ());
      aEntry.add ("commonName", aDT.getCommonName ());
      aEntry.add ("state", aDT.getState ().getID ());
      aEntry.add ("bisVersion", aDT.getBISVersion ());
      aEntry.add ("domainCommunity", aDT.getDomainCommunity ());
      if (aDT.getRemovalDate () != null)
        aEntry.add ("removalDate", aDT.getRemovalDate ().toString ());
      aMatching.add (aEntry);
    }

    return _buildListResult (EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION, aMatching, nOffset, nLimit);
  }

  @NonNull
  public SyncToolSpecification listDocumentTypeIdsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_document_type_ids")
                                    .description ("""
                                        Lists Document Type identifiers from the official Peppol codelist. \
                                        Can be filtered by state, domain community, and/or a text query that \
                                        matches against the common name or identifier value. Use this to \
                                        discover which document types (invoices, credit notes, orders, etc.) \
                                        are defined in the Peppol network. Results are paginated (default limit: \
                                        50).""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all states are included."),
                                                                                    "query",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional case-insensitive text search across the common name and identifier value, e.g. 'credit note', 'UBL.BE', 'XRechnung'"),
                                                                                    "domainCommunity",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by domain community, e.g. 'POAC', 'PRAC', 'Logistics'"),
                                                                                    "offset",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Number of matching entries to skip (default 0)"),
                                                                                    "limit",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Maximum number of entries to return (default " +
                                                                                                           DEFAULT_LIMIT +
                                                                                                           ")")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sState = (String) aArgs.get ("state");
      final String sQuery = (String) aArgs.get ("query");
      final String sDomainCommunity = (String) aArgs.get ("domainCommunity");
      final int nOffset = _parseOffset (aArgs.get ("offset"));
      final int nLimit = _parseLimit (aArgs.get ("limit"));
      return Helper.executeWithErrorHandling ( () -> _listDocumentTypeIds (sState,
                                                                           sQuery,
                                                                           sDomainCommunity,
                                                                           nOffset,
                                                                           nLimit));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 8: List all process identifiers
  // -------------------------------------------------------------------------

  @NonNull
  private IJsonObject _listProcessIds (@Nullable final String sState,
                                       @Nullable final String sQuery,
                                       final int nOffset,
                                       final int nLimit)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aMatching = new JsonArray ();

    for (final IPeppolPredefinedProcessIdentifier aProc : PredefinedProcessIdentifierManager.getAllProcessIdentifiers ())
    {
      if (eStateFilter != null && aProc.getState () != eStateFilter)
        continue;
      if (!_matchesQuery (sQuery, aProc.getValue ()))
        continue;

      final JsonObject aEntry = new JsonObject ();
      aEntry.add ("processId", aProc.getScheme () + "::" + aProc.getValue ());
      aEntry.add ("scheme", aProc.getScheme ());
      aEntry.add ("value", aProc.getValue ());
      aEntry.add ("state", aProc.getState ().getID ());
      aMatching.add (aEntry);
    }

    return _buildListResult (EPredefinedProcessIdentifier.CODE_LIST_VERSION, aMatching, nOffset, nLimit);
  }

  @NonNull
  public SyncToolSpecification listProcessIdsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_process_ids")
                                    .description ("""
                                        Lists Process identifiers from the official Peppol codelist. \
                                        Can be filtered by state and/or a text query that matches against \
                                        the process identifier value. Use this to discover which business \
                                        processes (billing, ordering, despatch advice, etc.) are defined \
                                        in the Peppol network. Results are paginated (default limit: \
                                        50).""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all states are included."),
                                                                                    "query",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional case-insensitive text search on the process identifier value, e.g. 'billing', 'ordering'"),
                                                                                    "offset",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Number of matching entries to skip (default 0)"),
                                                                                    "limit",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Maximum number of entries to return (default " +
                                                                                                           DEFAULT_LIMIT +
                                                                                                           ")")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sState = (String) aArgs.get ("state");
      final String sQuery = (String) aArgs.get ("query");
      final int nOffset = _parseOffset (aArgs.get ("offset"));
      final int nLimit = _parseLimit (aArgs.get ("limit"));
      return Helper.executeWithErrorHandling ( () -> _listProcessIds (sState, sQuery, nOffset, nLimit));
    });
  }

  // -------------------------------------------------------------------------
  // Tool 9: List all SPIS Use Case identifiers
  // -------------------------------------------------------------------------

  @NonNull
  private IJsonObject _listSPISUseCaseIds (@Nullable final String sState,
                                           @Nullable final String sQuery,
                                           final int nOffset,
                                           final int nLimit)
  {
    final var eStateFilter = _parseStateFilter (sState);
    final var aMatching = new JsonArray ();

    for (final var aUseCase : EPredefinedSPISUseCaseIdentifier.values ())
    {
      if (eStateFilter != null && aUseCase.getState () != eStateFilter)
        continue;
      if (!_matchesQuery (sQuery, aUseCase.getUseCaseID ()))
        continue;

      final JsonObject aEntry = new JsonObject ();
      aEntry.add ("useCaseId", aUseCase.getUseCaseID ());
      aEntry.add ("state", aUseCase.getState ().getID ());
      aEntry.add ("initialRelease", aUseCase.getInitialRelease ().toString ());
      if (aUseCase.getDeprecationRelease () != null)
        aEntry.add ("deprecationRelease", aUseCase.getDeprecationRelease ().toString ());
      if (aUseCase.getRemovalDate () != null)
        aEntry.add ("removalDate", aUseCase.getRemovalDate ().toString ());
      aMatching.add (aEntry);
    }

    return _buildListResult (EPredefinedSPISUseCaseIdentifier.CODE_LIST_VERSION, aMatching, nOffset, nLimit);
  }

  @NonNull
  public SyncToolSpecification listSPISUseCaseIdsTool ()
  {
    final var aTool = McpSchema.Tool.builder ()
                                    .name ("list_spis_use_case_ids")
                                    .description ("""
                                        Lists SPIS (Service Provider Information Service) Use Case \
                                        identifiers from the official Peppol codelist. Can be filtered by \
                                        state and/or a text query. Results are paginated (default limit: \
                                        50).""")
                                    .inputSchema (new McpSchema.JsonSchema ("object",
                                                                            Map.of ("state",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional filter by state: 'act' (active), 'dep' (deprecated), 'rem' (removed). If omitted, all states are included."),
                                                                                    "query",
                                                                                    Map.of ("type",
                                                                                            "string",
                                                                                            "description",
                                                                                            "Optional case-insensitive text search on the use case ID"),
                                                                                    "offset",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Number of matching entries to skip (default 0)"),
                                                                                    "limit",
                                                                                    Map.of ("type",
                                                                                            "integer",
                                                                                            "description",
                                                                                            "Maximum number of entries to return (default " +
                                                                                                           DEFAULT_LIMIT +
                                                                                                           ")")),
                                                                            List.of (),
                                                                            Boolean.FALSE,
                                                                            null,
                                                                            null))
                                    .build ();

    return new SyncToolSpecification (aTool, (exchange, request) -> {
      final var aArgs = request.arguments ();
      final String sState = (String) aArgs.get ("state");
      final String sQuery = (String) aArgs.get ("query");
      final int nOffset = _parseOffset (aArgs.get ("offset"));
      final int nLimit = _parseLimit (aArgs.get ("limit"));
      return Helper.executeWithErrorHandling ( () -> _listSPISUseCaseIds (sState, sQuery, nOffset, nLimit));
    });
  }
}
