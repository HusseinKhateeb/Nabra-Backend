package com.nabra.backend.modules.visualdictionary.controller;

import com.nabra.backend.modules.visualdictionary.dto.CategoryWithWordsDto;
import com.nabra.backend.modules.visualdictionary.dto.WordDto;
import com.nabra.backend.modules.visualdictionary.service.VisualDictionaryService;
import com.nabra.backend.security.principal.UserPrincipal;
import com.nabra.backend.modules.usermanagement.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisualDictionaryControllerTest {

    @Mock
    private VisualDictionaryService service;

    private VisualDictionaryController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new VisualDictionaryController(service);
        User user = new User();
        user.setId("u1");
        principal = new UserPrincipal(user);
    }

    @Test
    void categories_shouldReturnList() {
        CategoryWithWordsDto dto = new CategoryWithWordsDto();
        dto.setId("cat1");
        dto.setName("Animals");
        when(service.getCategoriesWithWords()).thenReturn(List.of(dto));

        List<CategoryWithWordsDto> result = controller.categories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("cat1");
    }

    @Test
    void deleteCategory_shouldReturnNoContent() {
        ResponseEntity<?> response = controller.deleteCategory("cat1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(service).deleteCategory("cat1");
    }

    @Test
    void deleteCategory_whenIllegalState_shouldReturnBadRequest() {
        doThrow(new IllegalStateException("Category has words")).when(service).deleteCategory("cat1");

        ResponseEntity<?> response = controller.deleteCategory("cat1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void getWords_shouldReturnList() {
        WordDto dto = new WordDto();
        dto.setId("w1");
        dto.setText("cat");
        when(service.getWords("cat1", "u1")).thenReturn(List.of(dto));

        List<WordDto> result = controller.getWords("cat1", principal);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("w1");
    }

    @Test
    void getWords_withNullPrincipal_shouldPassNullUserId() {
        WordDto dto = new WordDto();
        dto.setId("w1");
        when(service.getWords("cat1", null)).thenReturn(List.of(dto));

        List<WordDto> result = controller.getWords("cat1", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void addFavorite_shouldCallService() {
        controller.addFavorite(principal, "w1");

        verify(service).addFavorite("u1", "w1");
    }

    @Test
    void removeFavorite_shouldCallService() {
        controller.removeFavorite(principal, "w1");

        verify(service).removeFavorite("u1", "w1");
    }

    @Test
    void getFavorites_shouldReturnList() {
        WordDto dto = new WordDto();
        dto.setId("w1");
        dto.setText("cat");
        when(service.getFavorites("u1")).thenReturn(List.of(dto));

        List<WordDto> result = controller.getFavorites(principal);

        assertThat(result).hasSize(1);
    }

    @Test
    void deleteWord_shouldReturnNoContent() {
        ResponseEntity<?> response = controller.deleteWord("w1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(service).deleteWord("w1");
    }

    @Test
    void deleteWord_whenError_shouldReturnBadRequest() {
        doThrow(new IllegalArgumentException("Word not found")).when(service).deleteWord("w1");

        ResponseEntity<?> response = controller.deleteWord("w1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}
