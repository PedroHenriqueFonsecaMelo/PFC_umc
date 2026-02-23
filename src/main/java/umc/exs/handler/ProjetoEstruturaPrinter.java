package umc.exs.handler;
import java.io.File;

public class ProjetoEstruturaPrinter {

    public static void main(String[] args) {
        // Caminho para a raiz do seu pacote java
        File raiz = new File("src/main/java/umc/exs");

        System.out.println("DIAGRAMA DE CLASSES E PASTAS DO PROJETO");
        System.out.println("========================================");

        if (raiz.exists() && raiz.isDirectory()) {
            imprimirNo(raiz, 0, true);
        } else {
            System.out.println("Erro: Diretorio 'src/main/java/umc/exs' nao encontrado.");
        }
    }

    private static void imprimirNo(File arquivo, int nivel, boolean ultimo) {
        // Cria a indentação visual
        for (int i = 0; i < nivel; i++) {
            System.out.print("│   ");
        }

        // Define o prefixo (se é o último item da pasta ou não)
        System.out.print(ultimo ? "└── " : "├── ");

        // Se for diretório, adiciona uma barra no final
        if (arquivo.isDirectory()) {
            System.out.println(arquivo.getName() + "/");
            
            File[] filhos = arquivo.listFiles();
            if (filhos != null) {
                for (int i = 0; i < filhos.length; i++) {
                    // Chama recursivamente para as classes e subpastas
                    imprimirNo(filhos[i], nivel + 1, i == filhos.length - 1);
                }
            }
        } else {
            // Se for arquivo, imprime apenas o nome
            System.out.println(arquivo.getName());
        }
    }
}