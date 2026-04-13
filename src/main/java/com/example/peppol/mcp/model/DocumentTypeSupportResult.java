package com.example.peppol.mcp.model;

import java.util.ArrayList;
import java.util.List;

public class DocumentTypeSupportResult
{
  private String participantId;
  private String documentTypeId;
  private boolean supported;
  private List <EndpointInfo> endpoints = new ArrayList <> ();

  public String getParticipantId () { return participantId; }
  public void setParticipantId (String v) { this.participantId = v; }
  public String getDocumentTypeId () { return documentTypeId; }
  public void setDocumentTypeId (String v) { this.documentTypeId = v; }
  public boolean isSupported () { return supported; }
  public void setSupported (boolean v) { this.supported = v; }
  public List <EndpointInfo> getEndpoints () { return endpoints; }

  public void addEndpoint (final String transportProfile, final String url)
  {
    final EndpointInfo e = new EndpointInfo ();
    e.setTransportProfile (transportProfile);
    e.setEndpointUrl (url);
    e.setFound (true);
    endpoints.add (e);
  }
}
