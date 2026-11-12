import {ElMessage, ElMessageBox} from "element-plus";

// ============================================================================
// Access Token：只放在这个模块变量里（内存），不写 localStorage / sessionStorage。
// 不直接 export 变量本身，而是提供读写函数：import 进来的绑定是只读的，
// 外部想改只能走 setAccessToken，赋值入口唯一，出问题好排查。
// ============================================================================
let accessToken = null;

export function getAccessToken() {
    return accessToken;
}

export function setAccessToken(token) {
    accessToken = token || null;
}


export function removeToken() {
    accessToken = null;
    // 令牌清除的同时清除权限缓存：登出、异地踢下线、令牌失效都走此函数，
    // 单点收口确保不残留上一个账号的权限码（防串号）。函数声明有提升，后置定义可安全调用。
    clearPermissionCache();
}
// ============================================================================
// 权限缓存（解决 v-hasPermission 指令 N+1 请求风暴）
// ----------------------------------------------------------------------------
// 旧实现：指令每绑定一个按钮就 doGet("/api/login/info") 拉一次全量用户信息，
// 一个页面十几个权限按钮 = 十几个并发请求，且都在指令 mounted/updated 时触发。
// 新实现：登录态内首次拉取一次并缓存到 sessionStorage，指令只读缓存；
// 退出登录 / 切换账号时 clearPermissionCache() 清除，防串号。
// 用 sessionStorage 而非 localStorage：会话级隔离，关标签页自动失效，天然防跨账号污染。
// ============================================================================
const PERMISSION_CACHE_KEY = "apex_sales_permission_cache";

/**
 * 写入权限缓存（含按钮权限码与角色），由登录后首次拉取 /api/login/info 时调用。
 * @param {{ permissionList: string[], roleList?: string[], userId?: number }} info
 */
export function setPermissionCache(info) {
    try {
        window.sessionStorage.setItem(PERMISSION_CACHE_KEY, JSON.stringify({
            permissionList: info.permissionList || [],
            roleList: info.roleList || [],
            userId: info.userId ?? null,
        }));
    } catch (e) {
        // 序列化或存储异常不应阻断渲染，降级为无缓存（指令会再次拉取）
    }
}

/**
 * 读取权限缓存；未命中返回 null（调用方据此决定是否发起拉取）。
 * @returns {{ permissionList: string[], roleList: string[], userId: number|null } | null}
 */
export function getPermissionCache() {
    try {
        const raw = window.sessionStorage.getItem(PERMISSION_CACHE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch (e) {
        // 缓存被写坏（如手工篡改）时清掉，走重新拉取
        window.sessionStorage.removeItem(PERMISSION_CACHE_KEY);
        return null;
    }
}

/**
 * 清除权限缓存：退出登录、切换账号、令牌失效时必须调用，防止上一个账号的权限残留。
 */
export function clearPermissionCache() {
    window.sessionStorage.removeItem(PERMISSION_CACHE_KEY);
}

export function messageFrame(msg) {
   return ElMessageBox.confirm(
        msg,//消息提示
        'Warning',
        {
            confirmButtonText: 'OK',
            cancelButtonText: 'Cancel',
            type: 'warning',
        }
    );
}

/**
 * 封装返回函数
 *
 */
export function goBack() {
    window.history.back()
}

