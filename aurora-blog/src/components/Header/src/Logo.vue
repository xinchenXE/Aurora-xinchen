<template>
  <div class="flex items-start self-stretch relative" @click="handleClick">
    <div class="flex flex-col relative py-2 z-10 font-medium ob-drop-shadow cursor-pointer logo-text" @click="">
      <span class="flex text-3xl" v-if="websiteConfig.name">
        {{ websiteConfig.name }}
      </span>
      <span v-else class="flex text-3xl animation-text">LOADING</span>
      <span class="font-extrabold text-xs uppercase">
        {{ websiteConfig.englishName || 'BLOG' }}
      </span>
    </div>
    <!-- DeepSeek 同款星光呼吸动画：主星 pulse，小星错峰 twinkle -->
    <svg class="logo-sparkle" width="26" height="26" viewBox="0 0 26 26" fill="none" aria-hidden="true">
      <path
        d="M11 4c.55 4.4 2.05 6.5 9 8.9-6.95 2.4-8.45 4.5-9 8.9-.55-4.4-2.05-6.5-9-8.9 6.95-2.4 8.45-4.5 9-8.9z"
        fill="currentColor" />
      <path
        class="sparkle-sm"
        d="M21 2c.3 1.9 1 2.9 4 3.9-3 1-3.7 2-4 3.9-.3-1.9-1-2.9-4-3.9 3-1 3.7-2 4-3.9z"
        fill="currentColor" />
    </svg>
  </div>
</template>

<script lang="ts">
import { useAppStore } from '@/stores/app'
import { computed } from '@vue/reactivity'
import { defineComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useCommonStore } from '@/stores/common'
import { useNavigatorStore } from '@/stores/navigator'

export default defineComponent({
  name: 'Logo',
  setup() {
    const appStore = useAppStore()
    const commonStore = useCommonStore()
    const navigatorStore = useNavigatorStore()
    const router = useRouter()
    const handleClick = () => {
      router.push({ path: '/' })
      if (commonStore.isMobile && navigatorStore.openMenu === true) {
        navigatorStore.toggleMobileMenu()
      }
    }
    return {
      websiteConfig: computed(() => {
        return appStore.websiteConfig
      }),
      handleClick
    }
  }
})
</script>

<style lang="scss" scoped>
.logo-text {
  color: var(--header-text);
}
.logo-sparkle {
  position: absolute;
  top: 2px;
  right: -22px;
  z-index: 11;
  color: var(--text-bright);
  pointer-events: none;
  animation: logo-sparkle-pulse 2.4s ease-in-out infinite;
  .sparkle-sm {
    transform-box: fill-box;
    transform-origin: center;
    animation: logo-sparkle-twinkle 2.4s ease-in-out 0.4s infinite;
  }
}
@keyframes logo-sparkle-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.85;
  }
  50% {
    transform: scale(1.15);
    opacity: 1;
  }
}
@keyframes logo-sparkle-twinkle {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(0.6);
    opacity: 0.4;
  }
}
</style>
