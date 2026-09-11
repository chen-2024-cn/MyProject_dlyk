import {createRouter, createWebHistory} from "vue-router";
import {getTokenName} from "@/util/util.js";

const router = createRouter({
    history:createWebHistory(),
    routes:[
        {
            //路由路径
            path:'/',
            //路由路径所对应的页面
            component : () => import('../view/LoginForm.vue')
        },
        {
            //路由路径
                path:'/dashboard',
            //路由路径所对应的页面
            component : () => import('../view/DashboardView.vue'),
            // 需要登录才能访问（全局守卫据此拦截）
            meta: { requiresAuth: true },
            //子路由
            children : [
                {
                    //子路由不能以斜杠开头
                    path:'',
                    component: () => import('../view/StatisticView.vue'),
                },
                {
                    //子路由不能以斜杠开头
                    path:'user',
                    component: () => import('../view/UserView.vue'),
                },
                {
                    //id是动态变量用 :id
                    path:'user/:id',
                    component:() => import('../view/UserDetailView.vue'),
                },
                {
                    path:'activity',
                    component:() => import('../view/Activity.vue'),
                },
                {
                    path:'clue',
                    component:() => import('../view/ClueView.vue'),
                },
                {
                    path:'clue/add',
                    component:() => import('../view/ClueRecordView.vue'),
                },
                {
                    path:'clue/edit/:id',
                    component:() => import('../view/ClueRecordView.vue'),
                },
                {
                    path:'clue/:id',
                    component:() => import('../view/ClueDetailView.vue'),
                },
                {
                    path:'customer',
                    component:() => import('../view/CustomerView.vue'),
                },
                {
                    path:'customer/:id',
                    component:() => import('../view/CustomerDetailView.vue'),
                },
                {
                    path:'tran',
                    component:() => import('../view/TranView.vue'),
                },
                {
                    path:'tran/add',
                    component:() => import('../view/TranRecordView.vue'),
                },
                {
                    path:'tran/edit/:id',
                    component:() => import('../view/TranRecordView.vue'),
                },
                {
                    path:'tran/:id',
                    component:() => import('../view/TranDetailView.vue'),
                },
                {
                    path:'product',
                    component:() => import('../view/ProductView.vue'),
                },
                {
                    // 字典类型：目录总览页（卡片网格 + 数据质量巡检 + 跳转维护）
                    path:'dictype',
                    component:() => import('../view/DicTypeView.vue'),
                },
                {
                    // 字典数据：值维护工作台（类型选择器 + 深链 ?typeCode= + 排序号智能提示）
                    path:'dicvalue',
                    component:() => import('../view/DicValueView.vue'),
                },
                {
                    path:'system',
                    component:() => import('../view/SystemView.vue'),
                },
                {
                    path:'profile',
                    component:() => import('../view/UserProfileView.vue'),
                },
                {
                    path:'ai',
                    component:() => import('../view/AiAssistantView.vue'),
                }
            ]
        }
    ]
})

// ============================================================================
// 全局前置守卫：前端路由级登录拦截
// ----------------------------------------------------------------------------
// 审计发现问题：路由无任何守卫，未登录用户直接输入 /dashboard/* 也能挂载页面骨架，
// 页面再靠接口 401/901 回退——体验差且暴露系统页面结构。
// 修复：凡 requiresAuth 的路由（/dashboard 及其所有子路由由父路由 meta 继承判定），
// 无本地令牌一律重定向登录页；已登录用户访问登录页则直达工作台。
//
// 说明：前端守卫只是「体验层」拦截，真正的数据防线是后端 TokenVerifyFilter +
// @PreAuthorize（令牌可伪造删除但无法伪造有效），二者纵深配合。
// ============================================================================
// 采用 Vue Router 4 的「返回值」风格，而非已废弃的 next() 回调
// （next(value) 在 Vue Router 4 会打印 deprecation 警告；直接 return 目标或 undefined 等价且更简洁）
router.beforeEach((to) => {
    // 取值优先级与请求拦截器一致：会话级 sessionStorage → "记住我" 的 localStorage。
    // 这里不调用 getToken()，因为它内部会在无令牌时弹出重新登录对话框，
    // 与守卫自身的重定向行为重复叠加，会造成"还没跳转就先弹窗"的割裂体验。
    const token = window.sessionStorage.getItem(getTokenName())
        || window.localStorage.getItem(getTokenName());

    // 目标路由自身或其任一祖先声明了 requiresAuth（子路由未单独声明时沿用父级）
    const needAuth = to.matched.some(record => record.meta && record.meta.requiresAuth);

    if (needAuth && !token) {
        // 重定向登录页，并把原目标记入 redirect 参数，登录后可回跳（体验闭环）
        return { path: '/', query: { redirect: to.fullPath } };
    }
    if (to.path === '/' && token) {
        // 已登录再回登录页，直接送进工作台，避免重复登录
        return '/dashboard';
    }
    // 返回 undefined（或不 return）= 放行，等价于旧写法里的 next()
})

export default router
