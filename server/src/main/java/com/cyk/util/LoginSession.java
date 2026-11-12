package com.cyk.util;

/**
 * Redis 中保存的"当前有效会话"：sid + 当前有效的 Refresh 令牌 jti。
 *
 * <p>存储格式为 "sid:jti"。UUID 的字符集是十六进制加连字符，不含冒号，所以按冒号拆分是安全的。</p>
 *
 * <ul>
 *   <li>sid：登录时生成，整个会话期间不变。Access 和 Refresh 都带它，用于"单设备互斥"：
 *       别处重新登录会生成新的 sid，旧设备手里的令牌 sid 对不上，立刻被识别为被顶下线。</li>
 *   <li>jti：每次刷新都换新。用于"重放检测"：旧 Refresh 的 jti 对不上，说明它已被用过，
 *       很可能被窃取，需要踢掉整个会话。</li>
 * </ul>
 *
 * <p>设计为 record：不可变、自带 equals/hashCode/toString、没有冗余的 getter/setter 样板代码（JDK 16+，
 * 你的项目是 Java 21，可以放心使用）。</p>
 */
public record LoginSession(String sid, String jti) {

    /** 序列化成写入 Redis 的字符串 */
    public String toRedisValue() {
        return sid + ":" + jti;
    }

    /**
     * 从 Redis 读出的字符串还原。格式不合法（null、空串、缺冒号）一律返回 null，
     * 调用方按"会话不存在"处理，而不是抛异常，避免脏数据把请求打成 500。
     */
    public static LoginSession parse(String redisValue) {
        if (redisValue == null) {
            return null;
        }
        int idx = redisValue.indexOf(':');
        if (idx <= 0 || idx == redisValue.length() - 1) {
            return null;
        }
        return new LoginSession(redisValue.substring(0, idx), redisValue.substring(idx + 1));
    }
}