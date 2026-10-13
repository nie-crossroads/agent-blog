package com.blog.storage;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.service.LogService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.util.Auth;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "blog.storage.type", havingValue = "qiniu")
public class QiniuObjectStorage implements ObjectStorage {
    private static final Logger log = LoggerFactory.getLogger(QiniuObjectStorage.class);

    @Value("${blog.qiniu.access-key}")
    private String accessKey;
    @Value("${blog.qiniu.secret-key}")
    private String secretKey;
    @Value("${blog.qiniu.bucket}")
    private String bucket;
    @Value("${blog.qiniu.cdn-domain}")
    private String cdnDomain;
    @Value("${blog.qiniu.region}")
    private String region;
    @Resource
    private LogService logService;

    private Auth auth;
    private UploadManager uploadManager;
    private BucketManager bucketManager;
    private String domain;

    @PostConstruct
    public void init() {
        Configuration cfg = new Configuration(resolveRegion(region));
        this.auth = Auth.create(accessKey.trim(), secretKey.trim());
        this.uploadManager = new UploadManager(cfg);
        this.bucketManager = new BucketManager(auth, cfg);
        this.domain = cdnDomain.trim();
    }

    @Override
    public String put(String key, byte[] data, String contentType) {
        try {
            String token = auth.uploadToken(bucket);
            Response resp = uploadManager.put(data, key, token, null, contentType, false);
            if (resp == null || !resp.isOK()) {
                String detail = resp == null
                        ? "key=" + key + " 七牛云返回为空"
                        : "key=" + key + " status=" + resp.statusCode + " " + resp.error;
                log.warn("七牛云上传失败 {}", detail);
                logService.recordFail("上传文件", detail);
                throw new BizException(ErrorCode.QINIU_UPLOAD_FAILED);
            }
            return publicUrl(key);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("七牛云上传失败 key={}", key, e);
            logService.recordFail("上传文件", "key=" + key, e);
            throw new BizException(ErrorCode.QINIU_UPLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            bucketManager.delete(bucket, key);
        } catch (Exception e) {
            log.warn("七牛云删除失败 key={}", key, e);
            logService.recordFail("删除文件", "key=" + key, e);
            throw new BizException(ErrorCode.QINIU_DELETE_FAILED, e);
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
        BucketManager.BatchOperations ops = new BucketManager.BatchOperations();
        ops.addDeleteOp(bucket, keys.toArray(String[]::new));
        try {
            Response resp = bucketManager.batch(ops);
            if (resp == null || (resp.statusCode != 200 && resp.statusCode != 298)) {
                String detail = resp == null
                        ? "keys=" + String.join(",", keys) + " 七牛云返回为空"
                        : "keys=" + String.join(",", keys) + " status=" + resp.statusCode + " " + resp.error;
                log.warn("七牛云批量删除失败 {}", detail);
                logService.recordFail("删除文件", detail);
                throw new BizException(ErrorCode.QINIU_DELETE_FAILED);
            }
            applyBatchStatuses(keys, parseStatuses(resp));
        } catch (QiniuException e) {
            if (e.response != null && (e.response.statusCode == 200 || e.response.statusCode == 298)) {
                try {
                    applyBatchStatuses(keys, parseStatuses(e.response));
                    return;
                } catch (QiniuException parseError) {
                    e = parseError;
                }
            }
            log.warn("七牛云批量删除失败 keys={}", keys, e);
            logService.recordFail("删除文件", "keys=" + String.join(",", keys), e);
            throw new BizException(ErrorCode.QINIU_DELETE_FAILED, e);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("七牛云批量删除失败 keys={}", keys, e);
            logService.recordFail("删除文件", "keys=" + String.join(",", keys), e);
            throw new BizException(ErrorCode.QINIU_DELETE_FAILED, e);
        }
    }

    private static BatchStatus[] parseStatuses(Response resp) throws QiniuException {
        if (resp == null) {
            return new BatchStatus[0];
        }
        BatchStatus[] statuses = resp.jsonToObject(BatchStatus[].class);
        return statuses == null ? new BatchStatus[0] : statuses;
    }

    private void applyBatchStatuses(List<String> keys, BatchStatus[] statuses) {
        List<String> failed = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            BatchStatus status = i < statuses.length ? statuses[i] : null;
            int code = status == null ? 0 : status.code;
            if (code == 200 || code == 612) {
                continue;
            }
            String err = status != null && status.data != null ? status.data.error : "unknown";
            log.warn("七牛云批量删除失败 key={} code={} error={}", keys.get(i), code, err);
            failed.add(keys.get(i));
        }
        if (!failed.isEmpty()) {
            logService.recordFail("删除文件", "keys=" + String.join(",", failed));
            throw new BizException(ErrorCode.QINIU_DELETE_FAILED);
        }
    }

    @Override
    public String publicUrl(String key) {
        return ObjectStorage.joinUrl(domain, key);
    }

    @Override
    public String extractKey(String url) {
        return ObjectStorage.stripPrefix(url, domain);
    }

    private static Region resolveRegion(String value) {
        String code = value == null ? "auto" : value.trim().toLowerCase();
        return switch (code) {
            case "z0", "huadong" -> Region.huadong();
            case "z1", "huabei" -> Region.huabei();
            case "z2", "huanan" -> Region.huanan();
            case "na0", "beimei" -> Region.beimei();
            case "as0", "xinjiapo", "singapore" -> Region.xinjiapo();
            default -> Region.autoRegion();
        };
    }
}
