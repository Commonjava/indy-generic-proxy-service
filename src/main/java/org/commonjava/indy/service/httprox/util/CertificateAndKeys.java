package org.commonjava.indy.service.httprox.util;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class CertificateAndKeys
{
    private final Certificate certificate;

    private final PublicKey publicKey;

    private final PrivateKey privateKey;

    public CertificateAndKeys( Certificate certificate, KeyPair keyPair )
    {
        this.certificate = certificate;
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public CertificateAndKeys( Certificate certificate, PrivateKey privateKey, PublicKey publicKey )
    {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public Certificate getCertificate()
    {
        return certificate;
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public PublicKey getPublicKey()
    {
        return publicKey;
    }
}
