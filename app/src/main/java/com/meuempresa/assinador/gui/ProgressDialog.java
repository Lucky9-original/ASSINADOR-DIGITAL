package com.meuempresa.assinador.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Janela de progresso modal que exibe uma barra de progresso e mensagem.
 */
public class ProgressDialog extends JDialog {
    private final JProgressBar progressBar;
    private final JLabel messageLabel;

    /**
     * Constrói a janela de progresso.
     * @param owner janela pai
     * @param title título da janela
     * @param total número total de ficheiros
     */
    public ProgressDialog(Frame owner, String title, int total) {
        super(owner, title, true);
        progressBar = new JProgressBar(0, total);
        progressBar.setStringPainted(true);
        messageLabel = new JLabel("Preparando...");
        initLayout();
    }

    private void initLayout() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(messageLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        getContentPane().add(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    /**
     * Atualiza o valor da barra e a mensagem exibida.
     * @param value passo atual (1-based)
     * @param message mensagem de status
     */
    public void updateProgress(int value, String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            messageLabel.setText(message);
        });
    }
}
