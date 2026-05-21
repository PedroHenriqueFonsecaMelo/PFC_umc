package umc.exs.dto.extern;

import java.util.List;

import lombok.Data;

@Data
public class GoogleBookData {

    private List<Item> items;

    @Data
    public static class Item {
        private VolumeInfo volumeInfo;
    }

    @Data
    public static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String language;
        private String description;
        private ImageLinks imageLinks;
        private List<String> categories;
        private Integer pageCount;
        private String publishedDate;
        private String publisher;
    }

    @Data
    public static class ImageLinks {
        private String thumbnail;
        private String smallThumbnail;

    }

}
