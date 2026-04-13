package com.example.peppol.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude (JsonInclude.Include.NON_NULL)
public class ParticipantInfo
{
  private String participantId;
  private String network;
  private String smpUrl;
  private boolean registered;
  private String message;

  public String getParticipantId ()
  {
    return participantId;
  }

  public void setParticipantId (final String v)
  {
    this.participantId = v;
  }

  public String getNetwork ()
  {
    return network;
  }

  public void setNetwork (final String v)
  {
    this.network = v;
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

  public String getMessage ()
  {
    return message;
  }

  public void setMessage (final String v)
  {
    this.message = v;
  }
}
