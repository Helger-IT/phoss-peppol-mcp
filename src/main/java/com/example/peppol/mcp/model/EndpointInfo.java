package com.example.peppol.mcp.model;

public class EndpointInfo
{
  private String participantId;
  private String endpointUrl;
  private String transportProfile;
  private String certificateSubject;
  private boolean found;

  public String getParticipantId () { return participantId; }
  public void setParticipantId (String v) { this.participantId = v; }
  public String getEndpointUrl () { return endpointUrl; }
  public void setEndpointUrl (String v) { this.endpointUrl = v; }
  public String getTransportProfile () { return transportProfile; }
  public void setTransportProfile (String v) { this.transportProfile = v; }
  public String getCertificateSubject () { return certificateSubject; }
  public void setCertificateSubject (String v) { this.certificateSubject = v; }
  public boolean isFound () { return found; }
  public void setFound (boolean v) { this.found = v; }
}
