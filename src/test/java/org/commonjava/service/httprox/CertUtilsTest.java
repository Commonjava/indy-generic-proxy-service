package org.commonjava.service.httprox;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.commonjava.indy.service.httprox.util.CertUtils;
import org.commonjava.indy.service.httprox.util.CertificateAndKeys;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

public class CertUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger( CertUtilsTest.class );

    @Test
    public void testCertificateNotNull()
            throws Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "RSA" );
        assertNotNull( keyPairGenerator );
        KeyPair pair = keyPairGenerator.generateKeyPair();
        assertNotNull( pair );
        X509Certificate cert = CertUtils.generateX509Certificate( pair, "CN=<host>, O=Test Org", 42, "SHA256withRSA" );
        assertNotNull( cert );
    }

    @Test
    public void testCertificateValid()
            throws Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "RSA" );
        KeyPair pair = keyPairGenerator.generateKeyPair();
        X509Certificate cert = CertUtils.generateX509Certificate( pair, "CN=<host>, O=Test Org", 42, "SHA256withRSA" );
        cert.checkValidity( new Date() );
        PublicKey pubKey = pair.getPublic();
        PrivateKey privKey = pair.getPrivate();
        cert.verify( pubKey, BouncyCastleProvider.PROVIDER_NAME );
    }

    //@Test
    public void testCertificateNotYetValid()
            throws Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "RSA" );
        KeyPair pair = keyPairGenerator.generateKeyPair();
        X509Certificate cert = CertUtils.generateX509Certificate( pair, "CN=<host>, O=Test Org", 42, "SHA256withRSA" );
        GregorianCalendar yesterday = new GregorianCalendar();
        yesterday.add( Calendar.DAY_OF_WEEK, -1 );
        cert.checkValidity( yesterday.getTime() );
    }

    @Test
    public void testSubjectCertificateSignedByIssuerCertificateWithoutExtensionIsValid()
            throws Exception
    {
        PrivateKey caKey = CertUtils.getPrivateKey( "src/test/resources/ssl/ca.der" );
        X509Certificate caCert = CertUtils.loadX509Certificate( new File("src/test/resources/ssl", "ca.crt") );
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certificateAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert, caKey, false );
        PublicKey publicKey = certificateAndKeys.getPublicKey();
        X509CertificateHolder certHolder = new X509CertificateHolder( certificateAndKeys.getCertificate().getEncoded() );
        JcaContentVerifierProviderBuilder verifierBuilder = new JcaContentVerifierProviderBuilder().setProvider( BouncyCastleProvider.PROVIDER_NAME );
        logger.debug(">>>>>>> caCert >>>>>" + caCert + "<<<<<<<<<<");
        logger.debug(">>>>>>> generated and signed cert >>>>>" + certificateAndKeys.getCertificate() + "<<<<<<<<<<<<<");
        assertTrue( certHolder.isSignatureValid( verifierBuilder.build( caCert ) ) );
        Extension ext = certHolder.getExtension( Extension.basicConstraints );
        assertNull( ext );
    }

    @Test
    public void testIntermediateSignedCertificateWithExtension()
            throws Exception
    {
        PrivateKey caKey = CertUtils.getPrivateKey( "src/test/resources/ssl/ca.der" );
        X509Certificate caCert = CertUtils.loadX509Certificate( new File("src/test/resources/ssl", "ca.crt") );
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certificateAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert, caKey, true );
        PublicKey publicKey = certificateAndKeys.getPublicKey();
        X509CertificateHolder certHolder = new X509CertificateHolder( certificateAndKeys.getCertificate().getEncoded() );
        Extension ext = certHolder.getExtension( Extension.basicConstraints );
        assertNotNull( ext );
        assertEquals( ext.getExtnId() , Extension.basicConstraints );
        assertEquals( ext.getParsedValue(), new BasicConstraints( -1 ));
    }
}
