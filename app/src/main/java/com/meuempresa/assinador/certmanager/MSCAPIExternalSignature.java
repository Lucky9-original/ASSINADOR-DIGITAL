package com.meuempresa.assinador.certmanager;

import com.itextpdf.text.pdf.security.ExternalSignature;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

public class MSCAPIExternalSignature implements ExternalSignature {

    private final PrivateKey key;
    private final String digestAlgorithm; // Ex: "SHA-256"

    public MSCAPIExternalSignature(PrivateKey key, String digestAlgorithm) {
        this.key = key;
        this.digestAlgorithm = digestAlgorithm;
    }

    @Override
    public String getHashAlgorithm() {
        return digestAlgorithm.replace("-", "");
    }

    @Override
    public String getEncryptionAlgorithm() {
        return "RSA";
    }

    @Override
    public byte[] sign(byte[] message) throws GeneralSecurityException {
        String alg = digestAlgorithm.replace("-", "") + "withRSA";
        Signature sig = Signature.getInstance(alg, "SunMSCAPI");
        sig.initSign(key);
        sig.update(message);
        return sig.sign();
    }
}
