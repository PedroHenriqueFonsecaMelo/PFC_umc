package umc.exs.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    public static <T> ExternApiResponse<T> ok(T data, String message) {
        return ExternApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static ExternApiResponse<Void> ok(String message) {
        return ExternApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ExternApiResponse<T> fail(String message) {
        return ExternApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(message)
                .build();
    }
}