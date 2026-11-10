<template>
  <div class="header-controls flex flex-row items-center" tabindex="0">
    <Dropdown v-if="multiLanguage === 1" @command="handleClick">
      <span class="ob-drop-shadow" data-dia="language">
        <svg-icon icon-class="globe" />
        <span v-if="$i18n.locale == 'cn'">中文</span>
        <span v-if="$i18n.locale == 'en'">EN</span>
      </span>
      <DropdownMenu>
        <DropdownItem name="en">English</DropdownItem>
        <DropdownItem name="cn">中文</DropdownItem>
      </DropdownMenu>
    </Dropdown>
    <!-- 顶栏搜索入口：此前只有右下角导航球里有，顶栏缺失 -->
    <span class="ob-drop-shadow mr-1" data-dia="search" @click="openSearch">
      <svg-icon icon-class="search" />
    </span>
    <template v-if="userInfo === ''">
      <span class="mr-3" @click="openLoginDialog">{{ t('settings.login') }}</span>
    </template>
    <template v-if="userInfo !== ''">
      <!-- 用户菜单改为点击触发：悬停不显示，点击外部才关闭 -->
      <Dropdown>
        <span class="mr-2">
          <div class="flex-shrink-0">
            <div class="rounded-full ring-gray-100 overflow-hidden shaodw-lg w-9 h-9">
              <img
                class="avatar-img w-full h-full object-cover object-top"
                :src="userInfo.avatar || defaultAvatar"
                alt="" />
            </div>
          </div>
        </span>
        <DropdownMenu>
          <template v-if="!isMobile">
            <DropdownItem @click="openUserCenter">{{ t('settings.personal-center') }}</DropdownItem>
          </template>
          <DropdownItem @click="logout">{{ t('settings.logout') }}</DropdownItem>
        </DropdownMenu>
      </Dropdown>
    </template>
    <span no-hover-effect class="ob-drop-shadow" data-dia="light-switch">
      <ThemeToggle />
    </span>
  </div>
  <div id="userLogin">
  <el-dialog v-model="loginDialogVisible" width="700px" :fullscreen="isMobile" top="3rem"
             append-to-body custom-class="login-dialog-body" :show-close="false"
             :style="dialogThemeVars">
    <!-- 桌面端：仿翻转卡（保持原状，窄屏不适用，见下方移动端单列） -->
    <div v-if="!isMobile" class="userLogin-container">
      <div class="form-box" ref="formBoxRef">
        <!-- 邮箱注册 -->
        <div class="register-box hidden" ref="registerBoxRef">
          <h1>register</h1>
          <input type="text" v-model="loginInfo.username" placeholder="请输入邮箱"
                 maxlength="50"
                 style="width: 55% !important;position: relative;left: -7.4%;margin: 0 !important;">
          <button class="change-name-btn" @click="sendCode" >
            发送
          </button>
          <input type="text" v-model="loginInfo.code" placeholder="输入验证码">
          <input type="password" v-model="loginInfo.password" placeholder="输入密码">
          <button @click="register()">注册</button>
        </div>
        <!-- 登录 -->
        <div class="login-box" ref="loginBoxRef">
          <h1>login</h1>
          <input type="text" v-model="loginInfo.username" placeholder="昵称">
          <input type="password" v-model="loginInfo.password" placeholder="密码">
          <button @click="login()">登录</button>
          <br>
          <span class="text float-right" @click="openForgetPasswordDialog">忘记密码了?</span>
          <button @click="qqLogin()">QQ登录</button>
        </div>
      </div>
      <div class="con-box left">
        <h2>你好</h2>
        <p>您可以在此处使用邮箱注册</p>
        <img src="https://img0.baidu.com/it/u=3101692414,3580210645&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500" alt="" style="transform: scale(1.3);z-index: -1">
        <p>已有账号?</p>
        <button id="login" @click="goLogin">去登录</button>
      </div>
      <div class="con-box right">
        <h2>你好</h2>
        <p>您可以在此处使用邮箱或者QQ登录</p>
        <!-- <img alt="Static Badge" src="https://img.shields.io/badge/Beta-blue" style="width: unset;height: unset;margin: 0;opacity: 1;padding-top: 12px"> -->
        <img src="https://upload-bbs.mihoyo.com/upload/2022/11/06/171096414/940133f586bc69ca3442f202bd6c6281_1342243714944418976.gif" alt="" style="transform: scale(1.3);z-index: -1;width: 50%;height: 50%;">
        <p>第一次来?</p>
        <button id="register" @click="goRegister">去注册</button>
      </div>
    </div>
    <!-- 移动端：单列全屏，页签切换登录/注册（弃用 translateX 翻转） -->
    <div v-else class="userLogin-mobile">
      <button class="mobile-close" :aria-label="t('settings.login-dialog.close')"
              @click="loginDialogVisible = false">×</button>
      <div class="mobile-tabs" role="tablist">
        <button class="mobile-tab" :class="{ active: mobileTab === 'login' }"
                role="tab" :aria-selected="mobileTab === 'login'"
                @click="mobileTab = 'login'">{{ t('settings.login') }}</button>
        <button class="mobile-tab" :class="{ active: mobileTab === 'register' }"
                role="tab" :aria-selected="mobileTab === 'register'"
                @click="mobileTab = 'register'">{{ t('settings.login-dialog.register') }}</button>
      </div>
      <div v-show="mobileTab === 'login'" class="mobile-pane">
        <h1>{{ t('settings.login') }}</h1>
        <input type="text" v-model="loginInfo.username" :placeholder="t('settings.login-dialog.nickname')">
        <input type="password" v-model="loginInfo.password" :placeholder="t('settings.login-dialog.password')">
        <button @click="login()">{{ t('settings.login') }}</button>
        <span class="mobile-text" @click="openForgetPasswordDialog">{{ t('settings.login-dialog.forget') }}</span>
        <button @click="qqLogin()">{{ t('settings.login-dialog.qq') }}</button>
      </div>
      <div v-show="mobileTab === 'register'" class="mobile-pane">
        <h1>{{ t('settings.login-dialog.register') }}</h1>
        <input type="text" v-model="loginInfo.username" :placeholder="t('settings.login-dialog.email')" maxlength="50">
        <div class="mobile-code-row">
          <input type="text" v-model="loginInfo.code" :placeholder="t('settings.login-dialog.code')">
          <button class="mobile-code-btn" @click="sendCode">{{ t('settings.login-dialog.send') }}</button>
        </div>
        <input type="password" v-model="loginInfo.password" :placeholder="t('settings.login-dialog.password')">
        <button @click="register()">{{ t('settings.login-dialog.register') }}</button>
      </div>
    </div>
  </el-dialog>
  </div>
  <el-dialog v-model="forgetPasswordDialogVisible" width="30%" :fullscreen="isMobile" append-to-body
             :style="dialogThemeVars">
    <el-form>
      <el-form-item model="userInfo" class="mt-5">
        <el-input v-model="loginInfo.username" placeholder="邮箱" />
      </el-form-item>
      <el-form-item model="userInfo" class="mt-8">
        <el-input v-model="loginInfo.code" placeholder="验证码">
          <template #append>
            <span class="text" @click="sendCode">发送</span>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item model="userInfo" type="password" class="mt-8">
        <el-input v-model="loginInfo.password" type="password" show-password placeholder="新密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="updatePassword" size="large" class="mx-auto mt-3">确定</el-button>
      </el-form-item>
      <span class="text" @click="returnLoginDialog">返回登录</span>
    </el-form>
  </el-dialog>
  <el-dialog v-model="articlePasswordDialogVisible" width="30%" :fullscreen="isMobile" append-to-body
             :style="dialogThemeVars">
    <el-form @submit.native.prevent @keyup.enter.native="accessArticle">
      <el-form-item model="userInfo" class="mt-5">
        <el-input id="article-password-input" v-model="articlePassword" placeholder="文章受密码保护,请输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="accessArticle" size="large" class="mx-auto mt-3">校验密码</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
  <teleport to="body">
    <SearchModel />
  </teleport>
</template>

<script lang="ts">
import { computed, defineComponent, toRef, toRefs, reactive, getCurrentInstance, nextTick, ref, watch } from 'vue'
import { Dropdown, DropdownMenu, DropdownItem } from '@/components/Dropdown'
import { useAppStore } from '@/stores/app'
import { useCommonStore } from '@/stores/common'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import ThemeToggle from '@/components/ToggleSwitch/ThemeToggle.vue'
import api from '@/api/api'
import SearchModel from '@/components/SearchModel.vue'
import { useSearchStore } from '@/stores/search'
import config from '@/config/config'
import { useI18n } from 'vue-i18n'
import emitter from '@/utils/mitt'

// 后端 avatar 为空时兜底，保证登录态用户按钮可见可点
const defaultAvatar = require('@/assets/default-cover.jpg')

export default defineComponent({
  name: 'Controls',
  components: {
    Dropdown,
    DropdownMenu,
    DropdownItem,
    ThemeToggle,
    SearchModel
  },
  setup() {
    const { t } = useI18n()
    const proxy: any = getCurrentInstance()?.appContext.config.globalProperties
    const appStore = useAppStore()
    const commonStore = useCommonStore()
    const userStore = useUserStore()
    const searchStore = useSearchStore()
    const route = useRoute()
    const router = useRouter()
    const loginInfo = reactive({
      username: '' as any,
      password: '' as any,
      code: '' as any,
      val_password: '' as any
    })
    // 移动端登录/注册页签（桌面端不用，走 translateX 翻转卡）
    const mobileTab = ref<'login' | 'register'>('login')
    // 切换页签清空密码，避免注册密码残留到登录框（同 goRegister/otherLogin 的既有口径）
    watch(mobileTab, (val, old) => {
      if (val !== old) loginInfo.password = ''
    })
    // 卡面切换改用模板 ref：弹窗 append-to-body 后全局 DOM 查询不再可靠（可能取到页面其他同名节点）
    // nextTick 写入，确保 el-dialog 默认懒渲染的内容已挂载
    const formBoxRef = ref<HTMLElement | null>(null)
    const registerBoxRef = ref<HTMLElement | null>(null)
    const loginBoxRef = ref<HTMLElement | null>(null)
    const toggleCard = (toRegister: boolean) => {
      nextTick(() => {
        if (formBoxRef.value) formBoxRef.value.style.transform = toRegister ? 'translateX(80%)' : 'translateX(0%)'
        registerBoxRef.value?.classList.toggle('hidden', !toRegister)
        loginBoxRef.value?.classList.toggle('hidden', toRegister)
      })
    }
    const reactiveDate = reactive({
      loginDialogVisible: false,
      registerDialogVisible: false,
      forgetPasswordDialogVisible: false,
      articlePasswordDialogVisible: false,
      articlePassword: '',
      articleId: ''
    })
    emitter.on('changeArticlePasswordDialogVisible', (articleId: any) => {
      reactiveDate.articlePasswordDialogVisible = true
      reactiveDate.articlePassword = ''
      reactiveDate.articleId = articleId
      nextTick(() => {
        document.getElementById('article-password-input')?.focus()
      })
    })
    // AI 聊天浮窗在用户未登录时派发该事件，唤起登录弹窗
    emitter.on('openLoginDialog', () => {
      reactiveDate.loginDialogVisible = true
      mobileTab.value = 'login'
    })
    // 顶栏搜索：打开全局搜索弹窗（状态与右下角导航球共用 searchStore）
    const openSearch = () => {
      searchStore.setOpenModal(true)
    }

    const handleClick = (name: string): void => {
      appStore.changeLocale(name)
    }
    const login = () => {
      if (loginInfo.username.trim().length == 0 || loginInfo.password.trim().length == 0) {
        proxy.$notify({
          title: 'Warning',
          message: '账号或者密码不能为空',
          type: 'warning'
        })
        return
      }
      let params = new URLSearchParams()
      params.append('username', loginInfo.username)
      params.append('password', loginInfo.password)
      api.login(params).then(({ data }) => {
        if (data.flag) {
          userStore.userInfo = data.data
          sessionStorage.setItem('token', data.data.token)
          userStore.token = data.data.token
          proxy.$notify({
            title: 'Success',
            message: '登录成功',
            type: 'success'
          })
          reactiveDate.loginDialogVisible = false
        } else {
          proxy.$notify({
            title: 'Error',
            message: data.message || '登录失败',
            type: 'error'
          })
        }
      }).catch(() => {
        proxy.$notify({
          title: 'Error',
          message: '登录请求失败，请稍后重试',
          type: 'error'
        })
      })
    }
    

    const logout = () => {
      api.logout().then(({ data }) => {
        if (data.flag) {
          userStore.userInfo = ''
          userStore.token = ''
          userStore.accessArticles = []
          sessionStorage.removeItem('token')
          proxy.$notify({
            title: 'Success',
            message: '登出成功',
            type: 'success'
          })
        }
      })
    }
    const goLogin = () => {
      toggleCard(false)
    }
    const goRegister = () => {
      loginInfo.username = '';
      loginInfo.password = '';
      loginInfo.val_password = '';
      toggleCard(true)
    }
    const otherLogin = () => {
      loginInfo.username = '';
      loginInfo.password = '';
      loginInfo.val_password = '';
      toggleCard(false)
    }
    const openUserCenter = () => {
      userStore.userVisible = true
    }
    const openLoginDialog = () => {
      reactiveDate.loginDialogVisible = true
      mobileTab.value = 'login'
    }
    // 弹窗被 teleport 到 body 后拿不到 #App-Container 上的主题变量（同 AiChat.vue 先例），
    // 需在弹窗上显式注入，否则弹窗内 .text 等取 --text-normal 会落到继承默认值。
    // 其余主题变量来自 .theme-light/.theme-dark（绑在 body 上，teleport 后仍可继承）。
    const dialogThemeVars = computed(() => {
      const cfg: any = appStore.themeConfig
      if (cfg.theme === 'theme-dark') {
        return {
          '--text-accent': cfg.gradient.color_1,
          '--text-sub-accent': cfg.gradient.color_3,
          '--main-gradient': cfg.header_gradient_css
        }
      }
      return {
        '--text-accent': cfg.gradient.color_3,
        '--text-sub-accent': cfg.gradient.color_2,
        '--main-gradient': cfg.header_gradient_css
      }
    })

    const openRegisterDialog = () => {
      loginInfo.code = ''
      reactiveDate.loginDialogVisible = false
      reactiveDate.registerDialogVisible = true
    }
    const returnLoginDialog = () => {
      reactiveDate.registerDialogVisible = false
      reactiveDate.forgetPasswordDialogVisible = false
      reactiveDate.loginDialogVisible = true
    }
    const openForgetPasswordDialog = () => {
      loginInfo.code = ''
      reactiveDate.loginDialogVisible = false
      reactiveDate.forgetPasswordDialogVisible = true
    }
    const sendCode = () => {
      api.sendValidationCode(loginInfo.username).then(({ data }) => {
        if (data.flag) {
          proxy.$notify({
            title: 'Success',
            message: '验证码已发送',
            type: 'success'
          })
        }
      })
    }
    const register = () => {
      let params = {
        code: loginInfo.code,
        username: loginInfo.username,
        password: loginInfo.password
      }
      api.register(params).then(({ data }) => {
        if (data.flag) {
          proxy.$notify({
            title: 'Success',
            message: '注册成功,请点击登录进入系统',
            type: 'success'
          })
          toggleCard(false)
        }
      })
    }
    const qqLogin = () => {
      userStore.currentUrl = route.path
      reactiveDate.loginDialogVisible = false
      if (commonStore.isMobile) {
        //@ts-ignore
        QC.Login.showPopup({
          appId: config.qqLogin.QQ_APP_ID,
          redirectURI: config.qqLogin.QQ_REDIRECT_URI
        })
      } else {
        window.open(
          'https://graph.qq.com/oauth2.0/show?which=Login&display=pc&client_id=' +
            +config.qqLogin.QQ_APP_ID +
            '&response_type=token&scope=all&redirect_uri=' +
            config.qqLogin.QQ_REDIRECT_URI,
          '_self'
        )
      }
    }

    const updatePassword = () => {
      api.updatePassword(loginInfo).then(({ data }) => {
        if (data.flag) {
          proxy.$notify({
            title: 'Success',
            message: '修改成功',
            type: 'success'
          })
          reactiveDate.forgetPasswordDialogVisible = false
          reactiveDate.loginDialogVisible = true
        }
      })
    }
    const accessArticle = () => {
      if (reactiveDate.articlePassword.trim().length == 0) {
        proxy.$notify({
          title: 'Warning',
          message: '密码不能为空',
          type: 'warning'
        })
        return
      }
      api
        .accessArticle({
          articleId: reactiveDate.articleId,
          articlePassword: reactiveDate.articlePassword
        })
        .then(({ data }) => {
          if (data.flag) {
            reactiveDate.articlePasswordDialogVisible = false
            userStore.accessArticles.push(reactiveDate.articleId)
            router.push({ path: '/articles/' + reactiveDate.articleId })
          }
        })
    }
    return {
      loginInfo,
      ...toRefs(reactiveDate),
      // 模板 ref 必须从 setup 返回，否则 el-dialog 内的节点引用取不到（卡面切换会静默失效）
      formBoxRef,
      registerBoxRef,
      loginBoxRef,
      dialogThemeVars,
      mobileTab,
      userInfo: toRef(userStore.$state, 'userInfo'),
      defaultAvatar,
      isMobile: toRef(commonStore.$state, 'isMobile'),
      login,
      qqLogin,
      logout,
      handleClick,
      openSearch,
      openUserCenter,
      openLoginDialog,
      openRegisterDialog,
      otherLogin,
      returnLoginDialog,
      sendCode,
      register,
      updatePassword,
      openForgetPasswordDialog,
      accessArticle,
      goRegister,
      goLogin,
      multiLanguage: computed(() => {
        let websiteConfig: any = appStore.websiteConfig
        return websiteConfig.multiLanguage
      }),
      t
    }
  }
})
</script>
<style lang="scss">
/* 非 scoped 是有意的：登录弹窗加 append-to-body 后 teleport 到 body，
   作用域祖先（#userLogin）不再包住 .el-dialog，scoped 的 `:deep .login-dialog-body`
   会失配（2026-10-03 实测：规则命中时 dialog 宽 700px，失配后回落到 el-dialog 默认
   宽度，且被非法值 width="100" 放大成满屏 1440px）。
   用 `.el-dialog.login-dialog-body` 抬到 (0,2,0)：单类名与 element-plus 的 `.el-dialog`
   同特异性，会因源序落后而输给 `width: var(--el-dialog-width, 50%)`。 */
.el-dialog.login-dialog-body {
  padding: 0;
  background: transparent;
  /* element-plus 给 .el-dialog 默认带 1px 边框与投影。本弹窗的本体是个"空盒"：
     真实内容由绝对定位的 .userLogin-container / .userLogin-mobile 撑开。
     压成 height:0 后，残留的边框会在登录卡上方渲染成一条 700×2 的悬浮横线
     （2026-10-03 像素级实测：新增边缘仅在 y=48/50、x=370..1069，即弹窗自身宽度；
     这正是 ISS-033 截图 2 的"横贯导航区的线"）。故边框与投影一并去掉。 */
  border: none;
  box-shadow: none;
}
/* element-plus 的 20px 内边距落在 __body 上，只清 .el-dialog 的 padding 清不掉；
   桌面端靠 height:0 + 绝对定位内容遮住，移动端全屏会露成四边留白（实测左右各 21px）。 */
.el-dialog.login-dialog-body .el-dialog__body {
  padding: 0;
}
/* 仅桌面（非 fullscreen）折叠弹窗本体：内容由绝对定位的 .userLogin-container 撑开。
   加 :not(.is-fullscreen) 是必须的 —— 否则 width/height 会压过 element-plus 在
   全屏态下的 `--el-dialog-width:100%` / `height:100%`，移动端全屏就塌成 0 高。 */
.el-dialog.login-dialog-body:not(.is-fullscreen) {
  width: 700px;
  height: 0;
}

/* 移动端登录：单列全屏 + 页签切换（弃用 translateX 翻转卡）。
   复用 PC 侧字段、校验与 login()/register()/qqLogin() 及同一组 i18n 键。 */
.userLogin-mobile {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 2.5rem 1.5rem 3rem;
  background: var(--background-primary);
  color: var(--text-normal);
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-x: hidden;

  .mobile-close {
    position: absolute;
    top: 0.75rem;
    right: 0.75rem;
    width: 2.25rem;
    height: 2.25rem;
    border: none;
    background: transparent;
    color: var(--text-normal);
    font-size: 1.75rem;
    line-height: 1;
    cursor: pointer;
  }

  .mobile-tabs {
    display: flex;
    width: 100%;
    max-width: 420px;
    margin-top: 1.5rem;
    border-radius: 100px;
    border: 1px solid var(--glass-border);
    overflow: hidden;

    .mobile-tab {
      flex: 1;
      padding: 0.7rem 0;
      border: none;
      background: transparent;
      color: var(--text-normal);
      font-size: 0.95rem;
      letter-spacing: 2px;
      cursor: pointer;
      transition:
        background-color 250ms ease,
        color 250ms ease;

      &.active {
        background: var(--main-gradient);
        color: #fff;
      }
    }
  }

  .mobile-pane {
    width: 100%;
    max-width: 420px;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 2rem;

    h1 {
      margin-bottom: 1.25rem;
      /* PC 侧 h1 是白字（垫在紫色卡面上），单列浅底上必须跟随主题色 */
      color: var(--text-normal);
      letter-spacing: 5px;
    }

    input {
      width: 100%;
      box-sizing: border-box;
      background: transparent;
      border: none;
      border-bottom: 1px solid var(--glass-border);
      color: var(--text-normal);
      padding: 0.75rem 0;
      margin: 0.5rem 0;
      text-indent: 6px;
      font-size: 0.95rem;

      &::placeholder {
        color: var(--text-dim);
      }
      &:focus {
        outline: none;
        border-bottom-color: var(--text-normal);
      }
    }

    button {
      width: 100%;
      margin-top: 1.5rem;
      padding: 0.8rem;
      border: none;
      border-radius: 8px;
      background: var(--main-gradient);
      color: #fff;
      letter-spacing: 2px;
      cursor: pointer;
    }
  }

  .mobile-text {
    margin-top: 1rem;
    color: var(--text-accent);
    cursor: pointer;
    font-size: 0.85rem;
  }

  .mobile-code-row {
    display: flex;
    align-items: center;
    width: 100%;

    input {
      flex: 1;
      min-width: 0;
    }
    .mobile-code-btn {
      width: auto;
      margin: 0 0 0 0.75rem;
      padding: 0.5rem 0.9rem;
      font-size: 0.85rem;
      white-space: nowrap;
    }
  }
}
</style>
<style scoped>

.text {
  color: var(--text-normal);
  cursor: pointer;
}
#submit-button {
  outline: none;
  background: #0fb6d6;
}
.header-controls {
  span {
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--header-text);
    cursor: pointer;
    transition: opacity 250ms ease;
    padding-right: 0.5rem;
    &[no-hover-effect] {
      &:hover {
        opacity: 1;
      }
    }
    &:hover {
      opacity: 0.5;
    }
    .svg-icon {
      stroke: var(--header-text);
      height: 2rem;
      width: 2rem;
      margin-right: 0.5rem;
      pointer-events: none;
    }
  }
  .search-bar {
    @apply bg-transparent flex flex-row px-0 mr-2 rounded-full;
    opacity: 0;
    width: 0;
    transition: 300ms all ease-out;
    &.active {
      @apply bg-ob-deep-800;
      opacity: 0.95;
      width: 200px;
      imput {
        width: initial;
      }
    }
    &:focus {
      appearance: none;
      outline: none;
    }
    input {
      @apply flex flex-1 bg-transparent text-ob-normal px-6 box-border;
      width: 0;
      appearance: none;
      outline: none;
    }
    svg {
      @apply float-right;
    }
  }
}
.avatar-img {
  transition-property: transform;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 800ms;
  transform: rotate(-360deg);
}
.avatar-img:hover {
  transform: rotate(360deg);
}

.userLogin-container {
  margin: 5% auto;
  background-color: #fff;
  width: 650px;
  height: 415px;
  border-radius: 5px;
  box-shadow: 5px 5px 5px rgba(0,0,0,.1);
  position: relative
}

.form-box {
  position: absolute;
  top: -10%;
  left: 5%;
  background-color: #e19ef6;
  width: 320px;
  height: 500px;
  border-radius: 5px;
  box-shadow: 2px 0 10px rgba(0,0,0,.1);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2;
  transition: .5s ease-in-out
}

.login-box,.register-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%
}

.hidden {
  display: none;
  transition: .5s
}

h1 {
  text-align: center;
  margin-bottom: 25px;
  text-transform: uppercase;
  color: #fff;
  letter-spacing: 5px
}

h2 {
  text-align: center;
  margin-bottom: 25px;
  text-transform: uppercase;
  color: #fff;
  letter-spacing: 1px;
  margin-top: 28px;
}

input {
  background-color: transparent;
  width: 70%;
  color: #fff;
  border: none;
  border-bottom: 1px solid hsla(0,0%,100%,.4);
  padding: 10px 0;
  text-indent: 10px;
  margin: 8px 0;
  font-size: 14px;
  letter-spacing: 2px
}

input::-moz-placeholder {
  color: #fff
}

input::placeholder {
  color: #fff
}

input:focus {
  color: #a262ad;
  outline: none;
  border-bottom: 1px solid #a262ad80;
  transition: .5s
}

input:focus::-moz-placeholder {
  opacity: 0
}

input:focus::placeholder {
  opacity: 0
}

.form-box button {
  width: 70%;
  margin-top: 35px;
  background-color: #f6f6f6;
  outline: none;
  border-radius: 8px;
  padding: 13px;
  color: #a262ad;
  letter-spacing: 2px;
  border: none;
  cursor: pointer
}

.form-box button:hover {
  background-color: #a262ad;
  color: #f6f6f6;
  transition: background-color .5s ease
}

.con-box {
  width: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 50%;
  transform: translateY(-50%)
}

.con-box.left {
  left: -2%
}

.con-box.right {
  right: -2%
}

.con-box h2 {
  color: #8e9aaf;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 3px;
  text-align: center;
  margin-bottom: 4px
}

.con-box p {
  font-size: 12px;
  letter-spacing: 2px;
  color: #8e9aaf;
  text-align: center
}

.con-box span {
  color: #d3b7d8
}

.con-box img {
  width: 150px;
  height: 150px;
  opacity: .9;
  margin: 40px 0
}

.con-box button {
  background-color: #fff;
  color: #a262ad;
  border: 1px solid #d3b7d8;
  padding: 6px 10px;
  border-radius: 5px;
  letter-spacing: 1px;
  outline: none;
  cursor: pointer
}

.con-box button:hover {
  background-color: #d3b7d8;
  color: #fff
}

.change-name-btn {
  font-size: 14px;
  padding: 0 10px !important;
  height: 35px !important;
  margin: 0 !important;
  width: fit-content !important;
  position: relative;
  right: -33%;
  top: -40px;
}
</style>
