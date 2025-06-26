package com.meuempresa.assinador;

public class Main {
    static {
        try {
            System.loadLibrary("pteidlibj");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Não foi possível carregar a biblioteca nativa pteidlibj: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            pt.gov.cartaodecidadao.PTEID_ReaderSet.initSDK();
        } catch (Exception ex) {
            System.err.println("Erro ao inicializar o SDK: " + ex.getMessage());
            System.exit(1);
        }
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            new com.meuempresa.assinador.gui.InitialLayout().setVisible(true);
        });
    }
}
