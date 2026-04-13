package com.example.peppol.mcp.model;

public class EndpointInfo extends DocumentTypeEndpointInfo
{
  private String participantID;
  private String documentTypeID;
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

  public boolean isFound ()
  {
    return found;
  }

  public void setFound (final boolean v)
  {
    this.found = v;
  }
}
