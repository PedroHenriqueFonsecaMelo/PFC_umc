package umc.exs.DTOs.auth;

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

    /**
     * @return String
     */
    // Getters and Setters
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return T
     */
    public T getData() {
        return data;
    }

    /**
     * @param data
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * @return String
     */
    public String getError() {
        return error;
    }

    /**
     * @param error
     */
    public void setError(String error) {
        this.error = error;
    }
}
