package com.nabra.backend.modules.visualdictionary.controller;

import com.nabra.backend.modules.visualdictionary.dto.*;
import com.nabra.backend.modules.visualdictionary.model.Category;
import com.nabra.backend.modules.visualdictionary.model.Word;
import com.nabra.backend.modules.visualdictionary.model.WordVideo;
import com.nabra.backend.modules.visualdictionary.service.VisualDictionaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisualDictionaryAdminControllerTest {

    @Mock
    private VisualDictionaryService service;

    private VisualDictionaryAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new VisualDictionaryAdminController(service);
    }

    @Test
    void createCategory_shouldReturnCategoryDto() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Animals");
        req.setIcon("🐾");

        Category c = new Category();
        c.setId("cat1");
        c.setName("Animals");
        c.setIcon("🐾");

        when(service.createCategory(any())).thenReturn(c);

        CategoryDto result = controller.createCategory(req);

        assertThat(result.getId()).isEqualTo("cat1");
        assertThat(result.getName()).isEqualTo("Animals");
        assertThat(result.getIcon()).isEqualTo("🐾");
    }

    @Test
    void createWord_shouldReturnWordDto() {
        CreateWordRequest req = new CreateWordRequest();
        req.setText("cat");
        req.setDescription("A furry animal");
        req.setCategoryId("cat1");

        Word w = new Word();
        w.setId("w1");
        w.setText("cat");
        w.setDescription("A furry animal");

        when(service.createWord(any())).thenReturn(w);

        WordDto result = controller.createWord(req);

        assertThat(result.getId()).isEqualTo("w1");
        assertThat(result.getText()).isEqualTo("cat");
        assertThat(result.getDescription()).isEqualTo("A furry animal");
    }

    @Test
    void uploadVideo_shouldReturnWordDto() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", new byte[]{1, 2, 3});

        WordVideo video = new WordVideo();
        video.setVideoUrl("/videos/video.mp4");
        Word w = new Word();
        w.setId("w1");
        w.setText("cat");
        w.setDescription("A furry animal");
        video.setWord(w);

        when(service.uploadVideo(eq("w1"), any())).thenReturn(video);

        WordDto result = controller.uploadVideo("w1", file);

        assertThat(result.getId()).isEqualTo("w1");
        assertThat(result.getText()).isEqualTo("cat");
        assertThat(result.getVideoUrl()).isEqualTo("/videos/video.mp4");
    }
}
