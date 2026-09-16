package com.blog.storage;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.service.LogService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import com.qcloud.cos.transfer.Upload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(name = "blog.storage.type", havingValue = "cos")
public class CosObjectStorage implements ObjectStorage {
    private static final Logger log = LoggerFactory.getLogger(CosObjectStorage.class);
    @Value("${blog.cos.secret-id:}")
    private String secretId;
    @Value("${blog.cos.secret-key:}")
    private String secretKey;
    @Value("${blog.cos.bucket:}")
    private String bucket;
    @Value("${blog.cos.region:}")
    private String region;
    @Value("${blog.cos.cdn-domain:}")
    private String cdnDomain;
    @Resource
    private LogService logService;

    private COSClient client;
    private TransferManager transferManager;
    private String domain;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(secretKey) || !StringUtils.hasText(bucket) || !StringUtils.hasText(region)) {
            throw new IllegalStateException(ErrorCode.COS_CONFIG_INCOMPLETE.getMessage());
        }
        COSCredentials cred = new BasicCOSCredentials(secretId.trim(), secretKey.trim());
        ClientConfig config = new ClientConfig(new Region(region.trim()));
        config.setHttpProtocol(HttpProtocol.https);
        this.client = new COSClient(cred, config);
        // 公网上传，线程池不宜过大，避免慢网速导致超时。文档推荐同地域内网可用 16/32。
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        this.transferManager = new TransferManager(client, threadPool);
        TransferManagerConfiguration tmConfig = new TransferManagerConfiguration();
        tmConfig.setMultipartUploadThreshold(5 * 1024 * 1024L);
        tmConfig.setMinimumUploadPartSize(1 * 1024 * 1024L);
        this.transferManager.setConfiguration(tmConfig);
        this.domain = StringUtils.hasText(cdnDomain)
                ? cdnDomain.trim()
                : "https://" + bucket.trim() + ".cos." + region.trim() + ".myqcloud.com";
        log.info("腾讯 COS 已启用 bucket={} region={} domain={}", bucket, region, domain);
    }

    @PreDestroy
    public void shutdown() {
        if (transferManager != null) {
            transferManager.shutdownNow(true);
        }
    }

    @Override
    public String put(String key, byte[] data, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        PutObjectRequest request = new PutObjectRequest(bucket, key, new ByteArrayInputStream(data), metadata);
        try {
            Upload upload = transferManager.upload(request);
            upload.waitForUploadResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("腾讯 COS 上传被中断 key={}", key, e);
            logService.recordFail("上传文件", "key=" + key, e);
            throw new BizException(ErrorCode.COS_UPLOAD_INTERRUPTED, e);
        } catch (CosClientException e) {
            log.warn("腾讯 COS 上传失败 key={}", key, e);
            logService.recordFail("上传文件", "key=" + key, e);
            throw new BizException(ErrorCode.COS_UPLOAD_FAILED, e);
        }
        return publicUrl(key);
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(bucket, key);
        } catch (CosClientException e) {
            log.warn("腾讯 COS 删除失败 key={}", key, e);
            logService.recordFail("删除文件", "key=" + key, e);
            throw new BizException(ErrorCode.COS_DELETE_FAILED, e);
        }
    }

    @Override
    public void deleteAll(List<String> keys) {
        List<String> unique = ObjectStorage.distinctKeys(keys);
        if (unique.isEmpty()) {
            return;
        }
        if (unique.size() == 1) {
            delete(unique.get(0));
            return;
        }
        for (int from = 0; from < unique.size(); from += ObjectStorage.MAX_DELETE_BATCH) {
            deleteChunk(unique.subList(from, Math.min(from + ObjectStorage.MAX_DELETE_BATCH, unique.size())));
        }
    }

    private void deleteChunk(List<String> keys) {
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucket);
        List<DeleteObjectsRequest.KeyVersion> keyList = new ArrayList<>(keys.size());
        for (String key : keys) {
            keyList.add(new DeleteObjectsRequest.KeyVersion(key));
        }
        request.setKeys(keyList);
        try {
            client.deleteObjects(request);
        } catch (MultiObjectDeleteException e) {
            List<String> failed = new ArrayList<>();
            if (e.getErrors() != null) {
                for (MultiObjectDeleteException.DeleteError error : e.getErrors()) {
                    log.warn("腾讯 COS 批量删除失败 key={} code={} msg={}", error.getKey(), error.getCode(), error.getMessage());
                    if (error.getKey() != null) {
                        failed.add(error.getKey());
                    }
                }
            }
            String detail = failed.isEmpty() ? "keys=" + String.join(",", keys) : "keys=" + String.join(",", failed);
            logService.recordFail("删除文件", detail, e);
            throw new BizException(ErrorCode.COS_DELETE_FAILED, e);
        } catch (CosClientException e) {
            log.warn("腾讯 COS 批量删除失败 keys={}", keys, e);
            logService.recordFail("删除文件", "keys=" + String.join(",", keys), e);
            throw new BizException(ErrorCode.COS_DELETE_FAILED, e);
        }
    }

    @Override
    public String publicUrl(String key) {
        return ObjectStorage.joinUrl(domain, key);
    }

    @Override
    public String extractKey(String url) {
        String key = ObjectStorage.stripPrefix(url, domain);
        if (key != null) {
            return key;
        }
        String cosHost = bucket + ".cos." + region + ".myqcloud.com/";
        int idx = url == null ? -1 : url.indexOf(cosHost);
        if (idx >= 0) {
            String path = url.substring(idx + cosHost.length());
            int query = path.indexOf('?');
            return query >= 0 ? path.substring(0, query) : path;
        }
        return null;
    }
}
