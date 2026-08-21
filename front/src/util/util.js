import {ElMessage, ElMessageBox} from "element-plus";

/**
 * 获取存储再sessionStorage或者localStorage里面的token（jwt）的名字
 * @returns {string}
 */
export function getTokenName() {
    return "dlyk_token";
}


export function removeToken() {
    window.localStorage.removeItem(getTokenName());
    window.sessionStorage.removeItem(getTokenName());
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

/**
 * 获取token
 *
 * @returns {string}
 */
export function getToken() {
    let token = window.sessionStorage.getItem(getTokenName());
    if (!token) { //前面加了一个！，表示token不存在，token是空的，token没有值，这个意思
        token = window.localStorage.getItem(getTokenName());
    }
    if (token) { //表示token存在，token不是空的，token有值，这个意思
        return token;
    } else {
        messageConfirm("请求token为空，是否重新去登录？").then(() => { //用户点击“确定”按钮就会触发then函数
            //既然后端验证token未通过，那么前端的token肯定是有问题的，那没必要存储在浏览器中，直接删除一下
            removeToken();
            //跳到登录页
            window.location.href = "/";
        }).catch(() => { //用户点击“取消”按钮就会触发catch函数
            messageTip("取消去登录", "warning");
        })
    }
}