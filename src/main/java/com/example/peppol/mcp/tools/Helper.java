package com.example.peppol.mcp.tools;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.ObjectMapper;

@Immutable
final class Helper
{
  private static final Logger LOG = LoggerFactory.getLogger (Helper.class);
  static final ObjectMapper MAPPER = new ObjectMapper ();

  private Helper ()
  {}

  @NonNull
  static CallToolResult executeWithErrorHandling (@NonNull final IThrowingSupplier <?, Exception> aSupplier)
  {
    try
    {
      final Object aResult = aSupplier.get ();
      final String sJson = MAPPER.writerWithDefaultPrettyPrinter ().writeValueAsString (aResult);
      return McpSchema.CallToolResult.builder ().addTextContent (sJson).isError (Boolean.FALSE).build ();
    }
    catch (final Exception ex)
    {
      LOG.error ("Tool execution failed", ex);
      return McpSchema.CallToolResult.builder ()
                                     .addTextContent ("Error: " + ex.getMessage ())
                                     .isError (Boolean.TRUE)
                                     .build ();
    }
  }

  @Nullable
  static IParticipantIdentifier parseParticipantId (@NonNull final String sPID, final boolean bThrow)
  {
    var aPID = PeppolIdentifierFactory.INSTANCE.parseParticipantIdentifier (sPID);
    if (aPID == null)
      aPID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sPID);
    if (aPID == null && bThrow)
      throw new IllegalArgumentException ("Invalid Peppol Participant ID format '" +
                                          sPID +
                                          "'. Expected format is scheme:value, e.g. 0088:4012345678901");
    return aPID;
  }

  @Nullable
  static IDocumentTypeIdentifier parseDocTypeID (@NonNull final String sDTID, final boolean bThrow)
  {
    var aDTID = PeppolIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (sDTID);
    if (aDTID == null)
      aDTID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme (sDTID);
    if (aDTID == null && bThrow)
      throw new IllegalArgumentException ("Invalid Peppol Document Type ID format '" +
                                          sDTID +
                                          "'. Expected format is scheme::value, e.g. busdox-docid-qns::xyz");
    return aDTID;
  }

  @Nullable
  static IProcessIdentifier parseProcessID (@NonNull final String sPRID, final boolean bThrow)
  {
    var aPRID = PeppolIdentifierFactory.INSTANCE.parseProcessIdentifier (sPRID);
    if (aPRID == null)
      aPRID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme (sPRID);
    if (aPRID == null && bThrow)
      throw new IllegalArgumentException ("Invalid Peppol Process ID format '" +
                                          sPRID +
                                          "'. Expected format is scheme::value, e.g. cenbii-procid-ubl::xyz");
    return aPRID;
  }
}
