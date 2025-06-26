package com.meuempresa.assinador.signer;

import com.meuempresa.assinador.certmanager.LocalCertificate;
import com.meuempresa.assinador.data.PDFInfo;
import com.meuempresa.assinador.gui.ProgressDialog;

import javax.swing.SwingWorker;
import java.awt.Frame;
import java.io.File;
import java.util.List;

/**
 * Responsável por assinar vários PDFs e mostrar uma janela de progresso.
 */
public class DocumentSignerLocal {

    /**
     * Assina todos os documentos da lista usando os dados de assinatura armazenados,
     * exibindo uma janela modal de progresso durante o processo.
     *
     * @param owner        janela pai para o diálogo de progresso (normalmente o JFrame principal)
     * @param pdfInfos     lista de PDFInfo com ficheiro, retângulo e página
     * @param alias        Common Name do certificado (alias)
     * @param keyPassword  senha da chave privada
     * @param despachoText texto a incluir na layer2 da assinatura
     * @throws Exception em caso de erro na assinatura
     */
    public static void signAllDocuments(
            Frame owner,
            List<PDFInfo> pdfInfos,
            String alias,
            String keyPassword,
            String despachoText
    ) throws Exception {
        // Validação prévia
        for (PDFInfo info : pdfInfos) {
            if (info.getSignatureRect() == null || info.getCurrentPage() <= 0) {
                throw new Exception("O documento " +
                        info.getFile().getName() +
                        " não possui dados de assinatura completos.");
            }
        }

        // Cria o diálogo de progresso
        ProgressDialog progress = new ProgressDialog(owner, "Assinando documentos", pdfInfos.size());

        // SwingWorker para não bloquear o Event Dispatch Thread
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                int done = 0;
                for (PDFInfo info : pdfInfos) {
                    done++;

                    File originalFile = info.getFile();
                    java.awt.Rectangle awtRect = info.getSignatureRect();
                    com.itextpdf.text.Rectangle itextRect = new com.itextpdf.text.Rectangle(
                            awtRect.x,
                            awtRect.y,
                            awtRect.x + awtRect.width,
                            awtRect.y + awtRect.height
                    );
                    int page = info.getCurrentPage();

                    // Assina o PDF
                    LocalCertificate.signPdfPAdES(
                        originalFile,
                        alias,
                        keyPassword,
                        itextRect,
                        page,
                        despachoText
                    );

                    // Publica o progresso (valor de 1 até pdfInfos.size())
                    publish(done);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                // Recebe atualizações de progresso
                int last = chunks.get(chunks.size() - 1);
                String name = pdfInfos.get(last - 1).getFile().getName();
                progress.updateProgress(
                    last,
                    String.format("Assinado %d de %d: %s",
                        last, pdfInfos.size(), name
                    )
                );
            }

            @Override
            protected void done() {
                progress.dispose();
            }
        };

        // Executa e mostra modal
        worker.execute();
        progress.setVisible(true);
    }

}
