package com.example.peppol.mcp.model;

import java.security.cert.CertificateException;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xsds.peppol.smp1.EndpointType;

@JsonInclude (JsonInclude.Include.NON_NULL)
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
