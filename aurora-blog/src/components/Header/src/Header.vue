<template>
  <div class="header-container">
    <header class="site-header" :class="{ 'is-scrolled': isScrolled }">
      <Logo />
      <Navigation />
      <Controls />
    </header>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, onBeforeUnmount } from 'vue'
import { Logo, Navigation, Controls } from '../index'

export default defineComponent({
  name: 'Header',
  components: {
    Logo,
    Navigation,
    Controls
  },
  props: {
    msg: String
  },
  setup() {
    // 参考 DeepSeek：未滚动全透明，滚动后过渡为毛玻璃胶囊
    const isScrolled = ref(false)
    const onScroll = () => {
      isScrolled.value = window.scrollY > 40
    }
    onMounted(() => {
      onScroll()
      window.addEventListener('scroll', onScroll, { passive: true })
    })
    onBeforeUnmount(() => {
      window.removeEventListener('scroll', onScroll)
    })
    return { isScrolled }
  }
})
</script>

<style lang="scss" scoped>
.header-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  padding-top: 8px;
  pointer-events: none;

  .site-header {
    /* DeepSeek 同款悬浮胶囊：透明 -> 滚动后毛玻璃 */
    --header-text: #fff;
    /* 滚动态向中间等比收缩的系数（队列项 027；只影响观感，transform 不改布局盒） */
    --header-shrink-scale: 0.94;
    width: min(100% - 48px, var(--max-width));
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 4px 16px;
    border-radius: 100px;
    border: 1px solid transparent;
    background: transparent;
    pointer-events: auto;
    /* 向视口中线（水平）与自身中心（垂直）收缩；顶边下移约 2.3px，与 84px 内容预留的间隙只增不减 */
    transform-origin: 50% 50%;
    transition:
      background-color 0.3s ease,
      border-color 0.3s ease,
      box-shadow 0.3s ease,
      backdrop-filter 0.3s ease,
      -webkit-backdrop-filter 0.3s ease,
      transform 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);

    &.is-scrolled {
      --header-text: var(--text-normal);
      background: var(--header-pill-bg);
      backdrop-filter: blur(14px) saturate(160%);
      -webkit-backdrop-filter: blur(14px) saturate(160%);
      border-color: var(--glass-border);
      box-shadow: var(--glass-shadow);

      /* 移动端（< 997px）不收缩，与 App.vue 的 MOBILE_WITH = 996 严格对齐 */
      @media (min-width: 997px) {
        transform: scale(var(--header-shrink-scale));
      }
    }
  }
}
/* 老浏览器回退：无 backdrop-filter 时滚动态给不透明底色 */
@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .theme-light .site-header.is-scrolled {
    background: rgba(255, 253, 255, 0.88);
  }
  .theme-dark .site-header.is-scrolled {
    background: rgba(19, 17, 26, 0.88);
  }
}
</style>
