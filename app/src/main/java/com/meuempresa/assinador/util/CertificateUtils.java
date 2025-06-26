package com.meuempresa.assinador.util;

import com.meuempresa.assinador.certmanager.CertificateStore;
import com.meuempresa.assinador.certmanager.CardCertificate;
import java.util.ArrayList;
import java.util.List;

public class CertificateUtils {
    public static String[] getRealCertificates() {
        CertificateStore store = new CertificateStore();
        List<CardCertificate> lista = store.listarCertificados();
        List<String> filtered = new ArrayList<>();
        for (CardCertificate cert : lista) {
            if (!cert.getIssuer().toLowerCase().contains("cartão de cidadão")) {
                filtered.add(cert.getCommonName());
            }
        }
        return filtered.toArray(new String[0]);
    }
}