package umc.exs.service.core.bussiness;

import java.util.Map;

public class GeneroMapper {

    private static final Map<String, String> MAPA = Map.ofEntries(
        Map.entry("Fiction", "Ficção"),
        Map.entry("Juvenile Fiction", "Infantojuvenil"),
        Map.entry("Young Adult Fiction", "Jovem Adulto"),
        Map.entry("Science Fiction", "Ficção Científica"),
        Map.entry("Fantasy", "Fantasia"),
        Map.entry("Mystery", "Mistério"),
        Map.entry("Thriller", "Suspense"),
        Map.entry("Horror", "Terror"),
        Map.entry("Romance", "Romance"),
        Map.entry("Historical Fiction", "Ficção Histórica"),
        Map.entry("Adventure", "Aventura"),
        Map.entry("Biography & Autobiography", "Biografia"),
        Map.entry("Biography", "Biografia"),
        Map.entry("History", "História"),
        Map.entry("Philosophy", "Filosofia"),
        Map.entry("Psychology", "Psicologia"),
        Map.entry("Self-Help", "Autoajuda"),
        Map.entry("Business & Economics", "Negócios"),
        Map.entry("Science", "Ciências"),
        Map.entry("Technology", "Tecnologia"),
        Map.entry("Computers", "Computação"),
        Map.entry("Art", "Arte"),
        Map.entry("Music", "Música"),
        Map.entry("Poetry", "Poesia"),
        Map.entry("Drama", "Drama"),
        Map.entry("Comics & Graphic Novels", "Quadrinhos"),
        Map.entry("Religion", "Religião"),
        Map.entry("Cooking", "Culinária"),
        Map.entry("Sports & Recreation", "Esportes"),
        Map.entry("Travel", "Viagem"),
        Map.entry("Education", "Educação"),
        Map.entry("Literary Collections", "Literatura"),
        Map.entry("Literary Criticism", "Crítica Literária"),
        Map.entry("Nonfiction", "Não-ficção"),
        Map.entry("Social Science", "Ciências Sociais"),
        Map.entry("Political Science", "Política"),
        Map.entry("Medical", "Medicina"),
        Map.entry("Law", "Direito"),
        Map.entry("Nature", "Natureza"),
        Map.entry("Humor", "Humor"),
        Map.entry("Games & Activities", "Jogos")
    );

    public static String traduzir(String generoIngles) {
        if (generoIngles == null || generoIngles.isBlank()) return null;
        // Tenta tradução exata primeiro
        String trad = MAPA.get(generoIngles.trim());
        if (trad != null) return trad;
        // Tenta match parcial (ex: "Fiction / Fantasy" → "Fantasia")
        for (Map.Entry<String, String> entry : MAPA.entrySet()) {
            if (generoIngles.contains(entry.getKey())) return entry.getValue();
        }
        return generoIngles; // fallback: retorna o original
    }
}
