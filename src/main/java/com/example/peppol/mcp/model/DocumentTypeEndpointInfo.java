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

import java.security.cert.CertificateException;

import org.jspecify.annotations.NonNull;

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xsds.peppol.smp1.EndpointType;

public class DocumentTypeEndpointInfo
{
  private String processID;
  private String endpointUrl;
  private String transportProfile;
  private String certificateIssuer;
  private String certificateSubject;

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

  @NonNull
  public IJsonObject getAsJson ()
  {
    final JsonObject ret = new JsonObject ();
    if (processID != null)
      ret.add ("processID", processID);
    if (endpointUrl != null)
      ret.add ("endpointUrl", endpointUrl);
    if (transportProfile != null)
      ret.add ("transportProfile", transportProfile);
    if (certificateIssuer != null)
      ret.add ("certificateIssuer", certificateIssuer);
    if (certificateSubject != null)
      ret.add ("certificateSubject", certificateSubject);
    return ret;
  }

  public void init (@NonNull final EndpointType aEndpoint) throws CertificateException
  {
    setEndpointUrl (SMPClientReadOnly.getEndpointAddress (aEndpoint));
    setTransportProfile (aEndpoint.getTransportProfile ());
    final var aCert = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
    if (aCert != null)
    {
      setCertificateIssuer (aCert.getIssuerX500Principal ().getName ());
      setCertificateSubject (aCert.getSubjectX500Principal ().getName ());
    }
  }
}
