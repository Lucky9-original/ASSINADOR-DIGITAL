package com.meuempresa.assinador.gui;

import javax.swing.*;

// import javafx.stage.WindowEvent;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class InitialLayout extends JFrame {

    public InitialLayout() {
        // Define o ícone da janela a partir do recurso
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        setTitle("Assinador Digital - Menu Inicial");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null); // centraliza a janela
        initComponents();

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
        // Cria um painel com GridLayout de 3 linhas x 2 colunas com espaçamento
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // margem interna
        
        // Define os botões com HTML para centralizar o texto e garantir UTF-8.
        JButton btnAssinarCC = new JButton(
            "<html><meta charset='UTF-8'>" +
            "<div style='text-align:center; font-family:Arial, sans-serif; font-size:12px;'>" +
            "Assinar um<br>documento com CC" +
            "</div></html>"
        );
        
        JButton btnAssinarOutro = new JButton(
            "<html><meta charset='UTF-8'>" +
            "<div style='text-align:center; font-family:Arial, sans-serif; font-size:12px;'>" +
            "Assinar um<br>documento com outro Certificado" +
            "</div></html>"
        );
        
        JButton btnAssinarVariosMesmoLocalCC = new JButton(
            "<html><meta charset='UTF-8'>" +
            "<div style='text-align:center; font-family:Arial, sans-serif; font-size:12px;'>" +
            "Assinar vários documentos<br>no mesmo local com CC" +
            "</div></html>"
        );

        JButton btnAssinarVariosMesmoLocalOutro = new JButton(
            "<html><meta charset='UTF-8'>" +
            "<div style='text-align:center; font-family:Arial, sans-serif; font-size:12px;'>" +
            "Assinar vários documentos<br>no mesmo local com outro Certificado" +
            "</div></html>"
        );
        
        JButton btnAssinarVariosDiferentesLocalCC = new JButton(
            "<html><meta charset='UTF-8'>" +
            "<div style='text-align:center; font-family:Arial, sans-serif; font-size:12px;'>" +
            "Assinar vários documentos<br>em locais diferentes com CC" +
            "</div></html>"
        );
        
        JButton btnAssinarVariosDiferentesLocalOutro = new JButton(
            "<html><meta charset='UTF-8'>" +
            "<div style='text-align:center; font-family:Arial, sans-serif; font-size:12px;'>" +
            "Assinar vários documentos<br>em locais diferentes com outro Certificado" +
            "</div></html>"
        );
        
        // Cria um listener que chama o próximo menu, passando a opção desejada
        ActionListener openNextMenuListener = e -> {
            // Identifica qual botão foi clicado e define a opção de acordo
            Object source = e.getSource();
            int option = 0;
            if (source == btnAssinarCC) {
                option = 1;
            } else if (source == btnAssinarOutro) {
                option = 2;
            } else if (source == btnAssinarVariosMesmoLocalCC) {
                option = 3;
            } else if (source == btnAssinarVariosDiferentesLocalCC) {
                option = 5;
            } else if (source == btnAssinarVariosMesmoLocalOutro) {
                option = 4;
            } else if (source == btnAssinarVariosDiferentesLocalOutro) {
                option = 6;
            }
            openNextMenu(option);
        };
        
        // Adiciona o listener a cada botão
        btnAssinarCC.addActionListener(openNextMenuListener);
        btnAssinarOutro.addActionListener(openNextMenuListener);
        btnAssinarVariosMesmoLocalCC.addActionListener(openNextMenuListener);
        btnAssinarVariosMesmoLocalOutro.addActionListener(openNextMenuListener);
        btnAssinarVariosDiferentesLocalCC.addActionListener(openNextMenuListener);
        btnAssinarVariosDiferentesLocalOutro.addActionListener(openNextMenuListener);
        
        // Adiciona os botões ao painel
        panel.add(btnAssinarCC);
        panel.add(btnAssinarOutro);
        panel.add(btnAssinarVariosMesmoLocalCC);
        panel.add(btnAssinarVariosMesmoLocalOutro);
        panel.add(btnAssinarVariosDiferentesLocalCC);
        panel.add(btnAssinarVariosDiferentesLocalOutro);
        
        // Adiciona o painel ao frame
        add(panel, BorderLayout.CENTER);
    }
    
    // Método que abre o próximo menu, passando a opção selecionada
    private void openNextMenu(int option) {
        if (option == 3 || option == 5) {
            JOptionPane.showMessageDialog(this,
                "Atenção que nesta opção terá de colocar o PIN várias vezes. Por cada documento que quiser assinar de uma vez vai ter de colocar o PIN novamente!",
                "Informação",
                JOptionPane.INFORMATION_MESSAGE);
        }
        NextMenu nextMenu = new NextMenu(option);
        nextMenu.setVisible(true);
        // Fecha o menu inicial para evitar janelas redundantes
        this.dispose();
    }
    
}