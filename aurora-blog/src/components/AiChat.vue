<template>
  <transition name="ai-chat-pop">
    <div ref="windowRef" v-show="visible" class="ai-chat-window ob-glass-strong" :style="cssVars">
      <!-- 头部（博客主题渐变，跟随明暗主题） -->
      <div class="ai-chat-header">
        <div class="ai-chat-title">
          <span class="ai-chat-logo">
            <svg viewBox="0 0 1024 1024" width="14" height="14" fill="currentColor" aria-hidden="true">
              <path
                d="M956.408445 419.226665a250.670939 250.670939 0 0 0-22.425219-209.609236A263.163526 263.163526 0 0 0 652.490412 85.715535 259.784384 259.784384 0 0 0 457.728923 0.008192a261.422756 261.422756 0 0 0-249.44216 178.582564 258.453206 258.453206 0 0 0-172.848261 123.901894c-57.03583 96.868753-44.031251 219.132275 32.153053 302.279661a250.670939 250.670939 0 0 0 22.32282 209.609237 263.163526 263.163526 0 0 0 281.595213 123.901893A259.067596 259.067596 0 0 0 566.271077 1023.990784a260.60357 260.60357 0 0 0 249.339762-178.889759 258.453206 258.453206 0 0 0 172.848261-123.901893c57.445423-96.868753 44.13365-218.82508-32.050655-302.074865zM566.578272 957.124721c-45.362429 0-89.496079-15.666934-124.516283-44.543243 1.638372-0.921584 4.198329-2.150363 6.143895-3.481541l206.537289-117.757998a32.35785 32.35785 0 0 0 16.895713-29.081105V474.82892l87.243317 49.97035c1.023983 0.307195 1.638372 1.228779 1.638372 2.252762v238.075953c0 105.8798-86.936122 191.689541-193.942303 191.996736zM148.588578 781.102113a189.846373 189.846373 0 0 1-23.346803-128.612213c1.535974 1.023983 4.09593 2.559956 6.143895 3.48154L337.922959 773.729439c10.444622 6.143896 23.346803 6.143896 34.098621 0l252.30931-143.664758v99.531108c0 1.023983-0.307195 1.945567-1.331177 2.559956l-208.892449 118.986778a196.297463 196.297463 0 0 1-265.518686-70.04041zM94.112704 335.97688c22.630015-39.013737 58.367008-68.81163 101.16948-84.171369V494.591784c0 11.7758 6.45109 22.93721 16.793315 28.978707l252.30931 143.767156L377.141493 716.796006a3.174346 3.174346 0 0 1-2.867152 0.307195l-208.892448-118.986777A190.870355 190.870355 0 0 1 94.215102 335.874482z m717.607001 164.861198L559.410394 357.070922 646.653711 307.20297a3.174346 3.174346 0 0 1 2.969549-0.307195l208.892449 118.986777a190.358364 190.358364 0 0 1 70.961994 262.139544 194.556693 194.556693 0 0 1-101.16948 84.171369V529.407192a31.538664 31.538664 0 0 0-16.588518-28.671513z m87.03852-129.329002c-1.74077-1.023983-4.300727-2.559956-6.246294-3.48154l-206.639687-117.757999a34.09862 34.09862 0 0 0-33.996222 0L399.566711 393.934295v-99.531108c0-1.023983 0.307195-1.945567 1.331178-2.559956l208.892449-119.089176a195.990268 195.990268 0 0 1 265.518686 70.450003c22.732414 38.706542 31.129071 84.171369 23.346803 128.305018zM352.258716 548.862861l-87.243317-49.560757a2.457558 2.457558 0 0 1-1.638372-2.252762V258.870991c0-105.8798 87.243317-191.996736 194.556692-191.689541a194.556693 194.556693 0 0 1 124.209089 44.543243c-1.638372 0.921584-4.198329 2.252762-6.143896 3.48154l-206.639687 117.757999a31.948257 31.948257 0 0 0-16.793315 29.081105l-0.307194 286.715126z m47.307995-100.759887L512 384.001664l112.535687 63.998912v127.997824l-112.228492 63.998912-112.535687-63.998912-0.307195-127.997824z" />
            </svg>
          </span>
          <span>{{ roleName }}</span>
        </div>
        <button class="ai-chat-close" title="关闭" @click="close">×</button>
      </div>

      <!-- 消息区 -->
      <div ref="messagesRef" class="ai-chat-messages">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="msg-row"
          :class="msg.role">
          <img v-if="msg.role === 'ai'" class="msg-avatar" :src="aiAvatar" alt="ai" />
          <div class="msg-bubble">{{ msg.content }}</div>
          <img v-if="msg.role === 'user'" class="msg-avatar" :src="userAvatar" alt="user" />
        </div>
        <div v-if="loading" class="msg-row ai">
          <img class="msg-avatar" :src="aiAvatar" alt="ai" />
          <div class="msg-bubble typing">
            <span class="dot" /><span class="dot" /><span class="dot" />
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="ai-chat-input">
        <input
          v-model="inputText"
          type="text"
          placeholder="输入消息，回车发送…"
          :disabled="loading"
          @keyup.enter="send" />
        <button class="send-btn" :disabled="loading || !inputText.trim()" @click="send">发送</button>
      </div>
    </div>
  </transition>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, nextTick, onMounted, onBeforeUnmount, reactive, ref, toRef } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useAiChatStore } from '@/stores/ai-chat'
import api from '@/api/api'
import emitter from '@/utils/mitt'

export default defineComponent({
  name: 'AiChat',
  setup() {
    const proxy: any = getCurrentInstance()?.appContext.config.globalProperties
    const userStore = useUserStore()
    const appStore = useAppStore()
    const aiChatStore = useAiChatStore()

    const inputText = ref('')
    const loading = ref(false)
    const messagesRef = ref<HTMLElement | null>(null)
    const windowRef = ref<HTMLElement | null>(null)
    // 角色名与开场白来自 /api/ai/assistant（DB 优先、Nacos 兜底），失败时保留本地兜底文案
    const roleName = ref('AI 小助手')
    const messages = reactive<Array<{ role: 'user' | 'ai'; content: string }>>([
      { role: 'ai', content: '你好呀，有什么可以帮你的吗？' }
    ])

    const visible = toRef(aiChatStore.$state, 'visible')

    // 主题变量：聊天窗位于 #App-Container 之外，拿不到 App 注入的变量，需自行生成
    const cssVars = computed(() => {
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

    const loadAssistant = () => {
      api
        .aiAssistant()
        .then(({ data }) => {
          if (data.flag && data.data) {
            if (data.data.roleName) {
              roleName.value = data.data.roleName
            }
            if (data.data.greeting && messages.length === 1) {
              messages[0].content = data.data.greeting
            }
          }
        })
        .catch(() => {})
    }

    const userAvatar = computed(() => {
      const info: any = userStore.userInfo
      return (info && info.avatar) || require('@/assets/default-cover.jpg')
    })
    const aiAvatar = computed(() => {
      const cfg: any = appStore.websiteConfig
      return (cfg && cfg.authorAvatar) || require('@/assets/default-cover.jpg')
    })

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesRef.value) {
          messagesRef.value.scrollTop = messagesRef.value.scrollHeight
        }
      })
    }

    const close = () => {
      aiChatStore.close()
    }

    const send = () => {
      const text = inputText.value.trim()
      if (!text || loading.value) return
      messages.push({ role: 'user', content: text })
      inputText.value = ''
      loading.value = true
      scrollToBottom()
      api
        .aiChat({ message: text })
        .then(({ data }) => {
          if (data.flag) {
            messages.push({ role: 'ai', content: data.data })
          } else {
            messages.push({ role: 'ai', content: data.message || '抱歉，我暂时无法回答。' })
          }
        })
        .catch((err: any) => {
          const isTimeout = err && (err.code === 'ECONNABORTED' || String(err.message || '').includes('timeout'))
          messages.push({ role: 'ai', content: isTimeout ? 'AI 响应超时了，请稍后再试。' : '网络开小差了，请稍后再试。' })
        })
        .finally(() => {
          loading.value = false
          scrollToBottom()
        })
    }

    // live2d chatGPT 图标点击后派发 openAiChat，登录校验在此处理
    const handleOpen = () => {
      if (userStore.userInfo && userStore.userInfo !== '') {
        aiChatStore.open()
        scrollToBottom()
      } else {
        proxy.$notify({
          title: 'Warning',
          message: '请先登录后再使用 AI 聊天',
          type: 'warning'
        })
        emitter.emit('openLoginDialog')
      }
    }

    // 滚轮防穿透：消息列表当前方向已不可滚动时拦截，避免滚动到背后页面（兜底旧浏览器；现代浏览器另有 overscroll-behavior 双保险）
    const onWindowWheel = (e: WheelEvent) => {
      const el = messagesRef.value
      if (!el) return
      const scrollable = el.scrollHeight > el.clientHeight
      const atTop = el.scrollTop <= 0
      const atBottom = Math.ceil(el.scrollTop + el.clientHeight) >= el.scrollHeight
      const down = e.deltaY > 0
      if (!scrollable || (down && atBottom) || (!down && atTop)) {
        e.preventDefault()
      }
    }

    onMounted(() => {
      emitter.on('openAiChat', handleOpen)
      loadAssistant()
      windowRef.value?.addEventListener('wheel', onWindowWheel, { passive: false })
    })
    onBeforeUnmount(() => {
      emitter.off('openAiChat', handleOpen)
      windowRef.value?.removeEventListener('wheel', onWindowWheel)
    })

    return {
      visible,
      messages,
      messagesRef,
      windowRef,
      inputText,
      loading,
      roleName,
      cssVars,
      userAvatar,
      aiAvatar,
      send,
      close
    }
  }
})
</script>

<style lang="scss" scoped>
.ai-chat-window {
  position: fixed;
  right: 24px;
  /* 导航球、横排图标与返回顶部按钮均在距底 64px 以内，90px 可完全避开 */
  bottom: 90px;
  z-index: 2000;
  width: 360px;
  max-width: calc(100vw - 32px);
  height: 480px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 12px;
}

.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  color: #fff;
  background: var(--main-gradient);
}
.ai-chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.ai-chat-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
}
.ai-chat-close {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  opacity: 0.85;
  transition: opacity 0.2s;
  &:hover {
    opacity: 1;
  }
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  // 滚到顶/底后不再把滚动量传递给背后页面（滚动链截断）
  overscroll-behavior-y: contain;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  &.user {
    justify-content: flex-end;
  }
  &.ai {
    justify-content: flex-start;
  }
}
.msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.msg-bubble {
  max-width: 72%;
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  color: var(--text-normal);
  background: var(--background-primary-alt);
  .user & {
    color: #fff;
    background: var(--text-accent);
    border-bottom-right-radius: 4px;
  }
  .ai & {
    border-bottom-left-radius: 4px;
  }
}

.typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-normal);
  opacity: 0.4;
  animation: ai-dot 1s infinite ease-in-out;
  &:nth-child(2) {
    animation-delay: 0.2s;
  }
  &:nth-child(3) {
    animation-delay: 0.4s;
  }
}
@keyframes ai-dot {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  50% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.ai-chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid rgba(146, 146, 156, 0.2);
  input {
    flex: 1;
    padding: 9px 12px;
    font-size: 13px;
    color: var(--text-normal);
    background: var(--background-primary-alt);
    border: 1px solid rgba(146, 146, 156, 0.25);
    border-radius: 8px;
    outline: none;
    &:focus {
      border-color: var(--text-accent);
    }
  }
  .send-btn {
    padding: 9px 16px;
    font-size: 13px;
    color: #fff;
    background: var(--text-accent);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: opacity 0.2s;
    &:hover:not(:disabled) {
      opacity: 0.85;
    }
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.ai-chat-pop-enter-active,
.ai-chat-pop-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.ai-chat-pop-enter-from,
.ai-chat-pop-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}
</style>
