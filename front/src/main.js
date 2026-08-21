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

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

//el：指令所绑定到的页面dom元素。这可以用于直接操作DOM。
//binding：是一个对象，里面包含很多属性，重点看value属性：传递给指令的值。我们传的是 clue:delete 这个值
app.directive("hasPermission",  (el, binding) => {
    // 这会在 `mounted` 和 `updated` 时都调用
    doGet("/api/login/info", {}).then(resp => {
        let user = resp.data.data;
        let permissionList = user.permissionList;

        let flag = false;

        for (let key in permissionList) {
            if (permissionList[key] === binding.value) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            //没有权限，把页面元素隐藏掉
            //el.style.display = 'none';
            //把没有权限的按钮dom元素删除
            el.parentNode && el.parentNode.removeChild(el)
        }
    })
})


app.use(ElementPlus, {locale: zhCn}).use(router).mount('#app')
