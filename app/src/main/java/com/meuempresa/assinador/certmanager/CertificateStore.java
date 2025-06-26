package com.meuempresa.assinador.certmanager;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class CertificateStore {

    /**
     * Lista os certificados do repositório "Windows-MY" (certificados pessoais).
     *
     * @return Uma lista de CardCertificate com os dados extraídos de cada certificado.
     */
    public List<CardCertificate> listarCertificados() {
        List<CardCertificate> certificados = new ArrayList<>();
        try {
            // Cria a instância do KeyStore do Windows (certificados pessoais)
            KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null); // Não é necessário fornecer senha

            // Percorre os aliases (nomes identificadores de cada certificado)
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                if (cert != null) {
                    // Extrai o Common Name (CN) do subject do certificado
                    String subjectDN = cert.getSubjectX500Principal().getName();
                    String commonName = extractCommonName(subjectDN);
                    
                    // Extrai o emissor (issuer) e a data de validade (NotAfter)
                    String issuer = cert.getIssuerX500Principal().getName();
                    // Cria o objeto CardCertificate com os dados do certificado
                    CardCertificate cardCert = new CardCertificate(commonName, issuer, cert.getNotAfter());
                    certificados.add(cardCert);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return certificados;
    }
    
    /**
     * Extrai o Common Name (CN) a partir do DN (Distinguished Name).
     *
     * @param subjectDN O DN completo do certificado.
     * @return O valor do CN ou o DN completo se não encontrar.
     */
    private String extractCommonName(String subjectDN) {
        String[] tokens = subjectDN.split(",");
        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith("CN=")) {
                return token.substring(3);
            }
        }
        return subjectDN;
    }
}
