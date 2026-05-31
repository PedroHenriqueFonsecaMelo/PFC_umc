package umc.exs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyScanner {

    // Regex para capturar o nome da variável dentro de @Value("${NOME}") ou
    // @Value("${NOME:padrao}")
    private static final Pattern VALUE_PATTERN = Pattern
            .compile("@Value\\s*\\(\\s*\"\\s*\\$\\{([^}:]+)(?::[^}]*)?\\}\\s*\"\\s*\\)");

    // Regex para capturar nomes em Environment.getProperty("NOME")
    private static final Pattern ENV_PATTERN = Pattern.compile("getProperty\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");

    public static void main(String[] args) {

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        File pastaJava = new File("src/main/java/umc/exs");
        TreeSet<String> nomesEncontrados = new TreeSet<>();

        System.out.println("LISTA DE DEPENDÊNCIAS DE AMBIENTE REQUERIDAS PELO JAVA");
        System.out.println("=====================================================");

        if (pastaJava.exists() && pastaJava.isDirectory()) {
            escanearPasta(pastaJava, nomesEncontrados);

            if (nomesEncontrados.isEmpty()) {
                System.out.println("Nenhum contrato de variável externa encontrado no código Java.");
            } else {
                nomesEncontrados.forEach(nome -> System.out.println("ID: " + nome));
            }

            System.out.println("\nTotal de chaves mapeadas no código: " + nomesEncontrados.size());
        } else {
            System.err.println("Erro: Caminho do código fonte não encontrado.");
        }
    }

    private static void escanearPasta(File pasta, TreeSet<String> lista) {
        File[] arquivos = pasta.listFiles();
        if (arquivos == null)
            return;

        for (File arquivo : arquivos) {
            if (arquivo.isDirectory()) {
                escanearPasta(arquivo, lista);
            } else if (arquivo.getName().endsWith(".java")) {
                extrairNomes(arquivo, lista);
            }
        }
    }

    private static void extrairNomes(File arquivo, TreeSet<String> lista) {
        try (BufferedReader br = Files.newBufferedReader(arquivo.toPath(), StandardCharsets.UTF_8)) {
            String linha;
            while ((linha = br.readLine()) != null) {
                // Procura por @Value
                Matcher mValue = VALUE_PATTERN.matcher(linha);
                while (mValue.find()) {
                    lista.add(mValue.group(1));
                }

                // Procura por getProperty
                Matcher mEnv = ENV_PATTERN.matcher(linha);
                while (mEnv.find()) {
                    lista.add(mEnv.group(1));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo: " + arquivo.getAbsolutePath(), e);
        }
    }
}