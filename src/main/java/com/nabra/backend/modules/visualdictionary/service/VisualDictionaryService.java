
package com.nabra.backend.modules.visualdictionary.service;

import com.nabra.backend.modules.visualdictionary.dto.CategoryWithWordsDto;
import com.nabra.backend.modules.visualdictionary.dto.*;
import com.nabra.backend.modules.visualdictionary.model.*;
import com.nabra.backend.modules.visualdictionary.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VisualDictionaryService {
    // Thread pool for background video processing
    private static final ExecutorService videoExecutor = Executors.newSingleThreadExecutor();

    private final CategoryRepository categoryRepo;
    private final WordRepository wordRepo;
    private final WordVideoRepository videoRepo;
    private final FavoriteWordRepository favoriteRepo;

    private static final String VIDEO_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator
            + "videos" + File.separator;

    /*
     * =====================
     * USER
     * =====================
     */

    public List<CategoryWithWordsDto> getCategoriesWithWords() {
        return categoryRepo.findAll().stream().map(c -> {
            CategoryWithWordsDto dto = new CategoryWithWordsDto();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setIcon(c.getIcon());
            List<WordDto> words = wordRepo.findByCategoryId(c.getId()).stream().map(w -> {
                WordDto wd = new WordDto();
                wd.setId(w.getId());
                wd.setText(w.getText());
                wd.setDescription(w.getDescription());
                wd.setVideoUrl(w.getVideo() != null ? w.getVideo().getVideoUrl() : null);
                wd.setFavorite(false); // Not relevant here
                return wd;
            }).collect(Collectors.toList());
            dto.setWords(words);
            return dto;
        }).collect(Collectors.toList());
    }

    public List<WordDto> getWords(String categoryId, String userId) {
        return wordRepo.findByCategoryId(categoryId).stream().map(w -> {
            WordDto dto = new WordDto();
            dto.setId(w.getId());
            dto.setText(w.getText());
            dto.setDescription(w.getDescription());
            dto.setVideoUrl(w.getVideo() != null ? w.getVideo().getVideoUrl() : null);

            dto.setFavorite(
                    userId != null &&
                            favoriteRepo.existsByUserIdAndWord_Id(userId, w.getId()));

            return dto;
        }).collect(Collectors.toList());
    }

    /*
     * =====================
     * FAVORITES ⭐
     * =====================
     */

    public void addFavorite(String userId, String wordId) {
        if (!favoriteRepo.existsByUserIdAndWord_Id(userId, wordId)) {
            Word word = wordRepo.findById(wordId)
                    .orElseThrow(() -> new RuntimeException("Word not found"));

            FavoriteWord fav = new FavoriteWord();
            fav.setUserId(userId);
            fav.setWord(word);

            favoriteRepo.save(fav);
        }
    }

    public void removeFavorite(String userId, String wordId) {
        favoriteRepo.findByUserId(userId).stream()
                .filter(f -> f.getWord().getId().equals(wordId))
                .findFirst()
                .ifPresent(favoriteRepo::delete);
    }

    public List<WordDto> getFavorites(String userId) {
        return favoriteRepo.findByUserId(userId).stream()
                .map(fav -> {
                    Word w = fav.getWord();
                    WordDto dto = new WordDto();
                    dto.setId(w.getId());
                    dto.setText(w.getText());
                    dto.setDescription(w.getDescription());
                    dto.setVideoUrl(
                            w.getVideo() != null ? w.getVideo().getVideoUrl() : null);
                    dto.setFavorite(true);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /*
     * =====================
     * ADMIN
     * =====================
     */

    public Category createCategory(CreateCategoryRequest req) {
        Category c = new Category();
        c.setName(req.getName());
        c.setIcon(req.getIcon());
        return categoryRepo.save(c);
    }

    public Word createWord(CreateWordRequest req) {
        Category category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Word w = new Word();
        w.setText(req.getText());
        w.setDescription(req.getDescription());
        w.setCategory(category);

        return wordRepo.save(w);
    }

    public WordVideo uploadVideo(String wordId, MultipartFile file) {
        try {
            File dir = new File(VIDEO_DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create directory: " + dir.getAbsolutePath());
                }
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(dir, fileName);
            System.out.println("Saving video to: " + dest.getAbsolutePath());
            file.transferTo(dest);

            Word word = wordRepo.findById(wordId).orElseThrow();

            // Always return the final URL that will be used after encoding
            WordVideo video = new WordVideo();
            video.setWord(word);
            video.setVideoUrl("/videos/" + fileName); // This will be the final URL after encoding
            WordVideo savedVideo = videoRepo.save(video);


            // Run ffprobe/ffmpeg in background
            videoExecutor.submit(() -> {
                try {
                    // Use ffprobe to check codecs
                    String probeCmd = String.format(
                        "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 \"%s\"",
                        dest.getAbsolutePath()
                    );
                    Process probeVideo = Runtime.getRuntime().exec(probeCmd);
                    java.io.BufferedReader vReader = new java.io.BufferedReader(new java.io.InputStreamReader(probeVideo.getInputStream()));
                    String vCodec = vReader.readLine();
                    vReader.close();
                    probeVideo.waitFor();

                    probeCmd = String.format(
                        "ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 \"%s\"",
                        dest.getAbsolutePath()
                    );
                    Process probeAudio = Runtime.getRuntime().exec(probeCmd);
                    java.io.BufferedReader aReader = new java.io.BufferedReader(new java.io.InputStreamReader(probeAudio.getInputStream()));
                    String aCodec = aReader.readLine();
                    aReader.close();
                    probeAudio.waitFor();

                    boolean needsReencode = !("h264".equalsIgnoreCase(vCodec) && "aac".equalsIgnoreCase(aCodec));

                    if (needsReencode) {
                        String encodedFileName = fileName.replace(".mp4", "_encoded.mp4");
                        File encodedDest = new File(dir, encodedFileName);
                        String ffmpegCmd = String.format(
                                "ffmpeg -y -i \"%s\" -vf format=yuv420p -c:v libx264 -profile:v high -level:v 4.0 -c:a aac \"%s\"",
                                dest.getAbsolutePath(), encodedDest.getAbsolutePath());
                        System.out.println("FFmpeg command: " + ffmpegCmd);
                        Process ffmpeg = Runtime.getRuntime().exec(ffmpegCmd);
                        int exitCode = ffmpeg.waitFor();
                        if (exitCode == 0) {
                            dest.delete();
                            File finalDest = new File(dir, fileName);
                            boolean renamed = encodedDest.renameTo(finalDest);
                            if (!renamed) {
                                System.err.println("Failed to rename encoded video to original name.");
                            }
                        } else {
                            System.err.println("FFmpeg failed to re-encode video. Exit code: " + exitCode);
                        }
                    } else {
                        System.out.println("Video already in H.264/AAC format, skipping re-encode.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Respond immediately with the final URL
            // Client should poll or reload to check when the video is ready
            return savedVideo;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteCategory(String categoryId) {
        // Delete all words in the category before deleting the category itself
        List<Word> words = wordRepo.findByCategoryId(categoryId);
        if (!words.isEmpty()) {
            wordRepo.deleteAll(words);
        }
        categoryRepo.deleteById(categoryId);
    }

    public void deleteWord(String wordId) {
        if (!wordRepo.existsById(wordId)) {
            throw new IllegalArgumentException("Word not found");
        }
        wordRepo.deleteById(wordId);
    }
}
