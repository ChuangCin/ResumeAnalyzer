import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import Register from './pages/Register.vue'
import Login from './pages/Login.vue'
import UploadResume from './pages/UploadResume.vue'
import AnalysisResult from './pages/AnalysisResult.vue'
import ResumeLibrary from './pages/ResumeLibrary.vue'
import ProfileCenter from './pages/ProfileCenter.vue'
import MyMessages from './pages/MyMessages.vue'
import AdminLayout from './pages/admin/AdminLayout.vue'
import AdminUsers from './pages/admin/AdminUsers.vue'
import AdminJobs from './pages/admin/AdminJobs.vue'
import AdminAnalysis from './pages/admin/AdminAnalysis.vue'
import AdminStats from './pages/admin/AdminStats.vue'
import AdminMaintenance from './pages/admin/AdminMaintenance.vue'
import AdminProfile from './pages/admin/AdminProfile.vue'
import AdminMessages from './pages/admin/AdminMessages.vue'
import AdminAnalysisDetail from './pages/admin/AdminAnalysisDetail.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/register', component: Register },
  { path: '/login', component: Login },
  { path: '/upload', component: UploadResume },
  { path: '/resume-library', component: ResumeLibrary },
  { path: '/profile', component: ProfileCenter },
  { path: '/messages', component: MyMessages },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/stats',
    children: [
      { path: 'profile', component: AdminProfile },
      { path: 'messages', component: AdminMessages },
      { path: 'stats', component: AdminStats },
      { path: 'users', component: AdminUsers },
      { path: 'jobs', component: AdminJobs },
      { path: 'analysis', component: AdminAnalysis },
      { path: 'analysis/:id', component: AdminAnalysisDetail, props: true },
      { path: 'maintenance', component: AdminMaintenance }
    ]
  },
  { path: '/analysis/:id', component: AnalysisResult, props: true }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.path.startsWith('/admin')) {
    const role = localStorage.getItem('role')
    if (role !== 'admin') {
      const userId = localStorage.getItem('userId')
      return userId ? '/upload' : '/login'
    }
  }
})

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
