import axios from "axios";
import qs from "qs";
import {getTokenName, messageFrame, removeToken} from "@/util/util.js";
import {ElMessage, ElMessageBox} from "element-plus";

axios.defaults.baseURL = "http://localhost:8089";
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

    //拦截token验证结果，进行页面提示和跳转
    if(response.data.code > 900) {
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