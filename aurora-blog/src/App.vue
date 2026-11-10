<template>

  <div id="App-Wrapper" :class="[appWrapperClass, theme]" :style="wrapperStyle">
    <!-- 毛玻璃全屏衬底：复用站点 banner 图，不引入新颜色；删除此 div 即可一键关闭 T3 效果 -->
    <div class="app-glass-backdrop" :style="backdropImage"></div>
    <div
      id="App-Container"
      class="app-container max-w-10/12 lg:max-w-screen-2xl px-3 lg:px-8"
      @keydown.meta.k.stop.prevent=""
      tabindex="-1"
      :style="cssVariables">
      <HeaderMain />
      <div class="app-banner app-banner-image" :style="headerImage" />
      <div class="app-banner app-banner-screen" :style="headerBaseBackground" />
      <div class="relative z-10">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide-y" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
    <div id="loading-bar-wrapper" :class="loadingBarClass"></div>
    
  </div>
  <Footer id="footer" :style="cssVariables" />
  <div class="App-Mobile-sidebar" v-if="isMobile">
    <div id="App-Mobile-Profile" class="App-Mobile-wrapper">
      <MobileMenu />
    </div>

  </div>

  <AuroraNavigator />
<!--  <Dia v-if="!isMobile" />-->
  <Live2d  v-if="!isMobile" />
  <AiChat v-if="!isMobile" />
  <UserCenter />
  <teleport to="head">
    <title>{{ title }}</title>
  </teleport>
</template>

<script lang="ts">
import { computed, defineComponent, onBeforeMount, onUnmounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { useCommonStore } from '@/stores/common'
import { useMetaStore } from '@/stores/meta'
import HeaderMain from '@/components/Header/src/Header.vue'
import Footer from '@/components/Footer.vue'
import MobileMenu from '@/components/MobileMenu.vue'
import Live2d from '@/components/Live2d.vue'
import AuroraNavigator from '@/components/AuroraNavigator.vue'
import UserCenter from '@/components/UserCenter.vue'
import AiChat from '@/components/AiChat.vue'
import api from './api/api'
export default defineComponent({
  name: 'App',
  components: {
    HeaderMain,
    Footer,
    // Dia,
    Live2d,
    AuroraNavigator,
    MobileMenu,
    UserCenter,
    AiChat
  },
  setup() {
    const appStore = useAppStore()
    const commonStore = useCommonStore()
    const metaStore = useMetaStore()
    const MOBILE_WITH = 996
    const appWrapperClass = 'app-wrapper'
    const loadingBarClass = ref({
      'nprogress-custom-parent': false
    })
    const wrapperStyle = ref({ 'min-height': '100vh' })
    const isMobile = computed(() => {
      return commonStore.isMobile
    })
    onBeforeMount(() => {
      initialApp()
    })
    onUnmounted(() => {
      document.removeEventListener('copy', copyEventHandler)
      window.removeEventListener('resize', resizeHander)
    })
    const initialApp = () => {
      initResizeEvent()
      intialCopy()
      initWindowOnload()
      fetchWebsiteConfig()
      let wrapperHeight = screen.height
      const footerEl = document.getElementById('footer')
      const footerHeight = footerEl?.getBoundingClientRect().height
      if (typeof footerHeight === 'number') {
        wrapperHeight = wrapperHeight - footerHeight * 2
      }
      wrapperStyle.value = {
        'min-height': wrapperHeight + 'px'
      }
      appStore.initializeTheme(appStore.themeConfig.theme)
    }
    const fetchWebsiteConfig = () => {
      api.getWebsiteConfig().then(({ data }) => {
        appStore.viewCount = data.data.viewCount
        appStore.articleCount = data.data.articleCount
        appStore.talkCount = data.data.talkCount
        appStore.categoryCount = data.data.categoryCount
        appStore.tagCount = data.data.tagCount
        appStore.websiteConfig = data.data.websiteConfigDTO
      })
    }
    const copyEventHandler = (event: any) => {
      if (document.getSelection() instanceof Selection) {
        if (document.getSelection()?.toString() !== '' && event.clipboardData) {
          event.clipboardData.setData('text', document.getSelection())
          event.preventDefault()
        }
      }
    }
    const intialCopy = () => {
      document.addEventListener('copy', copyEventHandler)
    }
    const resizeHander = () => {
      const rect = document.body.getBoundingClientRect()
      const mobileState = rect.width - 1 < MOBILE_WITH
      if (isMobile.value !== mobileState) commonStore.changeMobileState(mobileState)
    }
    const initResizeEvent = () => {
      resizeHander()
      window.addEventListener('resize', resizeHander)
    }
    const initWindowOnload = () => {
      window.onload = () => {
        setTimeout(() => {
          window.scrollTo({
            top: 0
          })
        }, 10)
      }
    }
    return {
      title: metaStore.title,
      theme: computed(() => appStore.themeConfig.theme),
      headerImage: computed(() => {
        return {
          backgroundImage: `url(${commonStore.headerImage}), url(${require('@/assets/default-cover.jpg')})`,
          opacity: commonStore.headerImage !== '' ? 1 : 0
        }
      }),
      backdropImage: computed(() => {
        return {
          backgroundImage: `url(${commonStore.headerImage}), url(${require('@/assets/default-cover.jpg')})`
        }
      }),
      headerBaseBackground: computed(() => {
        return {
          background: appStore.themeConfig.header_gradient_css,
          opacity: commonStore.headerImage !== '' ? 0.91 : 0.99
        }
      }),
      wrapperStyle: computed(() => wrapperStyle.value),

      isMobile: computed(() => commonStore.isMobile),
      cssVariables: computed(() => {
        if (appStore.themeConfig.theme === 'theme-dark') {
          return `
            --text-accent: ${appStore.themeConfig.gradient.color_1};
            --text-sub-accent: ${appStore.themeConfig.gradient.color_3};
            --main-gradient: ${appStore.themeConfig.header_gradient_css};
          `
        }
        return `
          --text-accent: ${appStore.themeConfig.gradient.color_3};
          --text-sub-accent: ${appStore.themeConfig.gradient.color_2};
          --main-gradient: ${appStore.themeConfig.header_gradient_css};
        `
      }),
      appWrapperClass,
      loadingBarClass
    }
  }
})
</script>

<style lang="scss">
.arrow-left > .icon,
.arrow-right > .icon {
  display: inline !important;
}
.img-error {
  display: none !important;
}
.el-drawer {
  background: transparent !important;
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
}
.el-dialog {
  background: transparent !important;
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border: 1px solid var(--glass-border);
}
/* 右上角通知框（ElNotification，teleport 到 body）玻璃化，与抽屉/弹窗同级：
   主题类挂在 body（见 stores/app.ts），--glass-* 与文字主题变量可直接继承 */
.el-notification {
  background: transparent !important;
  /* 显式声明：任何主题/浏览器下通知卡片都不带背景色 */
  background-color: transparent !important;
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}
/* 主题作用域选择器抬高特异性，压过后加载的 theme-chalk 默认文字色；
   类型图标色（success/error/warning/info）保留 element-plus 默认语义色。
   深色主题保留粉色系；浅色主题走 --notify-*（theme-variables.scss），
   玻璃卡片无底色，用浅色文字保证可读性 */
.theme-dark .el-notification .el-notification__title {
  color: var(--text-bright);
}
.theme-dark .el-notification .el-notification__content {
  color: var(--text-normal);
}
.theme-dark .el-notification .el-notification__closeBtn {
  color: var(--text-dim);
}
.theme-dark .el-notification .el-notification__closeBtn:hover {
  color: var(--text-bright);
}
.theme-light .el-notification .el-notification__title {
  color: var(--notify-title);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}
.theme-light .el-notification .el-notification__content {
  color: var(--notify-text);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}
.theme-light .el-notification .el-notification__closeBtn {
  color: var(--notify-close);
}
.theme-light .el-notification .el-notification__closeBtn:hover {
  color: var(--notify-title);
}
/* 老浏览器回退：无 backdrop-filter 支持时用主题底色保证抽屉/弹窗/移动端侧栏可读
   （主题类挂在 body，见 stores/app.ts；回退色与 theme-variables.scss 的 .ob-glass 保持一致） */
@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .theme-light .el-drawer,
  .theme-light .el-dialog {
    background: rgba(255, 253, 255, 0.92) !important;
  }
  .theme-dark .el-drawer,
  .theme-dark .el-dialog {
    background: rgba(19, 17, 26, 0.92) !important;
  }
  .theme-light .App-Mobile-wrapper {
    background: rgba(255, 253, 255, 0.92);
  }
  .theme-dark .App-Mobile-wrapper {
    background: rgba(19, 17, 26, 0.92);
  }
}
body {
  background: var(--background-primary-alt);
}

/* 页脚与加载条抬升到玻璃衬底之上，避免被固定衬底盖住 */
#footer,
#loading-bar-wrapper {
  position: relative;
  z-index: 10;
}

*:focus {
  outline: none;
}

#app {
  @apply relative min-w-full min-h-screen h-full;
  // font-family: Rubik, Avenir, Helvetica, Arial, sans-serif;
  font-family: JetBrainsMono;
  .app-wrapper {
    @apply min-w-full h-full pb-12;
    background: transparent;
    transition-property: transform, border-radius;
    transition-duration: 350ms;
    transition-timing-function: ease;
    transform-origin: 0 42%;
    .app-container {
      color: var(--text-normal);
      margin: 0 auto;
      /* 顶部栏改为 fixed 悬浮胶囊后，给首屏内容留出胶囊高度 */
      padding-top: 5.25rem;
    }
  }

  .app-glass-backdrop {
    position: fixed;
    /* 四周外扩 48px（约 2 倍模糊半径），让模糊化开的边缘落在屏幕外 */
    top: -48px;
    left: -48px;
    width: calc(100% + 96px);
    height: calc(100% + 96px);
    z-index: 0;
    background-size: cover;
    background-position: center top 15%;
    /* 雾面衬底：看得出背景轮廓但看不清细节 */
    filter: blur(24px) saturate(1.05);
    transform: translateZ(0);
    pointer-events: none;
  }
  /* 暗色主题：模糊基础上只降低亮度，不叠加其它颜色 */
  &.theme-dark .app-glass-backdrop {
    filter: blur(24px) brightness(0.5) saturate(0.85);
  }
  /* 老浏览器回退：玻璃组件已退化为不透明底色（见顶部 @supports），模糊衬底失去视觉效果，隐藏以省 GPU */
  @supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
    .app-glass-backdrop {
      display: none;
    }
  }

  .header-wave {
    position: absolute;
    top: 100px;
    left: 0;
    z-index: 1;
  }

  .App-Mobile-sidebar {
    @apply fixed top-0 bottom-0 left-0;
  }
  .App-Mobile-wrapper {
    @apply relative overflow-y-auto h-full -mr-4 pr-6 pl-4 pt-8 opacity-0;
    transition: all 0.85s cubic-bezier(0, 1.8, 1, 1.2);
    transform: translateY(-20%);
    width: 280px;
    background: transparent;
    backdrop-filter: blur(18px) saturate(160%);
    -webkit-backdrop-filter: blur(18px) saturate(160%);
    border-right: 1px solid var(--glass-border);
  }
}

.app-banner {
  content: '';
  display: block;
  height: 600px;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1;
  clip-path: polygon(
    100% 0,
    0 0,
    0 77.5%,
    1% 77.4%,
    2% 77.1%,
    3% 76.6%,
    4% 75.9%,
    5% 75.05%,
    6% 74.05%,
    7% 72.95%,
    8% 71.75%,
    9% 70.55%,
    10% 69.3%,
    11% 68.05%,
    12% 66.9%,
    13% 65.8%,
    14% 64.8%,
    15% 64%,
    16% 63.35%,
    17% 62.85%,
    18% 62.6%,
    19% 62.5%,
    20% 62.65%,
    21% 63%,
    22% 63.5%,
    23% 64.2%,
    24% 65.1%,
    25% 66.1%,
    26% 67.2%,
    27% 68.4%,
    28% 69.65%,
    29% 70.9%,
    30% 72.15%,
    31% 73.3%,
    32% 74.35%,
    33% 75.3%,
    34% 76.1%,
    35% 76.75%,
    36% 77.2%,
    37% 77.45%,
    38% 77.5%,
    39% 77.3%,
    40% 76.95%,
    41% 76.4%,
    42% 75.65%,
    43% 74.75%,
    44% 73.75%,
    45% 72.6%,
    46% 71.4%,
    47% 70.15%,
    48% 68.9%,
    49% 67.7%,
    50% 66.55%,
    51% 65.5%,
    52% 64.55%,
    53% 63.75%,
    54% 63.15%,
    55% 62.75%,
    56% 62.55%,
    57% 62.5%,
    58% 62.7%,
    59% 63.1%,
    60% 63.7%,
    61% 64.45%,
    62% 65.4%,
    63% 66.45%,
    64% 67.6%,
    65% 68.8%,
    66% 70.05%,
    67% 71.3%,
    68% 72.5%,
    69% 73.6%,
    70% 74.65%,
    71% 75.55%,
    72% 76.35%,
    73% 76.9%,
    74% 77.3%,
    75% 77.5%,
    76% 77.45%,
    77% 77.25%,
    78% 76.8%,
    79% 76.2%,
    80% 75.4%,
    81% 74.45%,
    82% 73.4%,
    83% 72.25%,
    84% 71.05%,
    85% 69.8%,
    86% 68.55%,
    87% 67.35%,
    88% 66.2%,
    89% 65.2%,
    90% 64.3%,
    91% 63.55%,
    92% 63%,
    93% 62.65%,
    94% 62.5%,
    95% 62.55%,
    96% 62.8%,
    97% 63.3%,
    98% 63.9%,
    99% 64.75%,
    100% 65.7%
  );
}

.app-banner-image {
  z-index: 1;
  background-size: cover;
  opacity: 0;
  transition: ease-in-out opacity 300ms;
}

.app-banner-screen {
  transition: ease-in-out opacity 300ms;
  z-index: 2;
  opacity: 0.91;
}
</style>
