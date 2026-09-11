import axios from "axios";
import qs from "qs";
import {getTokenName, messageFrame, removeToken} from "@/util/util.js";
import {ElMessage, ElMessageBox} from "element-plus";

axios.defaults.baseURL = "http://localhost:8089";

// 业务码：账号已在其他设备登录，当前设备被顶下线（与后端 CodeEnum.TOKEN_IS_ELSEWHERE 对齐）
const KICKED_CODE = 905;

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

// 添加请求拦截器
axios.interceptors.request.use(function (config) {
    // 取值优先级：会话级 sessionStorage → 持久化 localStorage（记住我）
    let token = window.sessionStorage.getItem(getTokenName());
    if (token) {
        config.headers['Authorization'] = token;
        return config;
    }
    token = window.localStorage.getItem(getTokenName());
    if (token) {
        config.headers['Authorization'] = token;
        // 仅当 token 来自 localStorage（用户勾选过“记住我”）时，才告知后端续期为长会话；
        // 旧版逻辑把 rememberMe=true 写在“sessionStorage 未命中”分支里，语义颠倒。
        config.headers['rememberMe'] = true;
    }
    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加响应拦截器
axios.interceptors.response.use(function (response) {
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