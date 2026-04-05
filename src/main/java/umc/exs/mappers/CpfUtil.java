package umc.exs.mappers;

/**
 * Utilitário de mascaramento de CPF.
 * Separado da interface ClienteMapper para evitar conflitos
 * entre MapStruct e métodos estáticos em interfaces.
 */
public final class CpfUtil {

    private CpfUtil() {
    }

    /**
     * Mascara CPF para exibição segura no frontend.
     * Exemplo: "12345678901" → "***.***. 789-01"
     */
    public static String mascararCpf(String cpf) {
        if (cpf == null)
            return null;
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11)
            return "***.***.***-**";
        return String.format("***.***.%s-%s",
                digits.substring(6, 9),
                digits.substring(9));
    }
}
