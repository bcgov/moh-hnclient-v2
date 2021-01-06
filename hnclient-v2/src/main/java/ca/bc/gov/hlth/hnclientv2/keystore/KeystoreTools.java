package ca.bc.gov.hlth.hnclientv2.keystore;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class KeystoreTools {
    public static KeyStore loadKeyStore(File keystoreFile, String password, String keyStoreType) throws Exception {
        try (InputStream is = keystoreFile.toURI().toURL().openStream()) {
            KeyStore keystore = KeyStore.getInstance(keyStoreType);
            keystore.load(is, password.toCharArray());
            return keystore;
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    public static LocalDate getKeystoreExpiry(KeyStore keystore, String alias) throws KeyStoreException {
        Date certExpiryDate = ((X509Certificate) keystore.getCertificate(alias)).getNotAfter();
        return LocalDate.ofInstant(certExpiryDate.toInstant(), ZoneId.systemDefault());
    }

    public static X509Certificate generateX509(KeyPair keyPair, int certExpiryYears, String clientId) throws OperatorCreationException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        //Setup effective and expiry dates
        //Bouncy Castle currently only works with java.util.Date but it's still nicer to create them with java.time.LocalDate
        LocalDate effectiveLocalDate = LocalDate.now();
        LocalDate expiryLocalDate = effectiveLocalDate.plusYears(certExpiryYears);

        Date effectiveDate = java.sql.Date.valueOf(effectiveLocalDate);
        Date expiryDate = java.sql.Date.valueOf(expiryLocalDate);

        //Random for the cert serial
        SecureRandom random = new SecureRandom();

        //Set X509 initialization properties
        X500Name issuer = new X500Name("CN=" + clientId);
        BigInteger serial = new BigInteger(160, random);
        Time notBefore = new Time(effectiveDate);
        Time notAfter = new Time(expiryDate);
        X500Name subject = new X500Name("CN=" + clientId);
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        //Create cert builder
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);
        //Create cert signer using private key
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        //Create the X509 certificate
        X509CertificateHolder certHolder = certBuilder.build(signer);
        //Extract X509 cert from custom Bouncy Castle wrapper
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certHolder);

        cert.verify(keyPair.getPublic());

        return cert;
    }
}
