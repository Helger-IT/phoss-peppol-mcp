package com.example.peppol.mcp.model;

import java.util.ArrayList;
import java.util.List;

public class ParticipantInfo
{
  private String participantId;
  private String smpUrl;
  private boolean registered;
  private List <String> supportedDocumentTypes = new ArrayList <> ();

  public String getParticipantId () { return participantId; }
  public void setParticipantId (String v) { this.participantId = v; }
  public String getSmpUrl () { return smpUrl; }
  public void setSmpUrl (String v) { this.smpUrl = v; }
  public boolean isRegistered () { return registered; }
  public void setRegistered (boolean v) { this.registered = v; }
  public List <String> getSupportedDocumentTypes () { return supportedDocumentTypes; }
  public void setSupportedDocumentTypes (List <String> v) { this.supportedDocumentTypes = v; }
}
