package umc.exs;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjetoEstruturaPrinter {

    private static final Logger logger = LoggerFactory.getLogger(ProjetoEstruturaPrinter.class);

    public static void main(String[] args) {

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        File raiz = new File("src/main/java/umc/exs");

        logger.info("DIAGRAMA DE CLASSES E PASTAS DO PROJETO");
        logger.info("========================================");

        if (raiz.exists() && raiz.isDirectory()) {
            imprimirNo(raiz, 0, true);
        } else {
            logger.error("Erro: Diretorio 'src/main/java/umc/exs' nao encontrado.");
        }
    }

    private static void imprimirNo(File arquivo, int nivel, boolean ultimo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            sb.append("│   ");
        }

        sb.append(ultimo ? "└── " : "├── ");

        if (arquivo.isDirectory()) {
            sb.append(arquivo.getName()).append("/");
            logger.info(sb.toString());

            File[] filhos = arquivo.listFiles();
            if (filhos != null) {
                for (int i = 0; i < filhos.length; i++) {
                    imprimirNo(filhos[i], nivel + 1, i == filhos.length - 1);
                }
            }
        } else {

        }
    }
}