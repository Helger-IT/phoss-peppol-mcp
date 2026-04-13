package com.example.peppol.mcp.model;

public class EndpointInfo
{
  private String participantID;
  private String documentTypeID;
  private String processID;
  private String endpointUrl;
  private String transportProfile;
  private String certificateIssuer;
  private String certificateSubject;
  private boolean found;

  public String getParticipantID ()
  {
    return participantID;
  }

  public void setParticipantID (final String v)
  {
    this.participantID = v;
  }

  public String getDocumentTypeID ()
  {
    return documentTypeID;
  }

  public void setDocumentTypeID (final String v)
  {
    this.documentTypeID = v;
  }

  public String getProcessID ()
  {
    return processID;
  }

  public void setProcessID (final String v)
  {
    this.processID = v;
  }

  public String getEndpointUrl ()
  {
    return endpointUrl;
  }

  public void setEndpointUrl (final String v)
  {
    this.endpointUrl = v;
  }

  public String getTransportProfile ()
  {
    return transportProfile;
  }

  public void setTransportProfile (final String v)
  {
    this.transportProfile = v;
  }

  public String getCertificateIssuer ()
  {
    return certificateIssuer;
  }

  public void setCertificateIssuer (final String v)
  {
    this.certificateIssuer = v;
  }

  public String getCertificateSubject ()
  {
    return certificateSubject;
  }

  public void setCertificateSubject (final String v)
  {
    this.certificateSubject = v;
  }

  public boolean isFound ()
  {
    return found;
  }

  public void setFound (final boolean v)
  {
    this.found = v;
  }
}
