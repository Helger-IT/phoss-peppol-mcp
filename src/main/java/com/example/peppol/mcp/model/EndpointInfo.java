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

  @Override
  @NonNull
  public IJsonObject getAsJson ()
  {
    final JsonObject ret = new JsonObject ();
    if (participantID != null)
      ret.add ("participantID", participantID);
    if (documentTypeID != null)
      ret.add ("documentTypeID", documentTypeID);
    ret.add ("found", found);
    // Add parent fields
    for (final var aEntry : super.getAsJson ())
      ret.add (aEntry.getKey (), aEntry.getValue ());
    return ret;
  }
}
