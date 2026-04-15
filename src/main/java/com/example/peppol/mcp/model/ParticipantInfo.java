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
package com.example.peppol.mcp.model;

import org.jspecify.annotations.NonNull;

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

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

  @NonNull
  public IJsonObject getAsJson ()
  {
    final JsonObject ret = new JsonObject ();
    if (participantId != null)
      ret.add ("participantId", participantId);
    if (network != null)
      ret.add ("network", network);
    if (smpUrl != null)
      ret.add ("smpUrl", smpUrl);
    ret.add ("registered", registered);
    if (message != null)
      ret.add ("message", message);
    return ret;
  }
}
