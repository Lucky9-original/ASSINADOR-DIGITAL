package com.meuempresa.assinador.gui;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.geom.Rectangle2D;
import java.io.IOException;


public class PDFPreviewPanel extends JPanel {
    private PDDocument currentDocument;
    private int currentPage = 1;
    private int totalPages = 0;

    // Dados reais do certificado, será usado para pré‑visualização
    private String certificateCN = "";

    // Components para renderização
    private JLayeredPane layeredPane;
    private ImagePanel imagePanel;
    private SelectionOverlay overlay;

    private Rectangle2D.Float normalizedSignatureRect = null;
    private Rectangle absoluteSignatureRect = null;

    // === CALLBACKS ===
    public static interface SignatureLocationListener {
        void onSignatureSelected(Rectangle rect);
    }
    private SignatureLocationListener signatureLocationListener;
    public void setSignatureLocationListener(SignatureLocationListener listener) {
        this.signatureLocationListener = listener;
    }
    
    public static interface PageChangeListener {
        void onPageChanged(int newPage);
    }
    private PageChangeListener pageChangeListener;
    public void setPageChangeListener(PageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    private String despachoText = ""; // campo para armazenar o texto do despacho.

    public void setDespachoText(String text) {
        this.despachoText = text;
        overlay.repaint();
    }
    
    public String getDespachoText() {
        return despachoText;
    }

    public PDFPreviewPanel() {
        setLayout(new BorderLayout());
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        imagePanel = new ImagePanel();
        overlay = new SelectionOverlay();

        layeredPane.add(imagePanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(overlay, JLayeredPane.PALETTE_LAYER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (layeredPane == null || imagePanel == null || overlay == null) {
                    return;
                }
                layeredPane.setBounds(0, 0, getWidth(), getHeight());
                imagePanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                overlay.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                if (currentDocument != null) {
                    updatePreview();
                }
            }
        });
    }

    private Rectangle2D.Float convertToNormalized(Rectangle rect) {
        if (currentDocument != null) {
            // Obtém o mediaBox da página atual
            org.apache.pdfbox.pdmodel.PDPage page = currentDocument.getPage(currentPage - 1);
            org.apache.pdfbox.pdmodel.common.PDRectangle mediaBox = page.getMediaBox();
            float pdfWidth = mediaBox.getWidth();
            float pdfHeight = mediaBox.getHeight();
            
            // Converte as coordenadas absolutas para normalizadas
            float normX = (float) rect.x / pdfWidth;
            float normW = (float) rect.width / pdfWidth;
            // Ajusta Y invertendo a origem (PDF usa o canto inferior-esquerdo)
            float normY = 1f - (((float) rect.y + rect.height) / pdfHeight);
            float normH = (float) rect.height / pdfHeight;
            
            return new Rectangle2D.Float(normX, normY, normW, normH);
        }
        return null;
    }
    
    /**
     * Define o nome do certificado e atualiza o overlay.
     */
    public void setCertificateCN(String certificateCN) {
        this.certificateCN = certificateCN;
        overlay.repaint();
    }
    
    /**
     * Define o retângulo da assinatura que será exibido (no overlay).
     */
    public void setSignatureRectangle(Rectangle rect) {
        if (rect != null) {
            // Armazena o retângulo absoluto
            absoluteSignatureRect = rect;
            // Converte a partir do retângulo absoluto
            normalizedSignatureRect = convertToNormalized(absoluteSignatureRect);
            System.out.println("Normalized rect atualizado: " + normalizedSignatureRect);
        } else {
            absoluteSignatureRect = null;
            normalizedSignatureRect = null;
        }
        repaint();
    }

    public void refreshSignatureRectangle() {
        if (absoluteSignatureRect != null) {
            normalizedSignatureRect = convertToNormalized(absoluteSignatureRect);
            repaint();
        }
    }

    public Rectangle convertViewToAbsolute(Rectangle viewRect) {
        // Obtém os limites onde a imagem é renderizada
        Rectangle imageBounds = imagePanel.getImageRenderBounds();
        
        // Supondo que o retângulo viewRect esteja posicionado relativo a esses bounds,
        // calcula-se as proporções
        float relativeX = (float)(viewRect.x - imageBounds.x) / imageBounds.width;
        float relativeY = (float)(viewRect.y - imageBounds.y) / imageBounds.height;
        float relativeW = (float)viewRect.width / imageBounds.width;
        float relativeH = (float)viewRect.height / imageBounds.height;
        
        // Converte essas proporções para as coordenadas absolutas usando as dimensões do mediaBox
        org.apache.pdfbox.pdmodel.PDPage page = currentDocument.getPage(currentPage - 1);
        org.apache.pdfbox.pdmodel.common.PDRectangle mediaBox = page.getMediaBox();
        int absX = Math.round(relativeX * mediaBox.getWidth());
        // No PDF, a origem é no canto inferior-esquerdo,
        // por isso considera-se que absY deve ser calculado de forma inversa.
        int absY = Math.round((1 - relativeY - relativeH) * mediaBox.getHeight());
        int absW = Math.round(relativeW * mediaBox.getWidth());
        int absH = Math.round(relativeH * mediaBox.getHeight());
        
        return new Rectangle(absX, absY, absW, absH);
    }
    
    /**
     * Retorna a seleção atual obtida do overlay.
     */
    public Rectangle getSignatureRectangle() {
        return overlay.getSelectionRectangle();
    }
    
    /**
     * Carrega o arquivo PDF e atualiza o número de páginas.
     */
    public void loadDocument(File pdfFile, boolean isToggleLastPageSelected) {
        try {
            if (currentDocument != null) {
                currentDocument.close();
            }
            currentDocument = PDDocument.load(pdfFile);
            totalPages = currentDocument.getNumberOfPages();
            currentPage = isToggleLastPageSelected ? totalPages : 1;
            SwingUtilities.invokeLater(this::updatePreview);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar o PDF: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Renderiza a página atual do PDF, escalando a imagem para caber na altura do painel.
     */
    public void updatePreview() {
        if (currentDocument == null) return;
        try {
            PDFRenderer renderer = new PDFRenderer(currentDocument);
            BufferedImage image = renderer.renderImageWithDPI(currentPage - 1, 100);
            int panelHeight = getHeight();
            if (panelHeight <= 0) {
                panelHeight = 600;
            }
            int origWidth = image.getWidth();
            int origHeight = image.getHeight();
            float scaleFactor = (float) panelHeight / origHeight;
            int scaledWidth = Math.round(origWidth * scaleFactor);
            Image scaledImage = image.getScaledInstance(scaledWidth, panelHeight, Image.SCALE_SMOOTH);
            imagePanel.setImage(scaledImage);
            imagePanel.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao atualizar a pré-visualização: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void nextPage() {
        if (currentDocument != null && currentPage < totalPages) {
            currentPage++;
            if (absoluteSignatureRect != null) {
                normalizedSignatureRect = convertToNormalized(absoluteSignatureRect);
            }
            updatePreview();
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(currentPage);
            }
        }
    }
    
    public void previousPage() {
        if (currentDocument != null && currentPage > 1) {
            currentPage--;
            if (absoluteSignatureRect != null) {
                normalizedSignatureRect = convertToNormalized(absoluteSignatureRect);
            }
            updatePreview();
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(currentPage);
            }
        }
    }
    
    public void toggleLastPage(boolean selected) {
        if (currentDocument != null) {
            currentPage = selected ? totalPages : 1;
            updatePreview();
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(currentPage);
            }
        }
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }

    public void goToPage(int pageNumber) {
        if (pageNumber >= 1 && pageNumber <= getTotalPages()) {
            currentPage = pageNumber;
            updatePreview();
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(currentPage);
            }
        }
    }
    
    /**
     * Painel interno responsável por desenhar a imagem do PDF de forma centralizada.
     */
    private class ImagePanel extends JPanel {
        private Image image;
        
        public void setImage(Image image) {
            this.image = image;
        }
        
        /**
         * Retorna os limites onde a imagem é desenhada, centralizada no painel.
         */
        public Rectangle getImageRenderBounds() {
            if (image == null) return new Rectangle();
            int imgWidth = image.getWidth(this);
            int imgHeight = image.getHeight(this);
            int x = (getWidth() - imgWidth) / 2;
            int y = (getHeight() - imgHeight) / 2;
            return new Rectangle(x, y, imgWidth, imgHeight);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                Rectangle bounds = getImageRenderBounds();
                g.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, this);
            }
        }
    }
    
    /**
     * Componente transparente que captura os eventos do mouse e, ao concluir a seleção,
     * desenha a pré-visualização da assinatura. Ao finalizar o desenho (mouseReleased),
     * o callback é chamado imediatamente para atualizar a cache.
     */
    private class SelectionOverlay extends JComponent implements MouseListener, MouseMotionListener {
        private Point startPoint = null;
        private Rectangle selectionRect = null;
        private boolean previewActive = false;
        private Image signaturePreviewImage;
        private Image cnPreviewImage;
        private final int MIN_SELECTION_WIDTH = 70;
        private final int MIN_SELECTION_HEIGHT = 30;

        
        public SelectionOverlay() {
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
            try {
                signaturePreviewImage = ImageIO.read(getClass().getClassLoader().getResource("marca.png"));
            } catch (Exception ex) {
                ex.printStackTrace();
                signaturePreviewImage = null;
            }
        }
        
        public Rectangle getSelectionRectangle() {
            return selectionRect;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            
            if (normalizedSignatureRect != null) {
                System.out.println("Dentro do paintComponent " + normalizedSignatureRect);
                Rectangle imageBounds = imagePanel.getImageRenderBounds();
                
                int drawX = imageBounds.x + Math.round(normalizedSignatureRect.x * imageBounds.width);
                int drawY = imageBounds.y + Math.round(normalizedSignatureRect.y * imageBounds.height);
                int drawW = Math.round(normalizedSignatureRect.width * imageBounds.width);
                int drawH = Math.round(normalizedSignatureRect.height * imageBounds.height);
                
                Rectangle updatedRect = new Rectangle(drawX, drawY, drawW, drawH);

                System.out.println("Desenhando a assinatura em: " + updatedRect);
                
                // Desenhar a assinatura usando o retângulo recalculado
                if (previewActive && certificateCN != null && !certificateCN.trim().isEmpty() && signaturePreviewImage != null) {
                    Signature.drawSignaturePreview(g2, updatedRect, certificateCN, signaturePreviewImage, despachoText);
                } else {
                    try {
                        cnPreviewImage = ImageIO.read(getClass().getClassLoader().getResource("digitalSigned.png"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        cnPreviewImage = null;
                    }
                    Signature.drawEmptySignaturePreview(g2, cnPreviewImage, updatedRect, despachoText);
                }
            }
            g2.dispose();
        }
        
        private boolean isPointInImage(Point p) {
            Rectangle imageBounds = imagePanel.getImageRenderBounds();
            return imageBounds.contains(p);
        }
        
        private Point clampToImage(Point p) {
            Rectangle imageBounds = imagePanel.getImageRenderBounds();
            int x = Math.min(Math.max(p.x, imageBounds.x), imageBounds.x + imageBounds.width);
            int y = Math.min(Math.max(p.y, imageBounds.y), imageBounds.y + imageBounds.height);
            return new Point(x, y);
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (!isPointInImage(e.getPoint())) {
                startPoint = null;
                return;
            }
            startPoint = clampToImage(e.getPoint());
            selectionRect = new Rectangle(startPoint);
            previewActive = false;
            repaint();
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (startPoint == null) return;
            Point currentPoint = clampToImage(e.getPoint());
            selectionRect.setBounds(
                    Math.min(startPoint.x, currentPoint.x),
                    Math.min(startPoint.y, currentPoint.y),
                    Math.abs(startPoint.x - currentPoint.x),
                    Math.abs(startPoint.y - currentPoint.y)
            );
            repaint();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (startPoint == null) return;
            Point endPoint = clampToImage(e.getPoint());
            selectionRect.setBounds(
                Math.min(startPoint.x, endPoint.x),
                Math.min(startPoint.y, endPoint.y),
                Math.abs(startPoint.x - endPoint.x),
                Math.abs(startPoint.y - endPoint.y)
            );
            if (selectionRect.width < MIN_SELECTION_WIDTH || selectionRect.height < MIN_SELECTION_HEIGHT) {
                selectionRect = null;
                previewActive = false;
                normalizedSignatureRect = null;
                JOptionPane.showMessageDialog(this, "Assinatura demasiado pequena!");
                repaint();
                return;
            }
            
            // Obtém os limites atuais onde o PDF está sendo renderizado
            Rectangle imageBounds = imagePanel.getImageRenderBounds();
            if (imageBounds.width > 0 && imageBounds.height > 0) {
                // Calcula os valores relativos (normalizados) da seleção
                float normX = (float) (selectionRect.x - imageBounds.x) / imageBounds.width;
                float normY = (float) (selectionRect.y - imageBounds.y) / imageBounds.height;
                float normW = (float) selectionRect.width / imageBounds.width;
                float normH = (float) selectionRect.height / imageBounds.height;
                
                // Armazena o retângulo normalizado
                normalizedSignatureRect = new Rectangle2D.Float(normX, normY, normW, normH);
            }
            
            previewActive = true;
            repaint();
            
            if (currentDocument != null && normalizedSignatureRect != null) {
                // Obter a página atual para saber as dimensões reais (em pontos) do PDF
                org.apache.pdfbox.pdmodel.PDPage page = currentDocument.getPage(currentPage - 1);
                org.apache.pdfbox.pdmodel.common.PDRectangle mediaBox = page.getMediaBox();
                float pdfWidth = mediaBox.getWidth();
                float pdfHeight = mediaBox.getHeight();
                
                // Converte os valores normalizados para as coordenadas do PDF:
                int pdfX = Math.round(normalizedSignatureRect.x * pdfWidth);
                int pdfW = Math.round(normalizedSignatureRect.width * pdfWidth);
                int pdfH = Math.round(normalizedSignatureRect.height * pdfHeight);
                // Como o sistema do PDF possui a origem no canto inferior esquerdo, 
                // devemos inverter o eixo Y:
                int pdfY = Math.round(pdfHeight - ((normalizedSignatureRect.y + normalizedSignatureRect.height) * pdfHeight));
                
                Rectangle pdfRect = new Rectangle(pdfX, pdfY, pdfW, pdfH);
                
                if (signatureLocationListener != null) {
                    signatureLocationListener.onSignatureSelected(pdfRect);
                }
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            if (isPointInImage(e.getPoint())) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }
        
        @Override public void mouseClicked(MouseEvent e) { }
        @Override public void mouseEntered(MouseEvent e) { }
        @Override public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Fecha o documento atual (liberta o lock no ficheiro) e limpa a preview.
     */
    public void clearDocument() {
        // 1) Fecha o documento PDF se estiver aberto
        if (currentDocument != null) {
            try {
                currentDocument.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                currentDocument = null;
            }
        }
        // 2) Remove a imagem e todos os retângulos de assinatura
        imagePanel.setImage(null);
        normalizedSignatureRect = null;
        absoluteSignatureRect = null;
        // 3) Reseta informações de navegação
        totalPages = 0;
        currentPage = 0;
        // 4) Força o repaint para limpar a UI
        repaint();
    }

}