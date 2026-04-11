package umc.exs;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ProjetoEstruturaPrinter {

    public static void main(String[] args) {

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

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
        for (int i = 0; i < nivel; i++) {
            System.out.print("│   ");
        }

        System.out.print(ultimo ? "└── " : "├── ");

        if (arquivo.isDirectory()) {
            System.out.println(arquivo.getName() + "/");

            File[] filhos = arquivo.listFiles();
            if (filhos != null) {
                for (int i = 0; i < filhos.length; i++) {
                    imprimirNo(filhos[i], nivel + 1, i == filhos.length - 1);
                }
            }
        } else {
            System.out.println(arquivo.getName());
        }
    }
}