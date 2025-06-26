package com.meuempresa.assinador.data;

import java.awt.Rectangle;
import java.io.File;

public class PDFInfo {
    private final File file;
    private final String fileName;
    private int totalPages;
    private int currentPage;
    private Rectangle signatureRect;

    public PDFInfo(File file, int totalPages, int currentPage, Rectangle signatureRect) {
        this.file = file;
        this.fileName = file.getName();
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.signatureRect = signatureRect;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public Rectangle getSignatureRect() {
        return signatureRect;
    }

    public void setSignatureRect(Rectangle signatureRect) {
        this.signatureRect = signatureRect;
    }
}