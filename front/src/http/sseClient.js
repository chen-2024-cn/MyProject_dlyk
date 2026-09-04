import axios from "axios";
import { getTokenName } from "@/util/util.js";

/**
 * =============================================================================
 * SSE (Server-Sent Events) 流式请求客户端 —— 企业级封装
 * =============================================================================
 *
 * 【为什么要封装】
 * 浏览器原生 EventSource 只支持 GET、不能自动携带自定义请求头，
 * 若让业务组件自己拼 URL、管 token、判终止，会造成三处重复代码且难以统一维护。
 * 本模块把 SSE 连接细节集中收口，与 axios（httpRequest.js）同目录同风格：
 *   - httpRequest.js  → 普通请求-响应
 *   - sseClient.js    → AI 流式推送（EventSource）
 *
 * 【安全约定】
 * EventSource 无法设置 Authorization 请求头，因此 token 通过 URL 参数传递，
 * 后端 TokenVerifyFilter 已为 /api/ai/stream-chat 开启参数取 token 的专属校验通道。
 */

// ------------------------- 接口地址常量（集中管理，禁止散落硬编码） -------------------------
/** AI 流式对话接口 */
export const AI_STREAM_CHAT = "/api/ai/stream-chat";
/** AI 会话记忆重置接口 */
export const AI_RESET = "/api/ai/reset";

// ------------------------- 内部常量 -------------------------
// 流结束哨兵标记（保留兼容，正常流程由服务端关闭连接触发收尾）
const DONE_MARKER = "[DONE]";
// 「正常结束」判定阈值：最后一次收到消息距今不超过该毫秒数时，认为断线是正常的流终止
const NORMAL_CLOSE_GAP_MS = 1500;

/**
 * 获取全局请求基地址（与 axios 保持一致）
 * @returns {string} 后端基地址
 */
function getBaseUrl() {
    return axios.defaults.baseURL || "";
}

/**
 * 读取登录令牌（与 httpRequest 拦截器保持同样的取值优先级）
 * @returns {string} JWT，不存在时返回空串
 */
function getToken() {
    return (
        window.sessionStorage.getItem(getTokenName()) ||
        window.localStorage.getItem(getTokenName()) ||
        ""
    );
}

/**
 * 构建 GET 查询串：业务参数统一 encodeURIComponent，token 附加为 Authorization 参数
 * @param {Object} params 业务参数（值会被自动编码）
 * @param {string} token  JWT 令牌
 * @returns {string} 形如 "?a=1&b=2&Authorization=xxx"
 */
function buildQueryString(params, token) {
    const search = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
        if (value === undefined || value === null) return;
        search.append(key, value.toString());
    });
    if (token) {
        search.append("Authorization", token);
    }
    const qs = search.toString();
    return qs ? `?${qs}` : "";
}

/**
 * 创建 SSE 流式连接
 *
 * 说明：AI 对话的角色与权限完全由服务端从登录态判定，前端不再传 permissions。
 * 调用方只需传 message / memoryId（以及可选的 attachmentFileId）。
 *
 * 使用示例：
 * ```js
 * import { createSseConnection, AI_STREAM_CHAT } from "@/http/sseClient.js";
 *
 * const sse = createSseConnection(AI_STREAM_CHAT, {
 *   params: { message, memoryId },
 *   onMessage: (text) => { content.value += text },
 *   onComplete: () => { streaming.value = false },
 *   onError: (err) => console.error(err)
 * });
 * sse.close(); // 组件卸载或用户中断时手动关闭
 * ```
 *
 * @param {string} url 接口路径（以 / 开头，基地址自动拼接）
 * @param {Object} options 配置项
 * @param {Object} [options.params]    业务查询参数
 * @param {Function} [options.onMessage]  每收到一帧数据的回调（打字机追加）
 * @param {Function} [options.onComplete] 流正常结束的收尾回调
 * @param {Function} [options.onError]    流异常中断的回调
 * @returns {{ close: Function }} 连接句柄，调用 close() 主动断开
 */
export function createSseConnection(url, options = {}) {
    // 兼容性兜底：极少数环境不支持 EventSource
    if (typeof EventSource === "undefined") {
        options.onError && options.onError(new Error("当前浏览器不支持 EventSource（SSE）"));
        return { close: () => {} };
    }

    const { params = {}, onMessage, onComplete, onError } = options;
    const fullUrl = getBaseUrl() + url + buildQueryString(params, getToken());
    const eventSource = new EventSource(fullUrl);

    let finalized = false;     // 收尾防重入：保证 onComplete/onError 只触发一次
    let lastMessageAt = 0;     // 记录最后一次收到消息的时间戳（0 表示从未收到过数据）

    /** 统一的收尾出口（无论成功 / 异常，只执行一次后续回调） */
    const finalize = (asError, payload) => {
        if (finalized) return;
        finalized = true;
        eventSource.close();
        if (asError) {
            onError && onError(payload);
        } else {
            onComplete && onComplete();
        }
    };

    // 每收到一帧（后端一个 token / 一小段文本）触发一次
    eventSource.onmessage = (event) => {
        lastMessageAt = Date.now();
        // 哨兵标记直接收尾，不下发给业务层
        if (event.data === DONE_MARKER) {
            finalize(false);
            return;
        }
        onMessage && onMessage(event.data);
    };

    // EventSource 特性：服务端正常发完关闭连接时，也会触发 onerror。
    // 因此必须结合「最近是否收到过消息」区分：正常收尾 vs 真实异常。
    eventSource.onerror = () => {
        const gap = Date.now() - lastMessageAt;
        const receivedAnything = lastMessageAt !== 0;
        // 最近持续有数据在流动，此时的断线大概率是服务端正常关闭流
        if (receivedAnything && gap < NORMAL_CLOSE_GAP_MS) {
            finalize(false);
        } else {
            finalize(true, new Error("SSE 连接意外中断"));
        }
    };

    return {
        /** 主动关闭连接（组件卸载、用户切换会话时必须调用，防止连接泄漏） */
        close: () => finalize(false)
    };
}
