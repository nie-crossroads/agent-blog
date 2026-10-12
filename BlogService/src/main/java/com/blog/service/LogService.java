package com.blog.service;

import com.blog.common.PageQuery;
import com.blog.common.PageResult;
import com.blog.entity.BlogLog;
import com.blog.mapper.BlogLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class LogService {
    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private static final ThreadLocal<Integer> BOUND_CONSUMER = new ThreadLocal<>();

    @Resource
    private BlogLogMapper blogLogMapper;

    public static int currentConsumer() {
        Integer bound = BOUND_CONSUMER.get();
        if (bound != null) {
            return bound;
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String uri = request.getRequestURI();
            if (uri != null && uri.contains("/api/front")) {
                return 0;
            }
        }
        return 1;
    }

    public static void runBound(int consumer, Runnable task) {
        Integer previous = BOUND_CONSUMER.get();
        BOUND_CONSUMER.set(consumer);
        try {
            task.run();
        } finally {
            if (previous == null) {
                BOUND_CONSUMER.remove();
            } else {
                BOUND_CONSUMER.set(previous);
            }
        }
    }

    public PageResult<BlogLog> page(PageQuery query) {
        return new PageResult<>(blogLogMapper.countPage(query), blogLogMapper.selectPage(query));
    }

    public void record(String event, String status, String detail) {
        BlogLog row = new BlogLog();
        row.setEvent(event);
        row.setStatus(status);
        row.setExeTime(0);
        row.setDetail(clip(detail));
        row.setConsumer(currentConsumer());
        blogLogMapper.insert(row);
    }

    public void recordFail(String event, String detail) {
        writeFail(event, detail);
    }

    public void recordFail(String event, Throwable error) {
        writeFail(event, describe(error));
    }

    public void recordFail(String event, String hint, Throwable error) {
        writeFail(event, join(hint, describe(error)));
    }

    private void writeFail(String event, String detail) {
        try {
            record(event, "失败", detail);
        } catch (Exception e) {
            log.warn("写入运行日志失败", e);
        }
    }

    static String describe(Throwable error) {
        if (error == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = error;
        for (int depth = 0; current != null && depth < 3; depth++) {
            if (depth > 0) {
                builder.append(" <- ");
            }
            builder.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (StringUtils.hasText(message)) {
                builder.append(": ").append(sanitize(message.replaceAll("\\s+", " ").trim()));
            }
            Throwable cause = current.getCause();
            if (cause == null || cause == current) {
                break;
            }
            current = cause;
        }
        return builder.isEmpty() ? error.getClass().getSimpleName() : builder.toString();
    }

    private static String sanitize(String message) {
        String value = message.replaceAll(
                "(?i)((?:password|passwd|secret|token|authorization|api[_-]?key)\\s*[=:]\\s*)[^\\s,;]+",
                "$1*");
        value = value.replaceAll("(?i)([?&]key=)[^&\\s]+", "$1*");
        return value.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "*@*");
    }

    private static String join(String hint, String detail) {
        if (!StringUtils.hasText(hint)) {
            return detail;
        }
        if (!StringUtils.hasText(detail)) {
            return hint;
        }
        return hint + " | " + detail;
    }

    private static String clip(String detail) {
        if (detail == null) {
            return null;
        }
        String value = sanitize(detail);
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
