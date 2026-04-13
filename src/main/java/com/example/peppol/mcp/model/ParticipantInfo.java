package com.example.peppol.mcp.model;

public class ParticipantInfo
{
  private String participantId;
  private String smpUrl;
  private boolean registered;

  public String getParticipantId ()
  {
    return participantId;
  }

  public void setParticipantId (final String v)
  {
    this.participantId = v;
  }

  public String getSmpUrl ()
  {
    return smpUrl;
  }

  public void setSmpUrl (final String v)
  {
    this.smpUrl = v;
  }

  public boolean isRegistered ()
  {
    return registered;
  }

  public void setRegistered (final boolean v)
  {
    this.registered = v;
  }
}
