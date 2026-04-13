package com.example.peppol.mcp.model;

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helger.xsds.peppol.smp1.EndpointType;

@JsonInclude (JsonInclude.Include.NON_NULL)
public class DocumentTypeSupportResult
{
  private String participantId;
  private String documentTypeId;
  private boolean supported;
  private final List <DocumentTypeEndpointInfo> endpoints = new ArrayList <> ();

  public String getParticipantId ()
  {
    return participantId;
  }

  public void setParticipantId (final String v)
  {
    this.participantId = v;
  }

  public String getDocumentTypeId ()
  {
    return documentTypeId;
  }

  public void setDocumentTypeId (final String v)
  {
    this.documentTypeId = v;
  }

  public boolean isSupported ()
  {
    return supported;
  }

  public void setSupported (final boolean v)
  {
    this.supported = v;
  }

  public List <DocumentTypeEndpointInfo> getEndpoints ()
  {
    return endpoints;
  }

  public void addEndpoint (@NonNull final String sProcessID, @NonNull final EndpointType aEndpoint)
                                                                                                    throws CertificateException
  {
    final DocumentTypeEndpointInfo e = new DocumentTypeEndpointInfo ();
    e.setProcessID (sProcessID);
    e.init (aEndpoint);
    endpoints.add (e);
  }
}
