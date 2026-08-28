import axios from "axios";
import qs from "qs";
import {getTokenName, messageFrame, removeToken} from "@/util/util.js";
import {ElMessage, ElMessageBox} from "element-plus";

axios.defaults.baseURL = "http://localhost:8089";

// 业务码：账号已在其他设备登录，当前设备被顶下线（与后端 CodeEnum.TOKEN_IS_ELSEWHERE 对齐）
const KICKED_CODE = 905;

// 模块级弹窗锁：同一时刻多个请求同时收到 905 时，只允许弹出一个提示框
let kickedAlertOpen = false;
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
    // 对响应数据做点什么,再请求头创建token（jwt）传给后端
    let token = window.sessionStorage.getItem(getTokenName());
    if (!token) {
        //如果为空,从localStorage里面取
        token = window.localStorage.getItem(getTokenName());
        if (token) {}
        config.headers['rememberMe'] = true;
    }
    if (token) {
        config.headers['Authorization'] = token;
    }
    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加响应拦截器
axios.interceptors.response.use(function (response) {
    //TODO axios响应拦截器的控制台打印调试
    console.log('httpRequest.js: axios的响应拦截器' , response);

    // 单设备登录互斥：收到 905 说明该账号已在其他设备登录，当前设备被顶下线。
    // 与 901-904 区分开，给出明确的"已有人登录"提示并强制回到登录页。
    // 踢下线瞬间往往有多个并发请求同时收到 905，用模块级锁保证弹窗只弹一个。
    if (response.data.code === KICKED_CODE) {
        //清除token
        removeToken();
        if (!kickedAlertOpen) {
            kickedAlertOpen = true;
            ElMessageBox.alert(
                response.data.msg + "，当前设备已被迫退出，请重新登录。",
                "账号异地登录",
                {
                    confirmButtonText: "重新登录",
                    type: "error",
                    showClose: false
                }
            ).finally(() => {
                // 页面跳转会整体刷新模块状态，此处无需复位 kickedAlertOpen；
                // 保持 true 可以确保跳转完成前，心跳等残余请求的 901/902... 错误不会叠加新弹窗
                window.location.href = "/";
            });
        } else {
            // 其余并发请求直接静默跳转即可
            window.location.href = "/";
        }
        return Promise.reject(response);
    }

    //拦截token验证结果，进行页面提示和跳转
    if(response.data.code > 900) {
        // 905 异地登录弹窗尚未关闭（token 已被清除）时，心跳等残余请求返回的 901 等错误静默忽略，避免弹窗无限叠加
        if (kickedAlertOpen) {
            return Promise.reject(response);
        }
        //token未通过
            messageFrame(response.data.msg+ "是否重新登录？")
                .then(() => {//确认后
                //清除token
                removeToken();
                //跳转登录页
                window.location.href="/"
            })
                .catch(() => {//取消后
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