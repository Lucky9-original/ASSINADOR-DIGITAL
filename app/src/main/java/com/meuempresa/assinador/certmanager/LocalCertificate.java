package com.meuempresa.assinador.certmanager;
import com.meuempresa.assinador.gui.Signature;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.awt.image.BufferedImage;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.imageio.ImageIO;

import java.io.ByteArrayOutputStream;


public class LocalCertificate {

    public static KeyStore loadWindowsMyKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
        return ks;
    }
    
    /**
     * Extrai o Common Name (CN) do DN do certificado.
     */
    private static String extractCN(String subjectDN) {
        String[] tokens = subjectDN.split(",");
        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith("CN=")) {
                return token.substring(3);
            }
        }
        return null;
    }
    
    /**
     * Assina digitalmente um PDF segundo o padrão PAdES, criando um campo de assinatura visível.
     * O selo exibirá:
     *   - O CN do certificado;
     *   - A data/hora da assinatura (com fuso horário);
     *   - Uma imagem de marca (marca.png) dos recursos.
     * 
     *
     * @param pdfFile         Arquivo PDF original.
     * @param alias           Alias do certificado a ser utilizado.
     * @param keyPassword     Senha da chave.
     * @param rect            Retângulo onde a assinatura será exibida.
     * @param page            Número da página onde a assinatura aparecerá.
     * @param layer2Text      Texto adicional que será exibido na assinatura.
     * @throws Exception      Se ocorrer algum erro no processo de assinatura.
     */
    public static void signPdfPAdES(File pdfFile, String alias, String keyPassword,
                                    com.itextpdf.text.Rectangle rect, int page,
                                    String layer2Text) throws Exception {
        // Registra o provider BouncyCastle, se necessário.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        
        // Carrega o keystore e obtém a chave privada e a cadeia de certificados.
        KeyStore ks = loadWindowsMyKeystore();
        if (!ks.containsAlias(alias)) {
            throw new Exception("Certificado com alias '" + alias + "' não encontrado.");
        }
        PrivateKey pk = (PrivateKey) ks.getKey(alias, keyPassword.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);
        if (chain == null || chain.length == 0) {
            throw new Exception("Cadeia de certificados não encontrada para o alias '" + alias + "'.");
        }

        // Gera o novo caminho de arquivo: insere "_signed" antes da extensão
        String originalPath = pdfFile.getAbsolutePath();
        int dotIndex = originalPath.lastIndexOf('.');
        String newFilePath;
        if (dotIndex != -1) {
            newFilePath = originalPath.substring(0, dotIndex) + "_signed" + originalPath.substring(dotIndex);
        } else {
            newFilePath = originalPath + "_signed";
        }
        File signedFile = new File(newFilePath);

        PdfReader reader = null;
        FileOutputStream os = null;
        PdfStamper stamper = null;
        
        // Carrega o PDF original.
        try {
            reader = new PdfReader(pdfFile.getAbsolutePath());
            os = new FileOutputStream(signedFile);
        
            // Cria o stamper para assinatura.
            stamper = PdfStamper.createSignature(reader, os, '\0', /*tempFile*/ null, /*append*/ true);
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            // Não bloqueia alterações futuras, apenas marca como NÃO-CERTIFICADO
            appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
            appearance.setReason("Assinatura digital local");
            appearance.setLocation("Portugal");
            
            // Usa o retângulo e a página passados como parâmetros.
            // Converte as coordenadas para o sistema do PDF (origem no canto inferior esquerdo)
            float llx = rect.getLeft();
            float lly = rect.getTop() - rect.getHeight();
            // A coordenada X direita é a esquerda + largura; a coordenada Y superior é o topo convertido:
            float urx = llx + rect.getWidth();
            float ury = rect.getTop();

            Rectangle correctRect = new Rectangle(llx, lly, urx, ury);

            String uniqueFieldName = "SignatureField_" + System.currentTimeMillis();

            // Configura layers modernos e posiciona o selo com as coordenadas convertidas.
            appearance.setAcro6Layers(true);
            appearance.setVisibleSignature(correctRect, page, uniqueFieldName);
            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION);
            

            // Tenta carregar a imagem da marca (marca.png) dos recursos.
            try (InputStream imageStream = LocalCertificate.class.getClassLoader().getResourceAsStream("marca.png");) {
                if (imageStream != null) {
                    byte[] imageBytes = readAllBytes(imageStream);
                    imageStream.close();
                    com.itextpdf.text.Image signatureImage = com.itextpdf.text.Image.getInstance(imageBytes);
                    appearance.setSignatureGraphic(signatureImage);
                } else {
                    System.out.println("Imagem da marca (marca.png) não encontrada nos recursos.");
                }
            }
            
            // Em vez de chamar appearance.setLayer2Text(...), criei uma aparência personalizada:
            String certificateCN = extractCN(((X509Certificate) chain[0]).getSubjectDN().toString());
            customizeSignatureLayer(appearance, certificateCN, layer2Text); 
            
            // Cria o digest e a assinatura externa utilizando BouncyCastle.
            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new MSCAPIExternalSignature(pk, DigestAlgorithms.SHA256);
            
            int reserve = 8192;
            // Realiza a assinatura digital detachada (PAdES).
            MakeSignature.signDetached(appearance, digest, signature, chain, null, null, null, reserve, MakeSignature.CryptoStandard.CMS);

            // Fecha recursos
            stamper.close();
            reader.close();
            stamper = null;

        } finally {
            if (stamper != null) try {stamper.close();} catch (Exception e) {}
            if (reader != null) try {reader.close();} catch (Exception e) {}
            if (os != null) try { os.close(); } catch (Exception e) {}
        }

        
    }

    /**
     * Cria uma camada personalizada para a assinatura.
     * Se houver despacho (layer2Text não vazio), divide a área em dois:
     * a metade superior para o texto e a inferior para a imagem.
     * Caso contrário, desenha apenas a imagem centralizada na área completa.
     */
    private static void customizeSignatureLayer(PdfSignatureAppearance appearance, String certificateCN, String layer2Text) throws Exception {
        // Obtém as dimensões definidas para a área da assinatura
        Rectangle boundingBox = appearance.getRect();
        int width = (int) boundingBox.getWidth();
        int height = (int) boundingBox.getHeight();

        // Tenta carregar a imagem da marca (marca.png) dos recursos.
        BufferedImage signatureImage = null;
        InputStream imageStream = LocalCertificate.class.getClassLoader().getResourceAsStream("marca.png");
        if (imageStream != null) {
            signatureImage = ImageIO.read(imageStream);
            imageStream.close();
        } else {
            System.out.println("Imagem da marca (marca.png) não encontrada nos recursos.");
        }
        
        // Chama a função createSignatureImage da classe Signature para gerar a imagem do preview.
        BufferedImage previewImage = Signature.createSignatureImage(certificateCN, layer2Text, width, height, signatureImage);
        
        // Converte a imagem para um array de bytes em formato PNG (mantida em memória)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(previewImage, "png", baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        
        // Cria a imagem do iText a partir dos bytes gerados
        com.itextpdf.text.Image sigImage = com.itextpdf.text.Image.getInstance(imageBytes);
        
        // Configura a aparência da assinatura para usar a imagem gerada e define o modo de renderização para GRAPHIC
        appearance.setSignatureGraphic(sigImage);
        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
    }
    
    // Método auxiliar para Java 8 (readAllBytes não está disponível no InputStream)
    private static byte[] readAllBytes(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

}
