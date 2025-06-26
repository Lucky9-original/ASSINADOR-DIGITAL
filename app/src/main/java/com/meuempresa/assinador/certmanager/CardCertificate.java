package com.meuempresa.assinador.certmanager;

import java.util.Date;

public class CardCertificate {

    private String commonName;
    private String issuer;
    private Date validade;

    public CardCertificate(String commonName, String issuer, Date validade) {
        this.commonName = commonName;
        this.issuer = issuer;
        this.validade = validade;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getIssuer() {
        return issuer;
    }

    public Date getValidade() {
        return validade;
    }

    @Override
    public String toString() {
        return commonName + "\n  Emissor: " + issuer + "\n  Validade: " + validade;
    }
}
