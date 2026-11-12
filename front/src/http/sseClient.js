import { getAccessToken } from "@/util/util.js";
import { refreshAccessToken } from "@/http/httpRequest.js";

// SSE 流式请求客户端：fetch + ReadableStream。令牌放 Authorization 请求头，不进 URL。

export const AI_STREAM_CHAT = "/api/ai/stream-chat";
export const AI_RESET = "/api/ai/reset";

const DONE_MARKER = "[DONE]";
const ACCESS_EXPIRED_CODE = 906;

function buildQueryString(params) {
    const search = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
        if (value === undefined || value === null) return;
        search.append(key, value.toString());
    });
    const qs = search.toString();
    return qs ? `?${qs}` : "";
}

async function openStream(url, signal) {
    for (let attempt = 0; attempt < 2; attempt++) {
        const resp = await fetch(url, {
            method: "GET",
            headers: { Authorization: getAccessToken() || "", Accept: "text/event-stream" },
            cache: "no-store",
            signal
        });
        if ((resp.headers.get("content-type") || "").includes("text/event-stream")) {
            return resp;
        }
        // 不是事件流：后端的认证失败等错误是 JSON，HTTP 状态仍是 200，所以不能靠 resp.ok 判断
        const body = await resp.json().catch(() => null);
        if (body && body.code === ACCESS_EXPIRED_CODE && attempt === 0) {
            const result = await refreshAccessToken();
            if (result.ok) continue;
            throw new Error((result.data && result.data.msg) || "刷新登录状态失败，请稍后重试");
        }
        throw new Error((body && body.msg) || `请求失败（HTTP ${resp.status}）`);
    }
}

async function readEvents(resp, onData) {
    const reader = resp.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let dataLines = [];

    while (true) {
        const { done, value } = await reader.read();
        if (done) return;
        // stream: true 让解码器记住被切在两个数据块之间的半个汉字，下次接着解
        buffer += decoder.decode(value, { stream: true });

        let newline;
        while ((newline = buffer.indexOf("\n")) !== -1) {
            let line = buffer.slice(0, newline);
            buffer = buffer.slice(newline + 1);
            if (line.endsWith("\r")) line = line.slice(0, -1);

            if (line === "") {
                // 空行 = 一个事件结束
                if (dataLines.length > 0) {
                    onData(dataLines.join("\n"));
                    dataLines = [];
                }
                continue;
            }
            if (line.startsWith(":")) continue;

            const colon = line.indexOf(":");
            const field = colon === -1 ? line : line.slice(0, colon);
            let fieldValue = colon === -1 ? "" : line.slice(colon + 1);
            // 规范：冒号后若有一个空格要去掉（EventSource 也是这么做的，所以显示效果和以前一致）
            if (fieldValue.startsWith(" ")) fieldValue = fieldValue.slice(1);
            if (field === "data") dataLines.push(fieldValue);
        }
    }
}
export function createSseConnection(url, options = {}) {
    const { params = {}, onMessage, onComplete, onError } = options;
    const controller = new AbortController();
    let finalized = false;

    const finalize = (asError, payload) => {
        if (finalized) return;
        finalized = true;
        controller.abort();
        if (asError) {
            onError && onError(payload);
        } else {
            onComplete && onComplete();
        }
    };

    const run = async () => {
        const resp = await openStream(url + buildQueryString(params), controller.signal);
        await readEvents(resp, (data) => {
            if (finalized) return;
            if (data === DONE_MARKER) {
                finalize(false);
                return;
            }
            onMessage && onMessage(data);
        });
    };
    run().then(() => finalize(false), (err) => finalize(true, err));

    return { close: () => finalize(false) };
}
