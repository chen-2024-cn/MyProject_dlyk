// import './assets/main.css'

import { createApp } from 'vue'
import loginForm from './view/LoginForm.vue'
import App from "./App.vue";//根组件

//导入router组件
import router from './router/router.js'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'//导入CSS样式不需要from子句

//导入国际化中文包
import zhCn from 'element-plus/es/locale/lang/zh-cn'

//注册elementIcon图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import {doGet} from "@/http/httpRequest.js";
import {getPermissionCache, setPermissionCache} from "@/util/util.js";

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// ============================================================================
// 权限缓存单飞加载（single-flight）：解决 v-hasPermission 指令 N+1 请求风暴
// ----------------------------------------------------------------------------
// 旧实现：指令内部每个按钮都独立 doGet("/api/login/info")，十几个按钮 = 十几个并发请求。
// 新实现：
//   1. 命中 sessionStorage 缓存 → 同步判定，零请求；
//   2. 未命中 → 复用同一个 in-flight Promise，无论多少按钮同时挂载，只发 1 次请求；
//   3. 请求结果写入缓存，后续所有指令与页面直接读缓存。
// ============================================================================
let permissionPromise = null;

/** 加载登录人权限码集合（带缓存 + 并发单飞），resolve 出 Set 便于 O(1) 判定 */
function loadPermissionSet() {
    const cached = getPermissionCache();
    if (cached) {
        return Promise.resolve(new Set(cached.permissionList));
    }
    if (!permissionPromise) {
        permissionPromise = doGet("/api/login/info", {})
            .then(resp => {
                const user = resp.data.data || {};
                setPermissionCache({
                    permissionList: user.permissionList || [],
                    roleList: user.roleList || [],
                    userId: user.id ?? null,
                });
                return new Set(user.permissionList || []);
            })
            .catch(() => {
                // 拉取失败（多为令牌失效，交由响应拦截器处理跳转），本轮放行避免误删按钮
                permissionPromise = null;
                return new Set();
            });
    }
    return permissionPromise;
}

//el：指令所绑定到的页面dom元素。这可以用于直接操作DOM。
//binding：是一个对象，里面包含很多属性，重点看value属性：传递给指令的值。我们传的是 clue:delete 这个值
app.directive("hasPermission",  (el, binding) => {
    // 这会在 `mounted` 和 `updated` 时都调用
    loadPermissionSet().then(permissionSet => {
        if (!permissionSet.has(binding.value)) {
            //没有权限，把没有权限的按钮dom元素删除
            el.parentNode && el.parentNode.removeChild(el)
        }
    })
})


app.use(ElementPlus, {locale: zhCn}).use(router).mount('#app')
