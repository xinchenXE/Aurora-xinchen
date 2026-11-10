<template>
  <Toggle :status="status" @changeStatus="handleChange">
    <template v-if="theme === 'theme-light'">
      <svg-icon icon-class="sun" :style="svg" />
    </template>
    <template v-else>
      <svg-icon icon-class="moon" :style="svg" />
    </template>
  </Toggle>
</template>

<script lang="ts">
import { computed, defineComponent, reactive } from 'vue'
import Toggle from './Toggle.vue'
import { useAppStore } from '@/stores/app'

export default defineComponent({
  name: 'ThemeToggle',
  components: {
    Toggle
  },
  setup() {
    const appStore = useAppStore()
    // 开关状态始终从 store 主题派生（true = 亮色），避免一次性快照导致的状态漂移
    const status = computed(() => appStore.themeConfig.theme === 'theme-light')
    const svgStyle = reactive({
      fill: 'yellow',
      margin: '5.5px 3px 2px 5.5px'
    })
    // 纯切换语义：由 store 按当前主题取反；不传开关内部状态（其 true=亮位 与 isDark 参数含义相反）
    const handleChange = () => {
      appStore.toggleTheme()
    }
    return {
      theme: computed(() => {
        return appStore.themeConfig.theme
      }),
      svg: computed(() => svgStyle),
      handleChange,
      status
    }
  }
})
</script>
