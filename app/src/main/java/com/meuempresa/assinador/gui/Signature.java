package com.meuempresa.assinador.gui;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.awt.font.GlyphVector;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.AttributedCharacterIterator;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.ImageObserver;
import java.util.Map;

import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;


public class Signature {

    /**
     * Quebra o texto em várias linhas, garantindo que cada linha não ultrapasse maxWidth.
     */
    public static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int testWidth = fm.stringWidth(testLine);
            if (testWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    /**
     * Quebra o texto em várias linhas respeitando o caractere de nova linha ("\n").
     * Para cada parágrafo, é aplicada a função wrapText.
     */
    public static List<String> wrapTextRespectNewline(String text, FontMetrics fm, int maxWidth) {
        List<String> allLines = new ArrayList<>();
        String[] paragraphs = text.split("\n");
        for (String paragraph : paragraphs) {
            List<String> wrapped = wrapText(paragraph, fm, maxWidth);
            allLines.addAll(wrapped);
        }
        return allLines;
    }

    /**
     * Resultado do ajuste de fonte para um bloco de texto.
     */
    public static class FontFitResult {
        public final Font font;
        public final List<String> lines;
        public final FontMetrics fm;

        public FontFitResult(Font font, List<String> lines, FontMetrics fm) {
            this.font = font;
            this.lines = lines;
            this.fm = fm;
        }
    }

    /**
     * Seleciona o maior tamanho de fonte (entre maxFontSize e minFontSize) que permita que o texto,
     * ao ser quebrado em linhas (usando wrapText), caiba inteiramente na caixa (box) dada, considerando a margem.
     */
    public static FontFitResult chooseFontForBox(Graphics2D g2, String text, Rectangle box,
                                                 String fontName, int fontStyle, int maxFontSize,
                                                 int minFontSize, int margin) {
        FontFitResult result = null;
        for (int fontSize = maxFontSize; fontSize >= minFontSize; fontSize--) {
            Font font = new Font(fontName, fontStyle, fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            List<String> lines = wrapText(text, fm, box.width - 2 * margin);
            boolean fitsWidth = true;
            for (String line : lines) {
                if (fm.stringWidth(line) > box.width - 2 * margin) {
                    fitsWidth = false;
                    break;
                }
            }
            int lineHeight = fm.getHeight();
            int totalTextHeight = lines.size() * lineHeight;
            if (fitsWidth && totalTextHeight <= box.height - 2 * margin) {
                result = new FontFitResult(font, lines, fm);
                break;
            }
        }
        if (result == null) {
            Font font = new Font(fontName, fontStyle, minFontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            List<String> lines = wrapText(text, fm, box.width - 2 * margin);
            result = new FontFitResult(font, lines, fm);
        }
        return result;
    }

    /**
     * Seleciona, para uma área unificada, o maior tamanho de fonte (entre maxFontSize e minFontSize)
     * que permita que o texto (com quebras forçadas) caiba inteiramente na caixa (box) dada, considerando a margem.
     */
    public static FontFitResult chooseFontForUnifiedBox(Graphics2D g2, String text, Rectangle box,
                                                        String fontName, int fontStyle, int maxFontSize,
                                                        int minFontSize, int margin) {
        FontFitResult result = null;
        for (int fontSize = maxFontSize; fontSize >= minFontSize; fontSize--) {
            Font candidate = new Font(fontName, fontStyle, fontSize);
            g2.setFont(candidate);
            FontMetrics fm = g2.getFontMetrics(candidate);
            List<String> lines = wrapTextRespectNewline(text, fm, box.width - 2 * margin);
            boolean fitsWidth = true;
            for (String line : lines) {
                if (fm.stringWidth(line) > box.width - 2 * margin) {
                    fitsWidth = false;
                    break;
                }
            }
            int totalHeight = lines.size() * fm.getHeight();
            if (fitsWidth && totalHeight <= box.height - 2 * margin) {
                result = new FontFitResult(candidate, lines, fm);
                break;
            }
        }
        if (result == null) {
            Font candidate = new Font(fontName, fontStyle, minFontSize);
            g2.setFont(candidate);
            FontMetrics fm = g2.getFontMetrics(candidate);
            List<String> lines = wrapTextRespectNewline(text, fm, box.width - 2 * margin);
            result = new FontFitResult(candidate, lines, fm);
        }
        return result;
    }

    /**
     * Desenha a pré-visualização da assinatura no retângulo (rect) definido pelo usuário.
     * A área é dividida em três partes, caso haja texto no despacho:
     *
     * 1. Área superior: se o texto do despacho (despachoText) não estiver vazio,
     *    essa área exibe o conteúdo do despacho, alinhado à esquerda e
     *    ajustado para não ultrapassar os limites do retângulo.
     *
     * 2. Área inferior dividida em duas partes:
     *     - Lado esquerdo: exibe o nome comum do certificado, centralizado.
     *     - Lado direito: exibe um bloco unificado com "Assinado Digitalmente por <nome do certificado>"
     *       seguido de uma quebra de linha e a data atual.
     *
     * A imagem de fundo (backgroundImage) é desenhada com 30% de opacidade atrás do conteúdo.
     *
     * @param g2              Contexto gráfico.
     * @param rect            Retângulo definido pelo usuário.
     * @param certificateCN   Nome comum do certificado.
     * @param backgroundImage Imagem de fundo (pode ser null).
     * @param despachoText    Texto do despacho (se existir, será desenhado na área superior).
     */
    public static void drawSignaturePreview(Graphics2D g2, Rectangle rect, String certificateCN, Image backgroundImage, String despachoText) {
        // Hints de renderização para melhor qualidade.
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
        int margin = 10; // margem em pixels
        boolean hasDespacho = (despachoText != null && !despachoText.trim().isEmpty());
        
        // Determinar se deve usar o layout alternativo:
        // a) Área sem despacho: se a altura for menor que 70.
        // b) Área com despacho: se a altura for menor que 140 (pois a área de assinatura virá a ser metade).
        if (rect.height < 70 || (hasDespacho && rect.height < 140)) {
            // ------------------------------ LAYOUT ALTERNATIVO ------------------------------
            if (hasDespacho && rect.height < 140) {
                // Variante 2: área dividida – top para despacho e bottom para assinatura alternativa.
                Rectangle dispatchArea = new Rectangle(rect.x, rect.y, rect.width, rect.height / 2);
                Rectangle signatureArea = new Rectangle(rect.x, rect.y + rect.height / 2, rect.width, rect.height - rect.height / 2);
                
                // Desenha a imagem de fundo na área de assinatura.
                if (backgroundImage != null) {
                    drawBackgroundImage(g2, signatureArea, backgroundImage);
                }
                
                // Desenha o despacho na área superior usando o layout normal.
                Shape originalClip = g2.getClip();
                g2.setClip(dispatchArea);
                FontFitResult despachoFit = chooseFontForBox(g2, despachoText, dispatchArea, "Arial", Font.PLAIN, dispatchArea.height, 10, margin);
                g2.setFont(despachoFit.font);
                g2.setColor(Color.BLACK);
                int lineHeightDespacho = despachoFit.fm.getHeight();
                int startYDespacho = dispatchArea.y + margin + despachoFit.fm.getAscent();
                for (String line : despachoFit.lines) {
                    g2.drawString(line, dispatchArea.x + margin, startYDespacho);
                    startYDespacho += lineHeightDespacho;
                }
                g2.setClip(originalClip);
                
                // Desenha a assinatura alternativa na área inferior.
                drawAlternativeSignature(g2, signatureArea, certificateCN, margin);
                
            } else {
                // Variante 1: sem despacho, toda a área é para a assinatura alternativa.
                // Desenha a imagem de fundo.
                if (backgroundImage != null) {
                    drawBackgroundImage(g2, rect, backgroundImage);
                }
                drawAlternativeSignature(g2, rect, certificateCN, margin);
            }
            
            // Desenha a borda ao redor da área completa.
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(rect);
            return; // Fim do layout alternativo.
        }
        
        // ------------------------------ LAYOUT NORMAL ------------------------------
        // (Código original: área de despacho e divisão em assinatura em duas partes, esquerda e direita.)
        int topAreaHeight = 0;
        if (hasDespacho) {
            // Define área de despacho – 50% do retângulo.
            topAreaHeight = (int) (rect.height * 0.5);
        }
        Rectangle topArea = new Rectangle(rect.x, rect.y, rect.width, topAreaHeight);
        Rectangle bottomArea = new Rectangle(rect.x, rect.y + topAreaHeight, rect.width, rect.height - topAreaHeight);
    
        // Desenha a imagem de fundo apenas na área de assinatura (bottomArea).
        if (backgroundImage != null) {
            drawBackgroundImage(g2, bottomArea, backgroundImage);
        }
        
        Shape originalClip = g2.getClip();
        // Desenha o despacho, se existir.
        if (topAreaHeight > 0) {
            g2.setClip(topArea);
            FontFitResult despachoFit = chooseFontForBox(g2, despachoText, topArea, "Arial", Font.PLAIN, topArea.height, 10, margin);
            g2.setFont(despachoFit.font);
            g2.setColor(Color.BLACK);
            int lineHeightDespacho = despachoFit.fm.getHeight();
            int startYDespacho = topArea.y + margin + despachoFit.fm.getAscent();
            for (String line : despachoFit.lines) {
                g2.drawString(line, topArea.x + margin, startYDespacho);
                startYDespacho += lineHeightDespacho;
            }
            g2.setClip(originalClip);
        }
        
        // Divide a área de assinatura em duas partes (para o layout normal).
        Rectangle leftRect = new Rectangle(bottomArea.x, bottomArea.y, bottomArea.width / 2, bottomArea.height);
        Rectangle rightRect = new Rectangle(bottomArea.x + bottomArea.width / 2, bottomArea.y,
                                            bottomArea.width - bottomArea.width / 2, bottomArea.height);
        
        // Lado esquerdo
        g2.setClip(leftRect);
        String leftText = certificateCN;
        FontFitResult leftFit = chooseFontForBox(g2, leftText, leftRect, "Arial", Font.BOLD, leftRect.height, 10, margin);
        g2.setFont(leftFit.font);
        g2.setColor(Color.BLACK);
        int leftLineHeight = leftFit.fm.getHeight();
        int totalTextHeightLeft = leftFit.lines.size() * leftLineHeight;
        int startYLeft = leftRect.y + (leftRect.height - totalTextHeightLeft) / 2 + leftFit.fm.getAscent();
        for (String line : leftFit.lines) {
            int lineWidth = leftFit.fm.stringWidth(line);
            int startXLeft = leftRect.x + (leftRect.width - lineWidth) / 2;
            g2.drawString(line, startXLeft, startYLeft);
            startYLeft += leftLineHeight;
        }
        g2.setClip(originalClip);
        
        // Lado direito
        g2.setClip(rightRect);
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String rightText = "Assinado Digitalmente por " + certificateCN + "\n" + now.format(formatter);
        FontFitResult rightFit = chooseFontForUnifiedBox(g2, rightText, rightRect, "Arial", Font.PLAIN,
                                                         (int)(rightRect.height * 0.8), 10, margin);
        g2.setFont(rightFit.font);
        g2.setColor(Color.BLACK);
        int rightLineHeight = rightFit.fm.getHeight();
        int totalTextHeightRight = rightFit.lines.size() * rightLineHeight;
        int startYRight = rightRect.y + (rightRect.height - totalTextHeightRight) / 2 + rightFit.fm.getAscent();
        for (String line : rightFit.lines) {
            int lineWidth = rightFit.fm.stringWidth(line);
            int startXRight = rightRect.x + (rightRect.width - lineWidth) / 2;
            g2.drawString(line, startXRight, startYRight);
            startYRight += rightLineHeight;
        }
        g2.setClip(originalClip);
        
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(2));
        g2.draw(rect);
    }
    
    /**
     * Desenha a imagem de fundo centralizada na área dada, com opacidade de 30%.
     */
    private static void drawBackgroundImage(Graphics2D g2, Rectangle area, Image backgroundImage) {
        int imgWidth = backgroundImage.getWidth(null);
        int imgHeight = backgroundImage.getHeight(null);
        if (imgWidth > 0 && imgHeight > 0) {
            double aspectRatio = (double) imgWidth / imgHeight;
            int drawWidth = area.width;
            int drawHeight = area.height;
            if (area.width / (double) area.height > aspectRatio) {
                drawWidth = (int) (area.height * aspectRatio);
            } else {
                drawHeight = (int) (area.width / aspectRatio);
            }
            int drawX = area.x + (area.width - drawWidth) / 2;
            int drawY = area.y + (area.height - drawHeight) / 2;
            Composite originalComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.drawImage(backgroundImage, drawX, drawY, drawWidth, drawHeight, null);
            g2.setComposite(originalComposite);
        }
    }
    
    /**
     * Desenha o layout alternativo da assinatura (três linhas) na área designada, garantindo que 
     * o tamanho da fonte seja ajustado iterativamente até que o texto caiba dentro da caixa (sem overflow).
     * Na linha 2, o nome do certificado (certificateCN) é desenhado em negrito.
     */
    private static void drawAlternativeSignature(Graphics2D g2, Rectangle area, String certificateCN, int margin) {
        int availableWidth = area.width - 2 * margin;
        int availableHeight = area.height - 2 * margin;
        // Textos a desenhar:
        String line1 = "Assinado Digitalmente";
        String line2Prefix = "Nome: ";
        String line2Suffix = certificateCN;
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String line3 = "Data: " + now.format(formatter);
        
        // Inicia com um tamanho de fonte candidato (disponível/3)
        int candidateFontSize = Math.max(10, availableHeight / 3);
        
        // Ajusta iterativamente o tamanho da fonte para garantir que cada linha caiba em availableWidth
        while (candidateFontSize > 10) {
            Font plainFont = new Font("Arial", Font.PLAIN, candidateFontSize);
            Font boldFont = new Font("Arial", Font.BOLD, candidateFontSize);
            FontMetrics fmPlain = g2.getFontMetrics(plainFont);
            FontMetrics fmBold = g2.getFontMetrics(boldFont);
            int w1 = fmPlain.stringWidth(line1);
            int w2 = fmPlain.stringWidth(line2Prefix) + fmBold.stringWidth(line2Suffix);
            int w3 = fmPlain.stringWidth(line3);
            int lineHeight = Math.max(fmPlain.getHeight(), fmBold.getHeight());
            int totalTextHeight = lineHeight * 3;
            int maxWidth = Math.max(w1, Math.max(w2, w3));
            if (maxWidth <= availableWidth && totalTextHeight <= availableHeight) {
                break;
            }
            candidateFontSize--;
        }
        
        Font plainFont = new Font("Arial", Font.PLAIN, candidateFontSize);
        Font boldFont = new Font("Arial", Font.BOLD, candidateFontSize);
        FontMetrics fmPlain = g2.getFontMetrics(plainFont);
        FontMetrics fmBold = g2.getFontMetrics(boldFont);
        int lineHeight = Math.max(fmPlain.getHeight(), fmBold.getHeight());
        int totalTextHeight = lineHeight * 3;
        int startY = area.y + margin + (availableHeight - totalTextHeight) / 2 + fmPlain.getAscent();
        
        // Linha 1 (centralizada)
        int line1Width = fmPlain.stringWidth(line1);
        int startX1 = area.x + (area.width - line1Width) / 2;
        g2.setFont(plainFont);
        g2.setColor(Color.BLACK);
        g2.drawString(line1, startX1, startY);
        startY += lineHeight;
        
        // Linha 2: "Nome: " em plain e certificateCN em bold
        int prefixWidth = fmPlain.stringWidth(line2Prefix);
        int suffixWidth = fmBold.stringWidth(line2Suffix);
        int line2Width = prefixWidth + suffixWidth;
        int startX2 = area.x + (area.width - line2Width) / 2;
        g2.setFont(plainFont);
        g2.drawString(line2Prefix, startX2, startY);
        g2.setFont(boldFont);
        g2.drawString(line2Suffix, startX2 + prefixWidth, startY);
        startY += lineHeight;
        
        // Linha 3 (centralizada)
        int line3Width = fmPlain.stringWidth(line3);
        int startX3 = area.x + (area.width - line3Width) / 2;
        g2.setFont(plainFont);
        g2.drawString(line3, startX3, startY);
    }

    public static BufferedImage createSignatureImage(String certificateCN, String despachoText, int baseWidth, int baseHeight, Image backgroundImage) throws Exception {
        // --- Configuração do scale factor para supersampling ---
        int scaleFactor = 10;
        
        // --- Primeira etapa: Medição ---
        // Criar uma imagem temporária high-res baseada no tamanho base ampliado
        int tempWidth = baseWidth * scaleFactor;
        int tempHeight = baseHeight * scaleFactor;
        BufferedImage tempImage = new BufferedImage(tempWidth, tempHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2Temp = tempImage.createGraphics();
        
        // Rendering hints para alta qualidade na imagem de alta resolução
        g2Temp.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2Temp.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2Temp.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2Temp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2Temp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Aplica escala para que o desenho trabalhe com as mesmas coordenadas lógicas
        g2Temp.scale(scaleFactor, scaleFactor);
        
        // Encapsula o Graphics2D com o MeasurementGraphics2D para medir os desenhos
        MeasurementGraphics2D measG2 = new MeasurementGraphics2D(g2Temp);
        // Em seguida, encapsula com o NoBorderGraphics2D
        NoBorderGraphics2D g2Wrapper = new NoBorderGraphics2D(measG2, new Rectangle(0, 0, baseWidth, baseHeight));
        
        // Chama o método de desenho do preview usando o retângulo base
        drawSignaturePreview(g2Wrapper, new Rectangle(0, 0, baseWidth, baseHeight), certificateCN, backgroundImage, despachoText);
        
        // Libera recursos
        g2Wrapper.dispose();
        g2Temp.dispose();
        
        // Recupera as medidas máximas registradas (em unidades lógicas)
        int measuredWidth = (int) Math.ceil(measG2.getMaxX());
        int measuredHeight = (int) Math.ceil(measG2.getMaxY());
        // Garante que não fiquem menores que os valores base
        int marginExtra = 20;
        measuredWidth = Math.max(measuredWidth, baseWidth) + marginExtra;
        measuredHeight = Math.max(measuredHeight, baseHeight) + marginExtra;
        
        // --- Segunda etapa: Desenho final ---
        // Calcula as dimensões finais high-res multiplicando as medidas lógicas pelo scale factor
        int finalHighResWidth = measuredWidth * scaleFactor;
        int finalHighResHeight = measuredHeight * scaleFactor;
        
        // Cria a imagem high-res final com as novas dimensões
        BufferedImage highResFinalImage = new BufferedImage(finalHighResWidth, finalHighResHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2HighFinal = highResFinalImage.createGraphics();
        g2HighFinal.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2HighFinal.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2HighFinal.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2HighFinal.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2HighFinal.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Aplica novamente o scaling para trabalhar em coordenadas lógicas
        g2HighFinal.scale(scaleFactor, scaleFactor);
        
        // Define o retângulo final lógico com as dimensões medidas
        Rectangle finalRect = new Rectangle(0, 0, measuredWidth, measuredHeight);
        NoBorderGraphics2D g2FinalWrapper = new NoBorderGraphics2D(g2HighFinal, finalRect);
        
        // Desenha novamente o conteúdo final na imagem high-res
        drawSignaturePreview(g2FinalWrapper, finalRect, certificateCN, backgroundImage, despachoText);
        
        g2FinalWrapper.dispose();
        g2HighFinal.dispose();
        
        // Cria a imagem final na resolução desejada (downsample a imagem high-res)
        BufferedImage finalImage = new BufferedImage(measuredWidth, measuredHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2Final = finalImage.createGraphics();
        // Use uma interpolação bilinear para suavizar o resultado
        g2Final.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2Final.drawImage(highResFinalImage, 0, 0, measuredWidth, measuredHeight, null);
        g2Final.dispose();
        
        return finalImage;
    }

    public static class MeasurementGraphics2D extends Graphics2D {
        private final Graphics2D delegate;
        private float maxX = 0;
        private float maxY = 0;
        
        public MeasurementGraphics2D(Graphics2D delegate) {
            this.delegate = delegate;
        }
        
        public float getMaxX() {
            return maxX;
        }
        
        public float getMaxY() {
            return maxY;
        }
        
        // Intercepta os métodos de desenho de string para medir a área usada.
        @Override
        public void drawString(String str, float x, float y) {
            Font font = delegate.getFont();
            FontRenderContext frc = delegate.getFontRenderContext();
            Rectangle2D bounds = font.getStringBounds(str, frc);
            float drawnX = x + (float) bounds.getWidth();
            FontMetrics fm = delegate.getFontMetrics(font);
            float drawnY = y + fm.getDescent();
            
            if (drawnX > maxX) {
                maxX = drawnX;
            }
            if (drawnY > maxY) {
                maxY = drawnY;
            }
            delegate.drawString(str, x, y);
        }
        
        @Override
        public void drawString(String str, int x, int y) {
            drawString(str, (float)x, (float)y);
        }
        
        @Override
        public void drawString(AttributedCharacterIterator iterator, float x, float y) {
            StringBuilder sb = new StringBuilder();
            for (char c = iterator.first(); c != AttributedCharacterIterator.DONE; c = iterator.next()) {
                sb.append(c);
            }
            drawString(sb.toString(), x, y);
        }
        
        @Override
        public void drawString(AttributedCharacterIterator iterator, int x, int y) {
            drawString(iterator, (float)x, (float)y);
        }
        
        // Outros métodos abstratos de Graphics e Graphics2D são implementados por delegação:

        @Override
        public void draw(Shape s) {
            delegate.draw(s);
        }

        @Override
        public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
            return delegate.drawImage(img, xform, obs);
        }

        @Override
        public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
            delegate.drawImage(img, op, x, y);
        }

        @Override
        public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
            delegate.drawRenderedImage(img, xform);
        }

        @Override
        public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
            delegate.drawRenderableImage(img, xform);
        }

        @Override
        public void drawGlyphVector(java.awt.font.GlyphVector g, float x, float y) {
            delegate.drawGlyphVector(g, x, y);
        }

        @Override
        public void fill(Shape s) {
            delegate.fill(s);
        }

        @Override
        public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
            return delegate.hit(rect, s, onStroke);
        }

        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return delegate.getDeviceConfiguration();
        }

        @Override
        public void setComposite(Composite comp) {
            delegate.setComposite(comp);
        }

        @Override
        public void setPaint(Paint paint) {
            delegate.setPaint(paint);
        }

        @Override
        public void setStroke(Stroke s) {
            delegate.setStroke(s);
        }
        
        @Override
        public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
            delegate.setRenderingHint(hintKey, hintValue);
        }

        @Override
        public Object getRenderingHint(RenderingHints.Key hintKey) {
            return delegate.getRenderingHint(hintKey);
        }

        @Override
        public void setRenderingHints(Map<?, ?> hints) {
            delegate.setRenderingHints(hints);
        }

        @Override
        public void addRenderingHints(Map<?, ?> hints) {
            delegate.addRenderingHints(hints);
        }

        @Override
        public RenderingHints getRenderingHints() {
            return delegate.getRenderingHints();
        }

        @Override
        public void translate(int x, int y) {
            delegate.translate(x, y);
        }
        
        @Override
        public void translate(double tx, double ty) {
            delegate.translate(tx, ty);
        }

        @Override
        public void rotate(double theta) {
            delegate.rotate(theta);
        }

        @Override
        public void rotate(double theta, double x, double y) {
            delegate.rotate(theta, x, y);
        }

        @Override
        public void scale(double sx, double sy) {
            delegate.scale(sx, sy);
        }
        
        @Override
        public void shear(double shx, double shy) {
            delegate.shear(shx, shy);
        }
        
        @Override
        public void transform(AffineTransform Tx) {
            delegate.transform(Tx);
        }

        @Override
        public void setTransform(AffineTransform Tx) {
            delegate.setTransform(Tx);
        }

        @Override
        public AffineTransform getTransform() {
            return delegate.getTransform();
        }

        @Override
        public Paint getPaint() {
            return delegate.getPaint();
        }

        @Override
        public Composite getComposite() {
            return delegate.getComposite();
        }

        @Override
        public void setBackground(Color color) {
            delegate.setBackground(color);
        }

        @Override
        public Color getBackground() {
            return delegate.getBackground();
        }
        
        @Override
        public Stroke getStroke() {
            return delegate.getStroke();
        }

        @Override
        public void clip(Shape s) {
            delegate.clip(s);
        }
        
        @Override
        public FontRenderContext getFontRenderContext() {
            return delegate.getFontRenderContext();
        }

        @Override
        public void drawLine(int x1, int y1, int x2, int y2) {
            delegate.drawLine(x1, y1, x2, y2);
        }

        @Override
        public void setColor(Color c) {
            delegate.setColor(c);
        }

        @Override
        public Color getColor() {
            return delegate.getColor();
        }

        @Override
        public void setFont(Font font) {
            delegate.setFont(font);
        }

        @Override
        public Font getFont() {
            return delegate.getFont();
        }

        @Override
        public FontMetrics getFontMetrics(Font f) {
            return delegate.getFontMetrics(f);
        }

        @Override
        public void setXORMode(Color c1) {
            delegate.setXORMode(c1);
        }

        @Override
        public void setPaintMode() {
            delegate.setPaintMode();
        }

        @Override
        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.fillArc(x, y, width, height, startAngle, arcAngle);
        }

        @Override
        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.drawArc(x, y, width, height, startAngle, arcAngle);
        }

        @Override
        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.fillPolygon(xPoints, yPoints, nPoints);
        }

        @Override
        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolygon(xPoints, yPoints, nPoints);
        }
        
        // Métodos herdados de Graphics que não alterei
        @Override
        public Graphics create() {
            return delegate.create();
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }

        @Override
        public void clearRect(int x, int y, int width, int height) {
            delegate.clearRect(x, y, width, height);
        }

        @Override
        public void clipRect(int x, int y, int width, int height) {
            delegate.clipRect(x, y, width, height);
        }

        @Override
        public void copyArea(int x, int y, int width, int height, int dx, int dy) {
            delegate.copyArea(x, y, width, height, dx, dy);
        }

        @Override
        public void drawOval(int x, int y, int width, int height) {
            delegate.drawOval(x, y, width, height);
        }

        @Override
        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolyline(xPoints, yPoints, nPoints);
        }

        @Override
        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }

        @Override
        public void fillOval(int x, int y, int width, int height) {
            delegate.fillOval(x, y, width, height);
        }

        @Override
        public void fillRect(int x, int y, int width, int height) {
            delegate.fillRect(x, y, width, height);
        }

        @Override
        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
        
        @Override
        public Shape getClip() {
            return delegate.getClip();
        }
        
        @Override
        public Rectangle getClipBounds() {
            return delegate.getClipBounds();
        }
        
        @Override
        public void setClip(Shape clip) {
            delegate.setClip(clip);
        }
        
        @Override
        public void setClip(int x, int y, int width, int height) {
            delegate.setClip(x, y, width, height);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
            return delegate.drawImage(img, x, y, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
            return delegate.drawImage(img, x, y, bgcolor, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
        }
        
        @Override
        public boolean drawImage(Image img,
                                int dx1,
                                int dy1,
                                int dx2,
                                int dy2,
                                int sx1,
                                int sy1,
                                int sx2,
                                int sy2,
                                ImageObserver observer) {
            return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        }
        
        @Override
        public boolean drawImage(Image img,
                                int dx1,
                                int dy1,
                                int dx2,
                                int dy2,
                                int sx1,
                                int sy1,
                                int sx2,
                                int sy2,
                                Color bgcolor,
                                ImageObserver observer) {
            return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
        }
    }
    
    /**
     * Um wrapper para Graphics2D que ignora chamadas de desenho de uma forma específica.
     * Ignora a chamada para desenhar a borda azul,
     */
    private static class NoBorderGraphics2D extends Graphics2D {
        private final Graphics2D delegate;
        private final Shape skipShape;
    
        public NoBorderGraphics2D(Graphics2D delegate, Shape skipShape) {
            this.delegate = delegate;
            this.skipShape = skipShape;
        }
    
        @Override
        public void draw(Shape s) {
            // Se o shape é igual ao que quero ignorar (a borda azul), não faz nada.
            if (s != null && s.equals(skipShape)) {
                return;
            }
            delegate.draw(s);
        }
    
        // Métodos obrigatórios que delegam para o objeto original.
        @Override
        public void addRenderingHints(Map<?, ?> hints) {
            delegate.addRenderingHints(hints);
        }
        
        @Override
        public void clip(Shape s) {
            delegate.clip(s);
        }
        
        @Override
        public void drawGlyphVector(GlyphVector g, float x, float y) {
            delegate.drawGlyphVector(g, x, y);
        }
        
        @Override
        public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
            return delegate.drawImage(img, xform, obs);
        }
        
        @Override
        public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
            delegate.drawImage(img, op, x, y);
        }
        
        @Override
        public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
            delegate.drawRenderableImage(img, xform);
        }
        
        @Override
        public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
            delegate.drawRenderedImage(img, xform);
        }
        
        @Override
        public void fill(Shape s) {
            delegate.fill(s);
        }
        
        @Override
        public Color getBackground() {
            return delegate.getBackground();
        }
        
        @Override
        public Composite getComposite() {
            return delegate.getComposite();
        }
        
        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return delegate.getDeviceConfiguration();
        }
        
        @Override
        public FontRenderContext getFontRenderContext() {
            return delegate.getFontRenderContext();
        }
        
        @Override
        public Paint getPaint() {
            return delegate.getPaint();
        }
        
        @Override
        public Object getRenderingHint(RenderingHints.Key hintKey) {
            return delegate.getRenderingHint(hintKey);
        }
        
        @Override
        public RenderingHints getRenderingHints() {
            return delegate.getRenderingHints();
        }
        
        @Override
        public Stroke getStroke() {
            return delegate.getStroke();
        }
        
        @Override
        public AffineTransform getTransform() {
            return delegate.getTransform();
        }
        
        @Override
        public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
            return delegate.hit(rect, s, onStroke);
        }
        
        @Override
        public void rotate(double theta) {
            delegate.rotate(theta);
        }
        
        @Override
        public void rotate(double theta, double x, double y) {
            delegate.rotate(theta, x, y);
        }
        
        @Override
        public void scale(double sx, double sy) {
            delegate.scale(sx, sy);
        }
        
        @Override
        public void setBackground(Color color) {
            delegate.setBackground(color);
        }
        
        @Override
        public void setComposite(Composite comp) {
            delegate.setComposite(comp);
        }
        
        @Override
        public void setPaint(Paint paint) {
            delegate.setPaint(paint);
        }
        
        @Override
        public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
            delegate.setRenderingHint(hintKey, hintValue);
        }
        
        @Override
        public void setRenderingHints(Map<?, ?> hints) {
            delegate.setRenderingHints(hints);
        }
        
        @Override
        public void setStroke(Stroke s) {
            delegate.setStroke(s);
        }
        
        @Override
        public void setTransform(AffineTransform Tx) {
            delegate.setTransform(Tx);
        }
        
        @Override
        public void shear(double shx, double shy) {
            delegate.shear(shx, shy);
        }
        
        @Override
        public void transform(AffineTransform Tx) {
            delegate.transform(Tx);
        }
        
        @Override
        public void translate(int x, int y) {
            delegate.translate(x, y);
        }
        
        @Override
        public void translate(double x, double y) {
            delegate.translate(x, y);
        }
        
        @Override
        public Graphics create() {
            return new NoBorderGraphics2D((Graphics2D) delegate.create(), skipShape);
        }
        
        @Override
        public void dispose() {
            delegate.dispose();
        }
        
        @Override
        public Color getColor() {
            return delegate.getColor();
        }
        
        @Override
        public Font getFont() {
            return delegate.getFont();
        }
        
        @Override
        public void setColor(Color c) {
            delegate.setColor(c);
        }
        
        @Override
        public void setFont(Font font) {
            delegate.setFont(font);
        }
        
        @Override
        public FontMetrics getFontMetrics(Font f) {
            return delegate.getFontMetrics(f);
        }
        
        @Override
        public Rectangle getClipBounds() {
            return delegate.getClipBounds();
        }
        
        @Override
        public void clipRect(int x, int y, int width, int height) {
            delegate.clipRect(x, y, width, height);
        }
        
        @Override
        public void setClip(int x, int y, int width, int height) {
            delegate.setClip(x, y, width, height);
        }
        
        @Override
        public Shape getClip() {
            return delegate.getClip();
        }
        
        @Override
        public void setClip(Shape clip) {
            delegate.setClip(clip);
        }
        
        @Override
        public void copyArea(int x, int y, int width, int height, int dx, int dy) {
            delegate.copyArea(x, y, width, height, dx, dy);
        }
        
        @Override
        public void drawLine(int x1, int y1, int x2, int y2) {
            delegate.drawLine(x1, y1, x2, y2);
        }
        
        @Override
        public void fillRect(int x, int y, int width, int height) {
            delegate.fillRect(x, y, width, height);
        }
        
        @Override
        public void clearRect(int x, int y, int width, int height) {
            delegate.clearRect(x, y, width, height);
        }
        
        @Override
        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
        
        @Override
        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
        
        @Override
        public void drawOval(int x, int y, int width, int height) {
            delegate.drawOval(x, y, width, height);
        }
        
        @Override
        public void fillOval(int x, int y, int width, int height) {
            delegate.fillOval(x, y, width, height);
        }
        
        @Override
        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.drawArc(x, y, width, height, startAngle, arcAngle);
        }
        
        @Override
        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            delegate.fillArc(x, y, width, height, startAngle, arcAngle);
        }
        
        @Override
        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolyline(xPoints, yPoints, nPoints);
        }
        
        @Override
        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.drawPolygon(xPoints, yPoints, nPoints);
        }
        
        @Override
        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            delegate.fillPolygon(xPoints, yPoints, nPoints);
        }
        
        @Override
        public void setXORMode(Color c) {
            delegate.setXORMode(c);
        }
        
        @Override
        public void setPaintMode() {
            delegate.setPaintMode();
        }
        
        @Override
        public void drawString(AttributedCharacterIterator iterator, float x, float y) {
            delegate.drawString(iterator, x, y);
        }
        
        @Override
        public void drawString(AttributedCharacterIterator iterator, int x, int y) {
            delegate.drawString(iterator, x, y);
        }
        
        @Override
        public void drawString(String s, float x, float y) {
            delegate.drawString(s, x, y);
        }
        
        @Override
        public void drawString(String s, int x, int y) {
            delegate.drawString(s, x, y);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height,
                                 int sx, int sy, int sw, int sh, Color bgcolor, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, sx, sy, sw, sh, bgcolor, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height,
                                 int sx, int sy, int sw, int sh, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, sx, sy, sw, sh, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height,
                                 Color bgcolor, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
            return delegate.drawImage(img, x, y, bgcolor, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
            return delegate.drawImage(img, x, y, width, height, observer);
        }
        
        @Override
        public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
            return delegate.drawImage(img, x, y, observer);
        }
        
    }


    /**
     * Desenha a pré-visualização "vazia" da assinatura, para os casos em que não há dados do certificado.
     * Se houver despacho, a parte superior (60%) exibirá o texto do despacho e a parte inferior (40%)
     * exibirá a imagem digitalSigned.png; se não houver despacho, a imagem ocupa todo o retângulo.
     *
     * @param g2           Contexto gráfico.
     * @param rect         Retângulo definido.
     * @param despachoText Texto do despacho (pode estar vazio ou nulo).
     */
    public static void drawEmptySignaturePreview(Graphics2D g2, Image cnImage, Rectangle rect, String despachoText) {
        int margin = 10; // margem em pixels

        // Se houver despacho, reserva 60% para o texto; se não, todo o espaço fica para a imagem.
        int topAreaHeight = 0;
        if (despachoText != null && !despachoText.trim().isEmpty()) {
            topAreaHeight = (int) (rect.height * 0.6);
        }
        Rectangle topArea = new Rectangle(rect.x, rect.y, rect.width, topAreaHeight);
        Rectangle bottomArea = new Rectangle(rect.x, rect.y + topAreaHeight, rect.width, rect.height - topAreaHeight);

        Shape originalClip = g2.getClip();

        // Se houver texto no despacho, desenha a área superior
        if (topAreaHeight > 0) {
            g2.setClip(topArea);
            FontFitResult despachoFit = chooseFontForBox(g2, despachoText, topArea, "Arial", Font.PLAIN,
                                                          topArea.height, 10, margin);
            g2.setFont(despachoFit.font);
            g2.setColor(Color.BLACK);
            int lineHeight = despachoFit.fm.getHeight();
            int startY = topArea.y + margin + despachoFit.fm.getAscent();
            for (String line : despachoFit.lines) {
                g2.drawString(line, topArea.x + margin, startY);
                startY += lineHeight;
            }
            g2.setClip(originalClip);
        }

        // Desenha a imagem digitalSigned.png na área inferior (ou no retângulo todo se não houver despacho)
        // Tenta carregar a imagem digitalSigned.png (a partir da raiz do classpath)
        Image digitalSignedImage = null;
        try {
            digitalSignedImage = cnImage;
        } catch (Exception e) {
            System.err.println("Erro ao carregar digitalSigned.png: " + e.getMessage());
        }
        if (digitalSignedImage != null) {
            int imgWidth = digitalSignedImage.getWidth(null);
            int imgHeight = digitalSignedImage.getHeight(null);
            if (imgWidth > 0 && imgHeight > 0) {
                double aspectRatio = (double) imgWidth / imgHeight;
                int drawWidth = bottomArea.width;
                int drawHeight = bottomArea.height;
                if (bottomArea.width / (double) bottomArea.height > aspectRatio) {
                    drawWidth = (int) (bottomArea.height * aspectRatio);
                } else {
                    drawHeight = (int) (bottomArea.width / aspectRatio);
                }
                int drawX = bottomArea.x + (bottomArea.width - drawWidth) / 2;
                int drawY = bottomArea.y + (bottomArea.height - drawHeight) / 2;
                g2.drawImage(digitalSignedImage, drawX, drawY, drawWidth, drawHeight, null);
            }
        } else {
            // Se não carregar a imagem, preenche a área com uma cor
            g2.setColor(Color.LIGHT_GRAY);
            g2.fill(bottomArea);
        }

        // Restaura o clip e desenha a borda do retângulo
        g2.setClip(originalClip);
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(2));
        g2.draw(rect);
    }
}