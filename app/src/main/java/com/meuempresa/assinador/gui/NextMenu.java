package com.meuempresa.assinador.gui;

import com.meuempresa.assinador.data.PDFCache;
import com.meuempresa.assinador.data.PDFInfo;
import com.meuempresa.assinador.signer.DocumentSigner;
import com.meuempresa.assinador.signer.DocumentSignerLocal;
import com.meuempresa.assinador.util.CertificateUtils;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jnafilechooser.api.JnaFileChooser;

public class NextMenu extends JFrame implements FileListPanel.FileListListener {
    // Painéis e controlos do lado direito e gerais
    private JPanel rightPanel;
    private JButton btnAdicionarPDF;
    private JButton btnAssinarOutraForma;
    private JButton btnAssinar;
    private JButton btnPrev;
    private JButton btnNext;
    private JLabel lblPage;
    private JToggleButton toggleLastPage;
    private JButton btnLimparTudo; // NOVO
    
    // Gestão de PDFs
    private File currentPDFFile;
    private List<File> pdfFiles = new ArrayList<>();
    private FileListPanel fileListPanel;
    
    // O painel esquerdo será baseado em CardLayout para trocar entre “Escolher PDF” e “Visualizar PDF”
    private JPanel leftPanelContainer;
    private CardLayout leftCardLayout;
    private JButton btnEscolherPDF;
    private PDFPreviewPanel pdfPreviewPanel;

    // Nosso "cache" simples para armazenar informações de cada PDF
    private final PDFCache pdfCache = new PDFCache();

    // Para as opções 3 e 4, armazenamos uma assinatura global
    private Rectangle globalSignatureRect = null;
    private int globalSignaturePage = 1;

    private JComboBox<String> comboCertificados;
    
    // Opção recebida do menu anterior (1 a 6)
    private int option;

    // Método auxiliar para padronizar o tamanho dos botões
    private void uniformButtonSize(JButton... buttons) {
        Dimension size = new Dimension(220, 36); // largura, altura
        for (JButton btn : buttons) {
            btn.setMaximumSize(size);
            btn.setPreferredSize(size);
            btn.setMinimumSize(size);
        }
    }

    // Botão com gradiente verde claro
    private static class GreenGradientButton extends JButton {
        public GreenGradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.BLACK);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color start = new Color(180, 255, 200); // Verde muito claro
            Color end = Color.WHITE;
            GradientPaint gp = new GradientPaint(0, 0, start, 0, getHeight(), end);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            super.paintComponent(g);
            g2.dispose();
        }
        @Override
        public void updateUI() {
            super.updateUI();
            setContentAreaFilled(false);
        }
    }

    // Botão com gradiente vermelho claro
    private static class RedGradientButton extends JButton {
        public RedGradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.BLACK);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color start = new Color(255, 200, 200); // Vermelho muito claro
            Color end = Color.WHITE;
            GradientPaint gp = new GradientPaint(0, 0, start, 0, getHeight(), end);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            super.paintComponent(g);
            g2.dispose();
        }
        @Override
        public void updateUI() {
            super.updateUI();
            setContentAreaFilled(false);
        }
    }
    
    public NextMenu(int option) {
        // Define o ícone da janela a partir do recurso
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.option = option;
        setTitle("Assinador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        initComponents();
        setMinimumSize(new Dimension(800, 600));


        // Adiciona um listener para fechar recursos na ação de fechar a janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    pt.gov.cartaodecidadao.PTEID_ReaderSet.releaseSDK();
                    System.out.println("SDK liberado via windowClosing.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    private void initComponents() {
        // Cria o painel esquerdo com CardLayout.
        leftCardLayout = new CardLayout();
        leftPanelContainer = new JPanel(leftCardLayout);
        leftPanelContainer.setBackground(new Color(255, 255, 224));
        leftPanelContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Painel "Escolher PDF": mostrará o botão
        JPanel choosePanel = new JPanel(new GridBagLayout());
        choosePanel.setOpaque(false);
        btnEscolherPDF = new JButton("Escolher PDF");
        btnEscolherPDF.setFont(new Font("Arial", Font.PLAIN, 16));
        choosePanel.add(btnEscolherPDF);
        
        // Painel "Visualizar PDF": conterá o preview
        pdfPreviewPanel = new PDFPreviewPanel();

        pdfPreviewPanel.setSignatureLocationListener(rect -> {
            int currentPage = pdfPreviewPanel.getCurrentPage();
            System.out.println("Signature rectangle callback: " + rect + ", currentPage: " + currentPage);
            if (option == 3 || option == 4) {
                // Atualiza as variáveis globais
                globalSignatureRect = rect;
                globalSignaturePage = currentPage;
                // Atualiza a cache global para todos os PDFs
                updateGlobalCacheForSignature(currentPage, rect, pdfPreviewPanel.getTotalPages());
            } else if (option == 5 || option == 6) {
                if (currentPDFFile != null) {
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), currentPage, rect);
                }
            } else if (option == 1 || option == 2) {
                // Para as opções 1 e 2, atualiza a cache igualmente.
                if (currentPDFFile != null) {
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), currentPage, rect);
                }
            }
        });
        
        // Adiciona os dois cartões
        leftPanelContainer.add(choosePanel, "choose");
        leftPanelContainer.add(pdfPreviewPanel, "preview");
        
        // Inicialmente, mostra o cartão "choose"
        leftCardLayout.show(leftPanelContainer, "choose");
        
        // Painel principal esquerdo (contendo o card layout)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(leftPanelContainer, BorderLayout.CENTER);
        
        // Cria o painel direito com os controles
        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBackground(new Color(173, 216, 230));
        rightPanel.setMinimumSize(new Dimension(350, 0)); // Nova linha, desbloequeou o mexer livremente a divisória
        
        // Divide a janela: 70% esquerda; 30% direita
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            leftPanel,
            rightPanel
        );
        // Faz com que o redimensionamento preserve sempre os 70%/30%
        splitPane.setResizeWeight(0.7);
        // Permite arrastar continuamente e um “one-touch” collapse
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        // Posiciona logo o divisor a 70% da largura total
        splitPane.setDividerLocation(0.7);
        splitPane.setDividerSize(5);

        getContentPane().add(splitPane, BorderLayout.CENTER);


        // Garante que a divisória fica mesmo a 70%/30% ao abrir
        SwingUtilities.invokeLater(() -> {
            // Depois da janela estar visível, reposiciona com base na largura real
            splitPane.setDividerLocation((int)(getWidth() * 0.7));
        });

        for (Component c : rightPanel.getComponents()) {
            System.out.println(c.getClass().getName() + " min: " + c.getMinimumSize() + " pref: " + c.getPreferredSize());
        }

        // Listener do botão "Escolher PDF"
        btnEscolherPDF.addActionListener(e -> abrirFileDialog(false));
        
        // Top Panel do lado direito: certificados, navegação, despacho e listagem
        JPanel topPanel = new JPanel();

        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Se opção for 2, 4 ou 6, mostra a droplist de certificados
        if (option == 2 || option == 4 || option == 6) {
            JLabel labelCert = new JLabel("Selecione um Certificado:");
            labelCert.setAlignmentX(Component.CENTER_ALIGNMENT);
            labelCert.setFont(new Font("Arial", Font.BOLD, 14));
            topPanel.add(labelCert);
            topPanel.add(Box.createVerticalStrut(10));
            String[] certs = CertificateUtils.getRealCertificates();
            comboCertificados = new JComboBox<>(certs);
            comboCertificados.setAlignmentX(Component.CENTER_ALIGNMENT);
            comboCertificados.setMaximumSize(new Dimension(320, 30));
            
            // Atualiza o PDFPreviewPanel com o certificado selecionado
            pdfPreviewPanel.setCertificateCN(comboCertificados.getSelectedItem().toString());
            
            // Sempre que o usuário alterar a seleção, atualiza também o preview
            comboCertificados.addActionListener(e -> {
                pdfPreviewPanel.setCertificateCN(comboCertificados.getSelectedItem().toString());
            });
            
            topPanel.add(comboCertificados);
            topPanel.add(Box.createVerticalStrut(20));
        }
        
        // Painel de navegação de páginas
        JPanel pageNavPanel = new JPanel();
        pageNavPanel.setLayout(new BoxLayout(pageNavPanel, BoxLayout.Y_AXIS));
        pageNavPanel.setOpaque(false);
        JLabel lblPaginaTitulo = new JLabel("Página");
        lblPaginaTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblPaginaTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        pageNavPanel.add(lblPaginaTitulo);
        pageNavPanel.add(Box.createVerticalStrut(10));
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        btnPrev = new JButton("<");
        btnPrev.setFont(new Font("Arial", Font.PLAIN, 14));
        btnPrev.setEnabled(false);
        controlsPanel.add(btnPrev);
        controlsPanel.add(Box.createHorizontalStrut(10));
        lblPage = new JLabel("0 / 0");
        lblPage.setFont(new Font("Arial", Font.PLAIN, 14));
        lblPage.setHorizontalAlignment(SwingConstants.CENTER);
        controlsPanel.add(lblPage);
        controlsPanel.add(Box.createHorizontalStrut(10));
        btnNext = new JButton(">");
        btnNext.setFont(new Font("Arial", Font.PLAIN, 14));
        btnNext.setEnabled(false);
        controlsPanel.add(btnNext);
        controlsPanel.add(Box.createHorizontalStrut(20));
        toggleLastPage = new JToggleButton("Última Página");
        toggleLastPage.setFont(new Font("Arial", Font.PLAIN, 14));
        toggleLastPage.setEnabled(false);
        controlsPanel.add(toggleLastPage);
        pageNavPanel.add(controlsPanel);
        topPanel.add(pageNavPanel);
        topPanel.add(Box.createVerticalStrut(20));
        
        // Botão "Adicionar PDF"
        btnAdicionarPDF = new JButton("Adicionar PDF");
        btnAdicionarPDF.setFont(new Font("Arial", Font.PLAIN, 16));
        btnAdicionarPDF.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(btnAdicionarPDF);
        topPanel.add(Box.createVerticalStrut(20));
        btnAdicionarPDF.addActionListener(e -> {
            if (option == 1 || option == 2 || option == 5 || option == 6) {
                abrirFileDialog(false);
            } else if (option == 3 || option == 4) {
                abrirFileDialog(true);
            }
        });
        
        // Rótulo e área de texto "DESPACHO"
        JLabel lblDespacho = new JLabel("<html><meta charset='UTF-8'><div style='text-align:center;'>DESPACHO</div></html>", SwingConstants.CENTER);
        lblDespacho.setFont(new Font("Arial", Font.BOLD, 14));
        lblDespacho.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(lblDespacho);
        topPanel.add(Box.createVerticalStrut(10));
        JTextArea txtDespacho = new JTextArea(5, 30);
        txtDespacho.setLineWrap(true);
        txtDespacho.setWrapStyleWord(true);
        txtDespacho.setFont(new Font("Arial", Font.PLAIN, 12));
        txtDespacho.setBackground(Color.WHITE);
        txtDespacho.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        txtDespacho.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                pdfPreviewPanel.setDespachoText(txtDespacho.getText());
            }
        
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                pdfPreviewPanel.setDespachoText(txtDespacho.getText());
            }
        
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                pdfPreviewPanel.setDespachoText(txtDespacho.getText());
            }
        });
        JScrollPane despachoScrollPane = new JScrollPane(txtDespacho);

        despachoScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        despachoScrollPane.setMaximumSize(new Dimension(360, txtDespacho.getPreferredSize().height));
        despachoScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(despachoScrollPane);
        topPanel.add(Box.createVerticalStrut(20));

        // Painel de listagem de PDFs
        if (option == 3 || option == 4 || option == 5 || option == 6) {
            fileListPanel = new FileListPanel(this);

            JScrollPane fileListScrollPane = new JScrollPane(fileListPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            fileListScrollPane.setBorder(BorderFactory.createEmptyBorder());

            JPanel wrapperPanel = new JPanel();
            wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.Y_AXIS));
            wrapperPanel.setOpaque(false);
            wrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            wrapperPanel.add(fileListScrollPane);

            // Novo botão para limpar todos os documentos
            btnLimparTudo = new JButton("Limpar Tudo");
            btnLimparTudo.setFont(new Font("Arial", Font.PLAIN, 16));
            btnLimparTudo.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnLimparTudo.addActionListener(e -> clearAllDocuments());
            wrapperPanel.add(Box.createVerticalStrut(10));
            wrapperPanel.add(btnLimparTudo);
            wrapperPanel.add(Box.createVerticalStrut(10));

            uniformButtonSize(btnLimparTudo);
            // --------------------------------------

            rightPanel.add(wrapperPanel, BorderLayout.CENTER);
        }
        
        rightPanel.add(topPanel, BorderLayout.NORTH);
        
        // Painel inferior do lado direito: botões de assinatura
        JPanel bottomPanel = new JPanel();

        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnAssinarOutraForma = new RedGradientButton("Assinar de outra forma");
        btnAssinarOutraForma.setFont(new Font("Arial", Font.PLAIN, 16));
        btnAssinarOutraForma.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAssinarOutraForma.addActionListener(e -> resetApplication());
        bottomPanel.add(Box.createVerticalGlue());
        bottomPanel.add(btnAssinarOutraForma);
        bottomPanel.add(Box.createVerticalStrut(10));
        btnAssinar = new GreenGradientButton("Assinar");
        btnAssinar.setFont(new Font("Arial", Font.PLAIN, 16));
        btnAssinar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAssinar.addActionListener(e -> {
            // Verifica se há PDFs na cache
            if (pdfCache.getAllPDFInfos().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum PDF disponível para assinar.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Itera pelos PDFs do cache e valida se cada um possui os dados de assinatura necessários
            for (PDFInfo info : pdfCache.getAllPDFInfos()) {
                if (info.getSignatureRect() == null || info.getCurrentPage() <= 0) {
                    JOptionPane.showMessageDialog(this, "O documento " + info.getFileName() + " não possui a assinatura desenhada.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // Obtém o texto de despacho, se houver (por exemplo, de um JTextField)
            String despachoText = txtDespacho.getText();
            
            // Se a opção for 2, 4 ou 6, usa o fluxo existente (DocumentSignerLocal)
            if (option == 2 || option == 4 || option == 6) {
                Object selected = comboCertificados.getSelectedItem();
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Nenhum certificado selecionado.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String certificateAlias = selected.toString();
                
                try {
                    DocumentSignerLocal.signAllDocuments(this,new ArrayList<>(pdfCache.getAllPDFInfos()), certificateAlias, "", despachoText);
                    JOptionPane.showMessageDialog(this, "Todos os documentos foram assinados com sucesso!");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Erro ao assinar os documentos: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } 
            // Caso a opção seja 1, 3 ou 5, utiliza a assinatura via Cartão de Cidadão (DocumentSigner)
            else if (option == 1 || option == 3 || option == 5) {
                try {
                    DocumentSigner signer = new DocumentSigner();
                    for (PDFInfo info : pdfCache.getAllPDFInfos()) {
                        File pdfFile = info.getFile(); // método que retorna o arquivo PDF
                        int page = info.getCurrentPage();
                        java.awt.Rectangle rect = info.getSignatureRect();
                        // Para assinatura com Cartão de Cidadão, passo outputFile como null para que 
                        // o método gere automaticamente o nome com "_signed". Aqui, usa-se "" para location.
                        signer.signPdf(pdfFile, null, page, rect, "", despachoText);
                    }
                    JOptionPane.showMessageDialog(this, "Todos os documentos foram assinados com sucesso!");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Erro ao assinar os documentos: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Opção de assinatura inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        bottomPanel.add(btnAssinar);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        uniformButtonSize(btnAdicionarPDF, btnAssinarOutraForma, btnAssinar);
        
        // Listeners de navegação de página
        btnPrev.addActionListener(e -> {
            pdfPreviewPanel.previousPage();
            int newPage = pdfPreviewPanel.getCurrentPage();
            if (option == 3 || option == 4) {
                globalSignaturePage = newPage;
                updateGlobalCacheForPage(newPage);
            } else if (option == 5 || option == 6) {
                Rectangle viewRect = pdfPreviewPanel.getSignatureRectangle();
                if (viewRect != null) {
                    Rectangle absoluteRect = pdfPreviewPanel.convertViewToAbsolute(viewRect);
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), newPage, absoluteRect);
                }
            } else if (option == 1 || option == 2) {
                // Para as opções 1 e 2, atualiza a cache com o retângulo (se houver) ou nulo
                Rectangle viewRect = pdfPreviewPanel.getSignatureRectangle();
                if (viewRect != null) {
                    Rectangle absoluteRect = pdfPreviewPanel.convertViewToAbsolute(viewRect);
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), newPage, absoluteRect);
                } else {
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), newPage, null);
                }
            }
            pdfPreviewPanel.refreshSignatureRectangle();
            lblPage.setText(newPage + " / " + pdfPreviewPanel.getTotalPages());
        });
        
        btnNext.addActionListener(e -> {
            pdfPreviewPanel.nextPage();
            int newPage = pdfPreviewPanel.getCurrentPage();
            if (option == 3 || option == 4) {
                globalSignaturePage = newPage;
                updateGlobalCacheForPage(newPage);
            } else if (option == 5 || option == 6) {
                Rectangle viewRect = pdfPreviewPanel.getSignatureRectangle();
                if (viewRect != null) {
                    Rectangle absoluteRect = pdfPreviewPanel.convertViewToAbsolute(viewRect);
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), newPage, absoluteRect);
                }
            } else if (option == 1 || option == 2) {
                // Atualiza a cache para as opções 1 e 2 também
                Rectangle viewRect = pdfPreviewPanel.getSignatureRectangle();
                if (viewRect != null) {
                    Rectangle absoluteRect = pdfPreviewPanel.convertViewToAbsolute(viewRect);
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), newPage, absoluteRect);
                } else {
                    pdfCache.updatePDFInfo(currentPDFFile, pdfPreviewPanel.getTotalPages(), newPage, null);
                }
            }
            pdfPreviewPanel.refreshSignatureRectangle();
            lblPage.setText(newPage + " / " + pdfPreviewPanel.getTotalPages());
        });
        
        toggleLastPage.addItemListener(e -> {
            pdfPreviewPanel.toggleLastPage(e.getStateChange() == ItemEvent.SELECTED);
            lblPage.setText(pdfPreviewPanel.getCurrentPage() + " / " + pdfPreviewPanel.getTotalPages());
        });
    }

    /**
     * Método para limpar todos os documentos.
     */
    private void clearAllDocuments() {
        pdfPreviewPanel.clearDocument();
        pdfFiles.clear();
        pdfCache.clear();
        currentPDFFile = null;
        if (fileListPanel != null) {
            fileListPanel.refreshList(pdfFiles, null);
        }
        // Limpa o PDFPreviewPanel e volta ao modo "Escolher PDF"
        leftCardLayout.show(leftPanelContainer, "choose");
        pdfPreviewPanel.setSignatureRectangle(null);
        pdfPreviewPanel.repaint();
    }
    
    /**
     * Abre o FileDialog e para cada PDF novo chama loadDocument.
     */
    private void abrirFileDialog(boolean multiSelection) {
        JnaFileChooser fileChooser = new JnaFileChooser();
        fileChooser.setTitle("Selecione um PDF");
        fileChooser.setMultiSelectionEnabled(multiSelection);
        fileChooser.addFilter("PDF Files (*.pdf)", "pdf");

        boolean approved = fileChooser.showOpenDialog(this);
        if (!approved) {
            return; // Utilizador cancelou
        }

        File[] selectedFiles = fileChooser.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.length == 0) {
            return;
        }
    
        if (multiSelection) {
            for (File f : selectedFiles) {
                if (!pdfFilesContain(f)) {
                    pdfFiles.add(f);
                }
            }
            currentPDFFile = selectedFiles[selectedFiles.length - 1];
            loadDocument(currentPDFFile);
        } else {
            File selectedFile = selectedFiles[0];
            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(this, "Apenas ficheiros PDF são permitidos.",
                    "Arquivo Inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (pdfFilesContain(selectedFile)) {
                JOptionPane.showMessageDialog(this, "Este ficheiro já foi adicionado.",
                    "Ficheiro Duplicado", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            pdfFiles.add(selectedFile);
            currentPDFFile = selectedFile;
            loadDocument(currentPDFFile);
        }
    
        if (fileListPanel != null) {
            fileListPanel.refreshList(pdfFiles, currentPDFFile);
        }
    }

    private boolean pdfFilesContain(File f) {
        if (f == null) return false;
        String newPath = f.getAbsolutePath();
        for (File existing : pdfFiles) {
            if (existing.getAbsolutePath().equalsIgnoreCase(newPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Método central de carregamento e atualização do estado do PDF.
     * Salva o estado (quando houver) do PDF atual no cache e, ao carregar o novo, procura
     * os dados salvos (ou usa os valores default/global) para atualizar o PDFPreviewPanel,
     * os botões e o rótulo de navegação.
     */
    private void loadDocument(File pdfFile) {
        // Se já estiver a visualizar um PDF, salva o estado atual no cache
        
        // Atualiza o arquivo atual e carrega o novo PDF
        currentPDFFile = pdfFile;
        pdfPreviewPanel.loadDocument(pdfFile, toggleLastPage.isSelected());
        toggleLastPage.setEnabled(true);
        btnPrev.setEnabled(pdfPreviewPanel.getTotalPages() > 1);
        btnNext.setEnabled(pdfPreviewPanel.getTotalPages() > 1);

        try {
            Rectangle cachedSignatureRect = pdfCache.getPDFInfo(currentPDFFile).getSignatureRect();
            pdfPreviewPanel.setSignatureRectangle(cachedSignatureRect);
            pdfPreviewPanel.repaint();
        } catch (Exception e) {
            // Para Debug
            System.out.println("Ainda não tem retangulo");
        }
        
        // Usa dois invokeLater para garantir que a atualização interna do PDF já ocorreu

        SwingUtilities.invokeLater(() -> {
            int totalPages = pdfPreviewPanel.getTotalPages();
            if (option == 1 || option == 2) {
                // Para as opções 1 e 2, limpamos a cache e adicionamos o novo PDF
                int startPage = toggleLastPage.isSelected() ? totalPages : 1;
                pdfPreviewPanel.goToPage(startPage);
                pdfPreviewPanel.setSignatureRectangle(null);  // Nenhuma assinatura definida inicialmente
                pdfCache.clear();   // Garante que só haverá um PDF na cache
                pdfCache.updatePDFInfo(pdfFile, totalPages, startPage, null);
            } else if (option == 3 || option == 4) {
                // Para assinatura global, consulta a cache
                PDFInfo info = pdfCache.getPDFInfo(pdfFile);
                int pageToGo;
                Rectangle rect;
                if (info != null) {
                    pageToGo = info.getCurrentPage();
                    rect = info.getSignatureRect();
                } else {
                    // Se não houver dados na cache, usa os valores globais
                    pageToGo = globalSignaturePage;
                    rect = globalSignatureRect;
                    pdfCache.updatePDFInfo(pdfFile, totalPages, pageToGo, rect);
                }
                if (pageToGo > totalPages) {
                    pageToGo = totalPages;
                }
                pdfPreviewPanel.goToPage(pageToGo);
                pdfPreviewPanel.setSignatureRectangle(rect);
            } else if (option == 5 || option == 6) {
                // Para assinatura individual, apenas consulta a cache.
                PDFInfo info = pdfCache.getPDFInfo(pdfFile);
                if (info != null) {
                    int targetPage = info.getCurrentPage();
                    if (targetPage > totalPages) {
                        targetPage = 1;
                    }
                    pdfPreviewPanel.goToPage(targetPage);
                    pdfPreviewPanel.setSignatureRectangle(info.getSignatureRect());
                } else {
                    int startPage = toggleLastPage.isSelected() ? totalPages : 1;
                    pdfPreviewPanel.goToPage(startPage);
                    pdfPreviewPanel.setSignatureRectangle(null);
                    pdfCache.updatePDFInfo(pdfFile, totalPages, startPage, null);
                }
            }
            lblPage.setText(pdfPreviewPanel.getCurrentPage() + " / " + totalPages);
            btnPrev.setEnabled(totalPages > 1);
            btnNext.setEnabled(totalPages > 1);
        });

        
        // Mostra o cartão de preview
        leftCardLayout.show(leftPanelContainer, "preview");
    }
    
    /**
     * Atualiza, para todos os PDFs da aplicação, o retângulo atual e o número da página,
     * considerando que cada PDF pode ter um total de páginas diferente.
     */
    private void updateGlobalCacheForSignature(int currentPage, Rectangle newRect, int totalPagesGlobal) {
        for (File f : pdfFiles) {
            PDFInfo info = pdfCache.getPDFInfo(f);
            int fileTotal = (info != null) ? info.getTotalPages() : totalPagesGlobal;
            int newPage = currentPage > fileTotal ? fileTotal : currentPage;
            if (info != null) {
                info.setCurrentPage(newPage);
                info.setSignatureRect(newRect);
            } else {
                pdfCache.updatePDFInfo(f, fileTotal, newPage, newRect);
            }
        }
    }

    /**
     * Atualiza, para todos os PDFs, o número da página no cache.
     */
    private void updateGlobalCacheForPage(int currentPage) {
        for (File f : pdfFiles) {
            PDFInfo info = pdfCache.getPDFInfo(f);
            if (info != null) {
                int newPage = currentPage > info.getTotalPages() ? info.getTotalPages() : currentPage;
                info.setCurrentPage(newPage);
            }
        }
    }

    // Métodos do FileListListener
    @Override
    public void fileSelected(File f) {
        loadDocument(f);
        fileListPanel.refreshList(pdfFiles, currentPDFFile);
    }

    @Override
    public void fileRemoved(File f) {
        boolean wasCurrent = f.equals(currentPDFFile);
        pdfFiles.remove(f);
        pdfCache.removePDFInfo(f);
        
        if (wasCurrent) {
            if (!pdfFiles.isEmpty()) {
                currentPDFFile = pdfFiles.get(0);
                loadDocument(currentPDFFile);
            } else {
                currentPDFFile = null;
                leftCardLayout.show(leftPanelContainer, "choose");
            }
        }
        fileListPanel.refreshList(pdfFiles, currentPDFFile);
    }
    
    private void resetApplication() {
        clearAllDocuments(); // Limpa tudo antes de abrir a nova tela
        SwingUtilities.invokeLater(() -> new InitialLayout().setVisible(true));
        dispose();
    }
}