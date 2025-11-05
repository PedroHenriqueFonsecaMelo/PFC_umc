package umc.exs.model.dtos.auth;

public class ResponseDTO<T> {
    private String message;
    private T data;
    private String error;

    // Constructor for success response
    public ResponseDTO(String message, T data) {
        this.message = message;
        this.data = data;
    }

    // Constructor for error response
    public ResponseDTO(String error) {
        this.error = error;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

