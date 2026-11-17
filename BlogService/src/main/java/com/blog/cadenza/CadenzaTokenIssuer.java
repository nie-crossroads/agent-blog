package com.blog.cadenza;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 本机签发音乐平台令牌。根密钥从环境变量 {@code BLOG_CADENZA_ROOT_KEY} 读取，不写进参数。
 * 用法：{@code CadenzaTokenIssuer <平台id> <有效天数>}
 */
public final class CadenzaTokenIssuer {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter EXPIRE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

    private CadenzaTokenIssuer() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("用法: CadenzaTokenIssuer <平台id> <有效天数>");
            System.exit(2);
            return;
        }
        int platformId = parsePositive(args[0], "平台 id");
        int days = parsePositive(args[1], "有效天数");
        if (platformId < CadenzaTokens.MIN_PLATFORM_ID) {
            System.err.println("平台 id 从 32 开始");
            System.exit(2);
            return;
        }
        byte[] rootKey = CadenzaTokens.decodeRootKey(System.getenv("BLOG_CADENZA_ROOT_KEY"));
        if (rootKey.length != 32) {
            System.err.println("根密钥无效，需要环境变量 BLOG_CADENZA_ROOT_KEY（32 字节的 Base64）");
            System.exit(1);
            return;
        }
        long expireEpochSeconds = Instant.now().plusSeconds(days * 86400L).getEpochSecond();
        String token = CadenzaTokens.issue(rootKey, platformId, expireEpochSeconds);
        int verified = CadenzaTokens.verify(rootKey, token, Set.of(), Instant.now().getEpochSecond());
        if (verified != platformId) {
            System.err.println("签发后校验失败");
            System.exit(1);
            return;
        }
        System.err.println("平台 " + platformId + "，有效至 " + EXPIRE_FORMAT.format(Instant.ofEpochSecond(expireEpochSeconds)));
        System.out.println(token);
    }

    private static int parsePositive(String text, String label) {
        try {
            int value = Integer.parseInt(text);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // 下面统一报错
        }
        System.err.println(label + "无效");
        System.exit(2);
        return 0;
    }
}
