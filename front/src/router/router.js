import {createRouter, createWebHistory} from "vue-router";

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
                    //id是动态变量用：id
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
                    path:'dictype',
                    component:() => import('../view/DicView.vue'),
                },
                {
                    path:'dicvalue',
                    component:() => import('../view/DicView.vue'),
                },
                {
                    path:'system',
                    component:() => import('../view/SystemView.vue'),
                },
                {
                    path:'profile',
                    component:() => import('../view/UserProfileView.vue'),
                }
            ]
        }
    ]
})

export default router