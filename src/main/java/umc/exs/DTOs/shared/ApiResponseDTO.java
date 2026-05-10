package umc.exs.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponseDTO {
    private boolean success;
    private String message;
}