package com.cyk.constants;

/**
 * 常量类
 *
 */
public class Constants {

    public static final String LOGIN_URI = "/api/login";

    //redis的key的命名规范： 项目名:模块名:功能名:唯一业务参数(比如用户id)
    public static final String REDIS_JWT_KEY = "dlyk:user:login:";

    //redis中负责人的key
    public static final String REDIS_OWNER_KEY = "dlyk:user:owner";

    // 负责人列表缓存的兜底过期时间（分钟）：即使有绕过应用的直连数据库修改（或漏删缓存），
    // 缓存也会到期自动失效并由 Cache-Aside 回源重建，保证最终一致，而非永久脏数据
    public static final Long REDIS_OWNER_KEY_EXPIRE_MINUTES = 10L;

    // 登录失败计数器的 Redis Key 前缀
    public static final String REDIS_LOGIN_FAIL_KEY = "dlyk:login:fail:";

    // 登录锁的 Redis Key 前缀
    public static final String REDIS_LOGIN_LOCK_KEY = "dlyk:login:lock:";

    // 登录失败计数器的有效期：10分钟
    public static final Long LOGIN_FAIL_EXPIRE_MINUTES = 10L;

    // 账号锁定的有效期：30分钟
    public static final Long LOGIN_LOCK_EXPIRE_MINUTES = 30L;

    // 忘记密码限流防刷 limit 的 Key 前缀
    public static final String REDIS_RESET_PWD_LIMIT_KEY = "dlyk:reset_pwd:limit:";

    // 忘记密码缓存验证码的 Key 前缀
    public static final String REDIS_RESET_PWD_CODE_KEY = "dlyk:reset_pwd:code:";

    // 忘记密码输错次数计数器的 Key 前缀
    public static final String REDIS_RESET_PWD_FAIL_KEY = "dlyk:reset_pwd:fail_count:";

    // 重置密码验证码有效期：300秒（5分钟）
    public static final Long RESET_PWD_CODE_EXPIRE_SECONDS = 300L;

    // 重置密码发送验证码的防刷间隔：60秒
    public static final Long RESET_PWD_LIMIT_EXPIRE_SECONDS = 60L;

    // 重置密码验证码允许的最大错误尝试次数
    public static final Long RESET_PWD_MAX_FAIL_COUNT = 5L;

    //jwt过期时间7天
    public static final Long EXPIRE_TIME = 7 * 24 * 60 * 60L;

    //jwt过期时间60分钟
    public static final Long DEFAULT_EXPIRE_TIME = 60 * 60L;

    //分页时每页显示10条数据
    public static final int PAGE_SIZE = 10;

    //请求token的名称
    public static final String TOKEN_NAME = "Authorization";

    public static final String EMPTY = "";

    //导出Excel的接口路径
    public static final String EXPORT_EXCEL_URI = "/api/exportExcel";

    public static final String EXCEL_FILE_NAME = "客户信息数据";

    // ------------------------------------------------------------------
    // AI 领航员增值付费体系
    // ------------------------------------------------------------------

    /** 角色标识：系统管理员（决定 AI 助手挂载管理员工具包） */
    public static final String ROLE_ADMIN = "admin";

    /** 已开通的增值能力 Redis Key 前缀（完整 key = 前缀 + userId + ":" + abilityKey） */
    public static final String REDIS_AI_PREMIUM_ABILITY_KEY = "dlyk:ai:premium:";

    /** 未开通能力的负缓存 Key 后缀（防止缓存穿透：未付费请求不重复打库） */
    public static final String REDIS_AI_PREMIUM_ABILITY_DENY_SUFFIX = ":deny";

    /**
     * 负缓存有效期（秒）。
     * 取值权衡：太长会导致"刚付款用户短时间内仍被误拦"的体验窗口，
     * 太短则防穿透效果差；60 秒是常见生产折中值（配合开通时主动清除负缓存，
     * 实际新开通用户不会被误拦）。
     */
    public static final Long AI_PREMIUM_ABILITY_DENY_EXPIRE_SECONDS = 60L;

    /** AI 增值能力开通记录的默认有效期（秒）：30 天 */
    public static final Long AI_PREMIUM_ABILITY_EXPIRE_SECONDS = 30L * 24 * 60 * 60;

    /** AI 支付订单流水号前缀 */
    public static final String AI_PAYMENT_ORDER_NO_PREFIX = "AI";

    /** AI 工具生成的 Excel 文件输出目录（相对工作目录，供文件下载端点读取） */
    public static final String AI_EXPORT_DIR = "target/ai-export";

    /** AI 批量导入用户时未提供密码的统一初始密码 */
    public static final String AI_IMPORT_DEFAULT_PASSWORD = "Dlyk@2026";

}
