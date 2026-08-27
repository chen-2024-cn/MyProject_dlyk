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

}
