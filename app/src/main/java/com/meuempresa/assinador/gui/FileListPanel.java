package com.meuempresa.assinador.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class FileListPanel extends JPanel {
    private FileListListener listener;
    private Font font = new Font("Arial", Font.PLAIN, 12);
    
    public FileListPanel(FileListListener listener) {
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(0, 20, 0, 20)
        ));
    }
    
    public void refreshList(List<File> pdfFiles, File selectedFile) {
        removeAll();
        
        int containerWidth = 360;
        Container parent = getParent();
        if (parent != null && parent.getWidth() > 0) {
            containerWidth = parent.getWidth();
        } else if (getWidth() > 0) {
            containerWidth = getWidth();
        }
        int maxLabelWidth = (int) (containerWidth * 0.7);
        
        for (File f : pdfFiles) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel lblFile = new JLabel();
            lblFile.setFont(font);
            FontMetrics fm = lblFile.getFontMetrics(font);
            String fullName = f.getName();
            String displayName = fullName;
            if (fm.stringWidth(fullName) > maxLabelWidth) {
                displayName = truncateString(fullName, fm, maxLabelWidth);
            }
            lblFile.setText(displayName);
            lblFile.setToolTipText(fullName);
            lblFile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblFile.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (listener != null) {
                        listener.fileSelected(f);
                    }
                }
            });
            JButton btnRemove = new JButton("X");
            btnRemove.setFont(font);
            btnRemove.setMargin(new Insets(2, 5, 2, 5));
            btnRemove.addActionListener(e -> {
                if (listener != null) {
                    listener.fileRemoved(f);
                }
            });
            row.add(lblFile, BorderLayout.CENTER);
            row.add(btnRemove, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            
            if (f.equals(selectedFile)) {
                row.setBackground(new Color(200, 230, 255));
                row.setOpaque(true);
            }
            
            add(row);
            add(Box.createVerticalStrut(5));
        }
        
        revalidate();
        repaint();
    }
    
    private String truncateString(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        int availableWidth = maxWidth - ellipsisWidth;
        int index = 0;
        for (int i = 0; i < text.length(); i++) {
            if (fm.stringWidth(text.substring(0, i + 1)) > availableWidth) {
                index = i;
                break;
            }
        }
        if (index <= 0) {
            index = text.length();
        }
        return text.substring(0, index) + ellipsis;
    }
    
    // Interface para notificar seleção e remoção de arquivos
    public interface FileListListener {
        void fileSelected(File f);
        void fileRemoved(File f);
    }
}