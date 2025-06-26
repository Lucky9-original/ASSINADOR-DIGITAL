package com.meuempresa.assinador.signer;

import pt.gov.cartaodecidadao.PTEID_ReaderSet;
import pt.gov.cartaodecidadao.PTEID_EIDCard;
import pt.gov.cartaodecidadao.PTEID_PDFSignature;
import pt.gov.cartaodecidadao.PTEID_Exception;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.DocumentException;

public class DocumentSigner {

    /**
     * Assina um documento PDF inserindo um selo visível na posição especificada.
     * Esse método utiliza a classe PTEID_PDFSignature para carregar o PDF e gerar um novo
     * PDF com o selo de assinatura visível.
     *
     * @param pdfFile    Arquivo PDF a ser assinado.
     * @param outputFile Arquivo de saída (o PDF assinado).
     * @param page       Número da página onde o selo será inserido.
     * @param signatureRect Retângulo (em coordenadas do PDF) definido para a assinatura.
     *                      Essa área será totalmente ocupada pelo selo.
     * @return O código de retorno da operação (geralmente 0 indica sucesso).
     * @throws PTEID_Exception Se ocorrer algum erro durante o processo de assinatura.
     */
    public int signPdf(File pdfFile, File outputFile, int page, java.awt.Rectangle signatureRect, String location, String txtDespacho) throws PTEID_Exception, IOException, DocumentException {
        // Se outputFile for nulo, gera o caminho com "_signed" antes da extensão
        if (outputFile == null) {
            String originalPath = pdfFile.getAbsolutePath();
            int dotIndex = originalPath.lastIndexOf('.');
            String newFilePath = (dotIndex != -1)
                    ? originalPath.substring(0, dotIndex) + "_signed" + originalPath.substring(dotIndex)
                    : originalPath + "_signed";
            outputFile = new File(newFilePath);
        }
        
        // Carrega o PDF para determinar as dimensões da página
        float pageWidth;
        float pageHeight;
        com.itextpdf.text.pdf.PdfReader reader = null;
        try {
            reader = new com.itextpdf.text.pdf.PdfReader(pdfFile.getAbsolutePath());
            com.itextpdf.text.Rectangle pageSize = reader.getPageSize(page);
            pageWidth = pageSize.getWidth();
            pageHeight = pageSize.getHeight();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        // Converter as coordenadas do retângulo para valores normalizados (de 0 a 1)
        // Se o signatureRect estiver no sistema de coordenadas do PDF (origem no canto inferior esquerdo):
        double signHeight = 50 / (double) pageHeight;
        double normX = signatureRect.x / (double) pageWidth;
        // double normY = 1.0 - (signatureRect.y / (double) pageHeight) - signHeight;
        
        // Se houver texto no 'txtDespacho' (txtDespacho), insere-o na metade superior do retângulo
        // e ajusta os parâmetros para que a assinatura seja colocada na metade inferior.
        if (txtDespacho != null && !txtDespacho.trim().isEmpty()) {
    // Usa ByteArrayOutputStream para produzir o PDF modificado em memória
    File tempStamped = File.createTempFile("dispatchStamped", ".pdf");
    tempStamped.deleteOnExit();
    ByteArrayOutputStream baos = null;
    com.itextpdf.text.pdf.PdfReader reader2 = null;
    com.itextpdf.text.pdf.PdfStamper stamper = null;
    FileOutputStream fos = null;
    try {
        baos = new ByteArrayOutputStream();
        reader2 = new com.itextpdf.text.pdf.PdfReader(pdfFile.getAbsolutePath());
        stamper = new com.itextpdf.text.pdf.PdfStamper(reader2, baos);
        com.itextpdf.text.pdf.PdfContentByte canvas = stamper.getOverContent(page);
        // Define a área de despacho: a metade superior do retângulo
        float dispX = signatureRect.x;
        float dispY = signatureRect.y + signatureRect.height / 2.0f;
        float dispWidth = signatureRect.width;
        float dispHeight = signatureRect.height / 2.0f;
        com.itextpdf.text.Rectangle dispatchRect = new com.itextpdf.text.Rectangle(dispX, dispY, dispX + dispWidth, dispY + dispHeight);
        // Prepara a fonte (Helvetica) para o despacho
        com.itextpdf.text.pdf.BaseFont bf = com.itextpdf.text.pdf.BaseFont.createFont(
                com.itextpdf.text.pdf.BaseFont.HELVETICA, com.itextpdf.text.pdf.BaseFont.CP1252, com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED);
        float margin = 4f;
        float maxFontSize = Math.min(20, dispHeight - margin * 2);
        float fontSize = maxFontSize;
        int status;
        do {
            com.itextpdf.text.Font font = new com.itextpdf.text.Font(bf, fontSize);
            com.itextpdf.text.Phrase phrase = new com.itextpdf.text.Phrase(txtDespacho, font);
            com.itextpdf.text.pdf.ColumnText ct = new com.itextpdf.text.pdf.ColumnText(canvas);
            ct.setSimpleColumn(dispatchRect);
            ct.addText(phrase);
            status = ct.go(true);  // simula a renderização sem escrever
            if ((status & com.itextpdf.text.pdf.ColumnText.NO_MORE_TEXT) == 0) {
                fontSize -= 1;
            } else {
                break;
            }
        } while (fontSize > 4);
        // Escreve o texto com o tamanho de fonte determinado
        {
            com.itextpdf.text.Font font = new com.itextpdf.text.Font(bf, fontSize);
            com.itextpdf.text.Phrase phrase = new com.itextpdf.text.Phrase(txtDespacho, font);
            com.itextpdf.text.pdf.ColumnText ct = new com.itextpdf.text.pdf.ColumnText(canvas);
            ct.setSimpleColumn(dispatchRect);
            ct.addText(phrase);
            ct.go();
        }
        stamper.close(); // fecha o stamper para garantir que tudo foi escrito
        reader2.close(); // fecha o reader
        // Escreve o PDF modificado no ficheiro temporário
        fos = new FileOutputStream(tempStamped);
        fos.write(baos.toByteArray());
    } catch (Exception e) {
        throw new IOException("Erro ao inserir o despacho: " + e.getMessage(), e);    } finally {
        if (fos != null) try { fos.close(); } catch (Exception ex) {}
        if (stamper != null) try { stamper.close(); } catch (Exception ex) {}
        if (reader2 != null) try { reader2.close(); } catch (Exception ex) {}
        if (baos != null) try { baos.close(); } catch (Exception ex) {}    }
    // Atualiza o pdfFile para o PDF com o texto de despacho já incorporado
    pdfFile = tempStamped;
    // Reposiciona a assinatura: usa como área a metade inferior do retângulo original
    double normY = 1.0 - (signatureRect.y / (double) pageHeight) - (signHeight / 2);
    // Cria o objeto de assinatura (PTEID_PDFSignature) a partir do PDF modificado
    PTEID_PDFSignature pdfSignature = new PTEID_PDFSignature(pdfFile.getAbsolutePath());
    pdfSignature.enableSmallSignatureFormat();
    int result = 0;
    try {
        PTEID_EIDCard card = PTEID_ReaderSet.instance().getReader().getEIDCard();
        // Chama o método SignPDF; repassa txtDespacho como string vazia para que a assinatura não sobreponha o despacho
        result = card.SignPDF(pdfSignature, page, normX, normY, location, "", outputFile.getAbsolutePath());
    } catch (Exception e){
        throw new IOException("Verifique se introduziu o Cartão de Cidadão e volte a tentar");
    }
    return result;
} else {
            // Caso não haja despacho, posiciona a assinatura normalmente (usando o retângulo completo)
            double normY = 1.0 - (signatureRect.y / (double) pageHeight) - signHeight;
            PTEID_PDFSignature pdfSignature = new PTEID_PDFSignature(pdfFile.getAbsolutePath());
            pdfSignature.enableSmallSignatureFormat();
            int result = 0;
            try {
                PTEID_EIDCard card = PTEID_ReaderSet.instance().getReader().getEIDCard();
                // Chama o método SignPDF; repassa txtDespacho como string vazia para que a assinatura não sobreponha o despacho
                result = card.SignPDF(pdfSignature, page, normX, normY, location, "", outputFile.getAbsolutePath());
            } catch (Exception e){
                throw new IOException("Verifique se introduziu o Cartão de Cidadão e volte a tentar");
            }
            
            return result;
        }
    }
}
