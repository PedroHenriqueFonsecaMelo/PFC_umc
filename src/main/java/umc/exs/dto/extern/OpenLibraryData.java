package umc.exs.dto.extern;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryData {

    private String title;
    private List<Author> authors;
    private Cover cover;
    private String notes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cover {
        private String small;
        private String medium;
        private String large;
    }
}
