import axios from "axios";
import qs from "qs";
import {getAccessToken, setAccessToken, messageFrame, removeToken} from "@/util/util.js";
import {ElMessage, ElMessageBox} from "element-plus";

axios.defaults.baseURL = "";

// 业务码：账号已在其他设备登录，当前设备被顶下线（与后端 CodeEnum.TOKEN_IS_ELSEWHERE 对齐）
const KICKED_CODE = 905;

// 业务码：Access Token 已过期但会话可能仍有效，应当静默刷新（与后端 CodeEnum.ACCESS_TOKEN_EXPIRED 对齐）
const ACCESS_EXPIRED_CODE = 906;

/**
 * 模块级认证失效弹窗锁（901-905 全通道共用）。
 *
 * 【修复的缺陷】旧版只有 kickedAlertOpen 且仅用于 905 分支；901-904（令牌为空/篡改/
 * 过期/不匹配）分支无任何锁。而页面挂载时会并发发起多个请求（如权限查询 + 列表拉取
 * + 心跳探测），令牌一旦失效它们会几乎同时收到 9xx，于是每个请求各弹一个
 * “是否重新登录”对话框——实测确认页面上会叠出两层以上弹窗，且用户需逐个关闭。
 * 现改为统一单飞：无论多少并发请求失败，只弹一个框；其余静默忽略，
 * 因为第一个弹窗已经承担了全部交互职责（确认跳登录 / 取消关闭）。
 */
let authAlertOpen = false;
export function doGet(url, params) {
    return axios({
        method: "get",
        url: url,
        params: params,
        dataType: "json"  // 建议改为 responseType: 'json'，但非必需
    });
}

export function doPost(url, data) {
    return axios({
        method: "post",
        url: url,
        data: qs.stringify(data),//qs.stringify(data) 正是将 { loginAct: 'admin', loginPwd: 'aaa111' } 转换为 loginAct=admin&loginPwd=aaa111 这一键值对格式
        dataType: "json",
        headers: {
            /**
             * 后端 Spring Security 的 formLogin 期望接收 表单格式（application/x-www-form-urlencoded）的数据，而 axios 默认发送的是 JSON。
             */
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    });
}

// 专门用于发送标准的 JSON POST 请求（用于匹配后端的 @RequestBody 接收）
export function doPostJson(url, data) {
    return axios({
        method: "post",
        url: url,
        data: data, // 纯 JavaScript 对象，Axios 会内部将其转为 JSON 串，自动设置 Content-Type: application/json
        dataType: "json"
    });
}

// 专门用于文件上传的方法
export function doUploadFile(url, formData) {
    return axios({
        method: "post",
        url: url,
        data: formData,
        headers: {
            // 让浏览器自动设置正确的 Content-Type (multipart/form-data)
            'Content-Type': 'multipart/form-data'
        }
    });
}

export function doPut(url, data) {
    return axios({
        method: "put",
        url: url,
        data: data,
        dataType: "json"
    });
}

export function doDelete(url, params) {
    return axios({
        method: "delete",
        url: url,
        params: params,
        dataType: "json"
    });
}

// 下载文件：令牌走请求头（不再拼进 URL），拿到 Blob 后用临时链接触发浏览器保存
export async function doDownload(url, params, fallbackName) {
    let resp;
    try {
        resp = await axios({method: "get", url: url, params: params, responseType: "blob"});
    } catch (e) {
        // 9xx 被拦截器 reject 的是响应对象（不是 Error），弹窗已由拦截器负责，原样抛出
        if (!(e instanceof Error)) throw e;
        throw new Error(e.response ? `下载失败（HTTP ${e.response.status}）` : "网络异常，下载失败");
    }
    // 拦截器把 JSON 错误体还原成了对象，说明后端拒绝了请求（如权限不足）
    if (!(resp.data instanceof Blob)) {
        throw new Error(resp.data?.msg || "下载失败");
    }
    const objectUrl = URL.createObjectURL(resp.data);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = parseFileName(resp.headers["content-disposition"]) || fallbackName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    // 延后释放：立刻 revoke 可能赶在浏览器开始读取之前
    setTimeout(() => URL.revokeObjectURL(objectUrl), 10000);
}

// 从 Content-Disposition 里取文件名（后端用 URLEncoder 编码过，需要解码）
function parseFileName(disposition) {
    const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition || "");
    if (!match) return null;
    try {
        return decodeURIComponent(match[1]);
    } catch (e) {
        return match[1];
    }
}

// ============================================================================
// 令牌刷新：906 静默刷新 + 页面刷新后恢复登录态
// ============================================================================
const REFRESH_URL = "/api/auth/refresh";
const REFRESH_LOCK_NAME = "apex_sales-refresh-lock";

// 刷新专用的“裸”实例。axios.create() 出来的实例不继承全局拦截器：
// 刷新接口自己失败（901~905）时不会再触发弹窗，也不会递归地去刷新自己，
// 失败原因交给调用方决定怎么处理。
// 同源请求浏览器默认会带 Cookie，所以不需要 withCredentials（这是第 1 步改成同源的收益）
const refreshClient = axios.create({
    baseURL: "",
    // 锁是跨标签页持有的：如果这个请求无限挂起，其他标签页的刷新也会被一起堵死
    timeout: 10000
});

let refreshPromise = null;
/**
 * 刷新 Access Token。返回 { ok: true } 或 { ok: false, data }，data 是刷新接口返回的 body
 * （网络故障时为 null）。同一时刻最多只有一个刷新请求在飞，并发调用者共用同一个 Promise。
 */
export function refreshAccessToken() {
    if (!refreshPromise) {
        refreshPromise = runExclusively(doRefresh).finally(() => {
            refreshPromise = null;
        });
    }
    return refreshPromise;
}

/** 跨标签页互斥：Web Locks 要求安全上下文（HTTPS 或 localhost），不支持时降级为直接执行 */
function runExclusively(task) {
    if (typeof navigator !== "undefined" && navigator.locks) {
        return navigator.locks.request(REFRESH_LOCK_NAME, task);
    }
    return task();
}

async function doRefresh() {
    try {
        const resp = await refreshClient.post(REFRESH_URL);
        const body = resp.data;
        if (body && body.code === 200 && body.data) {
            setAccessToken(body.data);
            return {ok: true};
        }
        return {ok: false, data: body};
    } catch (e) {
        // 网络抖动、超时：不是“登录失效”，不能据此把用户踢出去
        return {ok: false, data: null};
    }
}

/**
 * 恢复登录态：页面刷新或新开标签页后内存里没有令牌，用 HttpOnly Cookie 换一个。
 * 返回 true 表示已登录（内存里现在有令牌）。
 */
export async function restoreSession() {
    if (getAccessToken()) {
        return true;
    }
    const result = await refreshAccessToken();
    return result.ok;
}

/**
 * 靠 HttpOnly Cookie 退出：Access 已失效、或 /api/logout 异常时的兜底。
 * HttpOnly Cookie 只有服务端能清除，前端不调这个接口，回到登录页就会被残留的 Cookie 自动登录回来。
 * 用没有拦截器的 refreshClient：请求自己不会再触发 9xx 弹窗；失败也不抛，调用方照常回登录页。
 */
export function logoutByCookie() {
    return refreshClient.post("/api/auth/logout").catch(() => {});
}

// 添加请求拦截器
axios.interceptors.request.use(function (config) {
    // 路径规范化：baseURL 为空串时 axios 不会拼接，没有开头 "/" 的相对路径会被浏览器
    // 按当前页面路径解析（如 /dashboard/user 页面里的 'api/users' 会变成 /dashboard/api/users）。
    // 在这里统一补齐，绝对地址（http/https 开头）不处理。
    if (config.url && !config.url.startsWith('/') && !/^https?:\/\//i.test(config.url)) {
        config.url = '/' + config.url;
    }
    // 令牌只来自内存。“记住我”不再需要前端告诉后端：它已经编码在 Refresh Cookie 的 rm 声明里
    const token = getAccessToken();
    if (token) {
        config.headers['Authorization'] = token;
    }
    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加响应拦截器
axios.interceptors.response.use(async function (response) {
    // 下载接口用 responseType: 'blob'，此时后端的 JSON 错误体（906/905/权限不足…）也会被包成 Blob。
    // 先还原成对象，下面原有的 906 刷新、905 踢下线逻辑才读得到 code。
    // 成功的文件响应 Content-Type 是 octet-stream，不会进这个分支
    if (response.config.responseType === 'blob'
        && response.data instanceof Blob
        && response.data.type.includes('application/json')) {
        response.data = JSON.parse(await response.data.text());
    }
    // 906：Access Token 过期，会话可能还活着 → 静默刷新，再把原请求重放一遍。
    // _retried 防止服务端异常地一直返回 906 时无限循环重放
    if (response.data && response.data.code === ACCESS_EXPIRED_CODE && !response.config._retried) {
        response.config._retried = true;
        const result = await refreshAccessToken();
        if (result.ok) {
            // 重放会重新走一遍请求拦截器，自动换上刚存好的新令牌；
            // 它的结果同样会经过本响应拦截器，所以重放后遇到 9xx 也会被正确处理
            return axios(response.config);
        }
        if (!result.data) {
            // 网络故障：不是登录失效，只把本次请求当作失败，不弹“重新登录”
            return Promise.reject(response);
        }
        // 刷新被后端拒绝：用刷新接口返回的 901~905 顶替本次响应，
        // 落到下面原有的统一处理（异地登录弹窗、重新登录确认框）
        response.data = result.data;
    }
    // 【安全修复】旧版此处 console.log 打印每一个完整响应报文（含 JWT、用户资料等敏感数据），
    // 生产环境控制台可直接拷贝登录凭证，已彻底移除；调试需要时请用 Network 面板。

    // 单设备登录互斥：收到 905 说明该账号已在其他设备登录，当前设备被顶下线。
    // 与 901-904 区分开，给出明确的"已有人登录"提示并强制回到登录页。
    // 踢下线瞬间往往有多个并发请求同时收到 905，用模块级锁保证弹窗只弹一个。
    if (response.data.code === KICKED_CODE) {
        //清除token（removeToken 内部会连带清除权限缓存，避免下个账号沿用旧权限）
        removeToken();
        if (!authAlertOpen) {
            authAlertOpen = true;
            ElMessageBox.alert(
                response.data.msg + "，当前设备已被迫退出，请重新登录。",
                "账号异地登录",
                {
                    confirmButtonText: "重新登录",
                    type: "error",
                    showClose: false
                }
            ).finally(() => {
                // 整页跳转会重置模块状态，此处无需手动复位 authAlertOpen
                window.location.href = "/";
            });
        }
        // 其余并发请求静默 reject，由首个弹窗统一处理（不再重复跳转）
        return Promise.reject(response);
    }

    //拦截token验证结果，进行页面提示和跳转（901 空 / 902 篡改 / 903 过期 / 904 不匹配）
    if(response.data.code > 900) {
        // 【单飞锁】已有认证失效弹窗在展示时，直接静默忽略：
        // token 早已被首个弹窗分支清除，后续请求的 9xx 都是同一事实的重复反馈，
        // 再弹只会堆叠出多层对话框，徒增用户操作成本
        if (authAlertOpen) {
            return Promise.reject(response);
        }
        authAlertOpen = true;
        //token未通过
        messageFrame(response.data.msg + "是否重新登录？")
            .then(() => {//确认后
                //清除token
                removeToken();
                //跳转登录页
                window.location.href="/"
            })
            .catch(() => {//取消后
                // 用户选择留在当前页：必须复位锁，否则本会话内后续任何令牌失效
                // 都会被静默吞掉，用户再也不会有重新登录的机会
                authAlertOpen = false;
                ElMessage({
                    type: 'info',
                    message: '已取消登录 ',
                })
            })
        return Promise.reject(response);
    }
    return response;
}, function (error) {
    // 对响应错误做点什么
    return Promise.reject(error);
});