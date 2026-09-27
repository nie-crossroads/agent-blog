package com.blog.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

@Component
public class AvifCompressor {
    private static final Logger log = LoggerFactory.getLogger(AvifCompressor.class);
    private static final int MAX_EDGE = 1600;
    private static final String[] CODECS = { "libaom-av1", "libsvtav1" };

    @Resource
    private LogService logService;

    /**
     * ffmpeg 原生库初始化失败后永久关闭 AVIF。
     * 原因：NoClassDefFoundError / UnsatisfiedLinkError 属于 Error，catch (Exception) 拦不住，
     * 若不熔断，之后每次上传都会重新触发一次原生库加载失败。
     */
    private final AtomicBoolean broken = new AtomicBoolean(false);

    public boolean isBroken() {
        return broken.get();
    }

    public byte[] compressToAvif(byte[] original) {
        if (broken.get()) {
            return null;
        }
        Path tmp = null;
        try {
            tmp = Files.createTempFile("blog-avif-", ".avif");
            if (!compressToAvif(original, tmp)) {
                return null;
            }
            return Files.readAllBytes(tmp);
        } catch (Throwable t) {
            onFailure(t);
            return null;
        } finally {
            deleteQuietly(tmp);
        }
    }

    public boolean compressToAvif(byte[] original, Path dest) {
        BufferedImage src = read(original);
        if (src == null) {
            return false;
        }
        try {
            BufferedImage scaled = toBgr(scale(src));
            Throwable last = null;
            for (String codec : CODECS) {
                try {
                    encode(scaled, dest, codec);
                    if (Files.exists(dest) && Files.size(dest) > 0) {
                        return true;
                    }
                } catch (Throwable t) {
                    last = t;
                    Files.deleteIfExists(dest);
                }
            }
            if (last != null) {
                onFailure(last);
            }
            return false;
        } catch (Throwable t) {
            onFailure(t);
            return false;
        }
    }

    /**
     * 纯 Java 兜底方案：AVIF 不可用时用它产出 JPEG 缩略图，保证上传不会退化成"直接用原图"。
     * 依赖只有 thumbnailator + JDK 自带 ImageIO，无原生库。
     */
    public byte[] compressToJpeg(byte[] original) {
        BufferedImage src = read(original);
        if (src == null) {
            return null;
        }
        try {
            BufferedImage scaled = toBgr(scale(src));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(scaled, "jpg", out)) {
                log.warn("jpeg 兜底压缩失败：无可用 ImageWriter");
                return null;
            }
            return out.toByteArray();
        } catch (Throwable t) {
            log.warn("jpeg 兜底压缩失败: {}", t.getMessage());
            return null;
        }
    }

    /**
     * 启动时探一次 ffmpeg 原生库：失败就提前熔断，运维在启动日志里就能看到 AVIF 不可用，
     * 不用等到第一次上传才发现问题。
     */
    @PostConstruct
    public void init() {
        try {
            Class.forName("org.bytedeco.ffmpeg.global.avutil");
            log.info("AVIF 压缩可用：ffmpeg 原生库加载成功");
        } catch (Throwable t) {
            onFailure(t);
        }
    }

    private void onFailure(Throwable t) {
        if (t instanceof Error || t instanceof ClassNotFoundException) {
            if (broken.compareAndSet(false, true)) {
                log.error("AVIF 压缩不可用，已在本进程内永久关闭，后续图片上传回退为 JPEG 缩略图。"
                        + "常见原因：① /tmp 挂载 noexec，javacpp 无法加载 .so；"
                        + "② 服务器缺少 libstdc++ / libgomp；③ glibc 版本过低。"
                        + "完整堆栈如下（只打印一次）", t);
                logService.recordFail("压缩图片", "AVIF 原生库初始化失败，已回退 JPEG", t);
            }
            return;
        }
        log.warn("avif compress failed: {}", t.getMessage());
        logService.recordFail("压缩图片", t);
    }

    private BufferedImage read(byte[] original) {
        try {
            return ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            log.warn("读取图片失败");
            return null;
        }
    }

    private BufferedImage scale(BufferedImage src) throws IOException {
        if (src.getWidth() <= MAX_EDGE && src.getHeight() <= MAX_EDGE) {
            return src;
        }
        return Thumbnails.of(src).size(MAX_EDGE, MAX_EDGE).asBufferedImage();
    }

    private BufferedImage toBgr(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return src;
        }
        BufferedImage bgr = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bgr.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return bgr;
    }

    private void encode(BufferedImage src, Path dest, String codec) throws Exception {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(dest.toFile(), src.getWidth(), src.getHeight());
        recorder.setFormat("avif");
        recorder.setVideoCodecName(codec);
        recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(1);
        recorder.setVideoOption("crf", "32");
        recorder.setVideoOption("cpu-used", "6");
        recorder.setVideoOption("still-picture", "1");
        recorder.setVideoOption("usage", "allintra");
        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            recorder.start();
            recorder.record(converter.convert(src));
            recorder.stop();
        } finally {
            recorder.release();
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除临时文件失败");
        }
    }
}
