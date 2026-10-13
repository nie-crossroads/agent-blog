package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.ImageUrls;
import com.blog.dto.UploadResult;
import com.blog.storage.ObjectStorage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class MiscService {
    private static final Logger log = LoggerFactory.getLogger(MiscService.class);

    @Resource
    private AvifCompressor avifCompressor;
    @Resource
    private ObjectStorage objectStorage;
    @Resource
    private LogService logService;

    @Value("${blog.storage.prefix}")
    private String storagePrefix;
    @Value("${blog.upload.dir}")
    private String uploadDir;
    @Value("${blog.upload.url-prefix}")
    private String urlPrefix;

    private Path uploadRoot;

    @PostConstruct
    public void initUploadRoot() {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("当前文件存储: {}", objectStorage.getClass().getSimpleName());
    }

    public UploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_REQUIRED);
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        String stem = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String name = stem + ext;
        String key = objectKey(name);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("读取上传文件失败", e);
            logService.recordFail("上传文件", e);
            throw new BizException(ErrorCode.FILE_READ_FAILED, e);
        }
        String url = clipUrl(objectStorage.put(key, bytes, contentType(file, ext)));
        String thumbnailUrl = url;
        if (isGif(ext, bytes)) {
            return UploadResult.of(url, url);
        }
        String thumbName = ".avif".equals(ext) ? stem + "_t.avif" : stem + ".avif";
        String thumbType = "image/avif";
        byte[] thumb = avifCompressor.compressToAvif(bytes);
        if (thumb == null || thumb.length == 0) {
            thumbName = ".jpg".equals(ext) ? stem + "_t.jpg" : stem + ".jpg";
            thumbType = "image/jpeg";
            thumb = avifCompressor.compressToJpeg(bytes);
        }
        if (thumb != null && thumb.length > 0) {
            thumbnailUrl = clipUrl(objectStorage.put(objectKey(thumbName), thumb, thumbType));
        }
        return UploadResult.of(url, thumbnailUrl);
    }

    private static boolean isGif(String ext, byte[] bytes) {
        if (".gif".equals(ext)) {
            return true;
        }
        if (bytes == null || bytes.length < 6) {
            return false;
        }
        return bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';
    }

    public void tryDeleteFiles(String... urls) {
        if (urls == null) {
            return;
        }
        tryDeleteFiles(Arrays.asList(urls));
    }

    public void tryDeleteFiles(Collection<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        List<String> leftovers = new ArrayList<>();
        for (String url : urls) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            String key = objectStorage.extractKey(url);
            if (StringUtils.hasText(key)) {
                keys.add(key);
            } else {
                leftovers.add(url);
            }
        }
        if (!keys.isEmpty()) {
            try {
                objectStorage.deleteAll(new ArrayList<>(keys));
            } catch (Exception e) {
                log.warn("批量删除文件失败 keys={}", keys, e);
                logService.recordFail("删除文件", String.join(",", keys), e);
            }
        }
        for (String url : leftovers) {
            try {
                deleteLegacyLocal(url);
            } catch (Exception e) {
                log.warn("删除文件失败 url={}", url, e);
                logService.recordFail("删除文件", url, e);
            }
        }
    }

    public void tryDeleteFile(String url) {
        tryDeleteFiles(url);
    }

    private void deleteLegacyLocal(String url) throws IOException {
        if (!url.startsWith(urlPrefix)) {
            return;
        }
        String relative = url.substring(urlPrefix.length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        Path file = uploadRoot.resolve(relative).normalize();
        if (!file.startsWith(uploadRoot)) {
            return;
        }
        Files.deleteIfExists(file);
    }

    private String objectKey(String filename) {
        LocalDate now = LocalDate.now();
        int quarter = (now.getMonthValue() - 1) / 3 + 1;
        String prefix = storagePrefix.trim();
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/" + now.getYear() + "/Q" + quarter + "/" + filename;
    }

    private static String contentType(MultipartFile file, String ext) {
        String type = file.getContentType();
        if (StringUtils.hasText(type) && !"application/octet-stream".equals(type)) {
            return type;
        }
        return switch (ext) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".avif" -> "image/avif";
            case ".svg" -> "image/svg+xml";
            case ".mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    public String clipUrl(String url) {
        return ImageUrls.clip(url);
    }
}
