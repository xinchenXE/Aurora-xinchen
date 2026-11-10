<template>
  <transition name="dropdown-content">
    <div
      v-if="!expand && active"
      class="ob-glass-panel origin-top-right absolute right-0 mt-2 w-48 py-2">
      <slot />
    </div>
    <div
      v-else-if="expand && active"
      class="ob-glass-panel flex flex-col justify-center items-center mt-2 w-48 py-2">
      <slot />
    </div>
  </transition>
</template>

<script lang="ts">
import { computed, defineComponent, inject } from 'vue'

export default defineComponent({
  name: 'DropdownMenu',
  props: {
    expand: {
      type: Boolean,
      default: false
    }
  },
  setup() {
    const sharedState = inject('sharedState', { active: false })
    const active = computed(() => sharedState.active)
    return {
      active
    }
  }
})
</script>

<style lang="scss" scoped>
.ob-glass-panel {
  /* 对齐 DeepSeek ds-glass-dropdown：模糊玻璃 + 细边框 + 三层软阴影 */
  background: var(--header-pill-bg);
  backdrop-filter: blur(14px) saturate(160%);
  -webkit-backdrop-filter: blur(14px) saturate(160%);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  box-shadow:
    0 0 1px 0 rgba(0, 0, 0, 0.2),
    0 0 4px 0 rgba(0, 0, 0, 0.02),
    0 12px 32px 0 rgba(0, 0, 0, 0.08);
  color: var(--text-normal);
}
.dropdown-content-enter-active,
.dropdown-content-leave-active {
  transition: all 0.2s;
}
.dropdown-content-enter,
.dropdown-content-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
<style lang="scss">
/* 老浏览器回退：无 backdrop-filter 时玻璃面板给不透明底色 */
@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .theme-light .ob-glass-panel {
    background: rgba(255, 253, 255, 0.92);
  }
  .theme-dark .ob-glass-panel {
    background: rgba(19, 17, 26, 0.92);
  }
}
</style>
