package com.blog.storage;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.service.LogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConditionalOnProperty(name = "blog.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalObjectStorage.class);

    @Value("${blog.upload.dir}")
    private String uploadDir;
    @Value("${blog.upload.url-prefix}")
    private String urlPrefix;
    @Resource
    private LogService logService;

    private Path uploadRoot;
    private String prefix;

    @PostConstruct
    public void init() {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    @Override
    public String put(String key, byte[] data, String contentType) {
        Path dest = resolve(key);
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, data);
        } catch (IOException e) {
            log.warn("本地文件保存失败 key={}", key, e);
            logService.recordFail("保存文件", "key=" + key, e);
            throw new BizException(ErrorCode.FILE_WRITE_FAILED, e);
        }
        return publicUrl(key);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("本地文件删除失败 key={}", key, e);
            logService.recordFail("删除文件", "key=" + key, e);
            throw new BizException(ErrorCode.FILE_DELETE_FAILED, e);
        }
    }

    @Override
    public String publicUrl(String key) {
        return ObjectStorage.joinUrl(prefix, key);
    }

    @Override
    public String extractKey(String url) {
        return ObjectStorage.stripPrefix(url, prefix);
    }

    private Path resolve(String key) {
        Path file = uploadRoot.resolve(key).normalize();
        if (!file.startsWith(uploadRoot)) {
            throw new BizException(ErrorCode.ILLEGAL_FILE_PATH);
        }
        return file;
    }
}
