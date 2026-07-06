import { createRouter, createWebHistory } from "vue-router";
import { isAuthenticated } from "../utils/authStorage";

const routes = [
  {
    path: "/",
    redirect: "/login",
  },
  {
    path: "/login",
    name: "login",
    component: () => import("../views/LoginView.vue"),
  },
  {
    path: "/case-reader",
    name: "case-reader",
    component: () => import("../views/CaseReaderView.vue"),
  },
  {
    path: '/case-query',
    name: 'case-query',
    component: () => import('../views/CaseQueryLayout.vue'),
    redirect: '/case-query/home',
    children: [
      {
        path: '/case-query/home',
        name: 'query-home',
        component: () => import('../views/HomeView.vue'),
      },
      {
        path: '/case-query/search',
        name: 'case-list',
        component: () => import('../views/SearchCases.vue'),
      },
      {
        path:'/case-query/favorite',
        name: 'favorite-case',
        component: () => import('../views/FavoriteCases.vue'),
      },
      {
        path:'/case-query/history',
        name: 'history-case',
        component: () => import('../views/HistoryCases.vue'),
      },
      {
        path: '/case-query/recharge',
        name: 'recharge',
        component: () => import('../views/RechargeView.vue'),
      },
      {
        path: '/case-query/kb',
        name: 'knowledge-base',
        component: () => import('../views/KnowledgeBaseView.vue'),
      },
    ]
  }
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

router.beforeEach((to, from, next) => {
  const isAuth = isAuthenticated();
  if (!isAuth && to.path !== '/login') {
    next('/login');
  } else if (isAuth && to.path === '/login') {
    next();
  } else {
    next();
  }
});

export default router;
