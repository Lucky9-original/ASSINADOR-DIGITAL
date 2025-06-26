package com.meuempresa.assinador.data;

import java.awt.Rectangle;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PDFCache {
    private final Map<String, PDFInfo> cache;

    public PDFCache() {
        cache = new HashMap<>();
    }

    private String key(File file) {
        return file.getAbsolutePath().toLowerCase();
    }

    // Método auxiliar que imprime o conteúdo da cache
    private void printCache() {
        System.out.println("----- PDFCache Contents -----");
        if (cache.isEmpty()) {
            System.out.println("Cache is EMPTY");
        } else {
            for (Map.Entry<String, PDFInfo> entry : cache.entrySet()) {
                PDFInfo info = entry.getValue();
                System.out.println(String.format("File: %s | TotalPages: %d | CurrentPage: %d | SignatureRect: %s",
                        info.getFileName(),
                        info.getTotalPages(),
                        info.getCurrentPage(),
                        info.getSignatureRect()));
            }
        }
        System.out.println("----- End of Cache -----");
    }

    public void updatePDFInfo(File file, int totalPages, int currentPage, Rectangle signatureRect) {
        String k = key(file);
        PDFInfo info = new PDFInfo(file, totalPages, currentPage, signatureRect);
        cache.put(k, info);
        printCache(); // Imprime o cache após atualizar
    }

    public PDFInfo getPDFInfo(File file) {
        return cache.get(key(file));
    }

    public void removePDFInfo(File file) {
        cache.remove(key(file));
        printCache(); // Imprime o cache após remover
    }

    public Collection<PDFInfo> getAllPDFInfos() {
        return cache.values();
    }

    public void clear() {
        cache.clear();
        printCache();
    }
}