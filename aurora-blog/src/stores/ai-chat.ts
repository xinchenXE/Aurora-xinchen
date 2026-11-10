import { defineStore } from 'pinia'

// AI 聊天浮窗状态（挂载在 App 层，跨路由保持不变）
export const useAiChatStore = defineStore('aiChatStore', {
  state: () => {
    return {
      visible: false
    }
  },
  actions: {
    open() {
      this.visible = true
    },
    close() {
      this.visible = false
    },
    toggle() {
      this.visible = !this.visible
    }
  }
})
