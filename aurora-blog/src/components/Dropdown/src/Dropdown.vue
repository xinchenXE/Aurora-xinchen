<template>
  <div
    class="ob-dropdown relative z-50"
    @click="toggle"
    @mouseover="hoverHandler"
    @mouseleave="leaveHandler"
    v-click-away="onClickAway">
    <slot />
  </div>
</template>

<script lang="ts">
import { defineComponent, provide, reactive, ref, toRefs, watch } from 'vue'
import { onBeforeUnmount } from 'vue'
import { useDropdownStore } from '@/stores/dropdown'
import { useCommonStore } from '@/stores/common'

export default defineComponent({
  emits: ['command'],
  name: 'Dropdown',
  props: {
    hover: {
      type: Boolean,
      default: false
    }
  },
  setup(props, { emit }) {
    const commonStore = useCommonStore()
    const mouseHover = toRefs(props).hover
    const dropdownStore = useDropdownStore()
    const eventId = ref(0)
    let hoverTimer: ReturnType<typeof setTimeout> | null = null
    let sharedState: { active: boolean } = reactive({
      active: false
    })
    provide('sharedState', sharedState)
    watch(
      () => dropdownStore.commandName,
      (newName, oldName) => {
        const name = newName ? newName : oldName
        if (eventId.value === dropdownStore.uid) emit('command', name)
      }
    )
    const toggle = () => {
      if (!sharedState.active) eventId.value = dropdownStore.setUid()
      if (!mouseHover.value) sharedState.active = !sharedState.active
    }
    const onClickAway = () => {
      if (!mouseHover.value && !commonStore.isMobile) {
        sharedState.active = false
        eventId.value = 0
      }
    }
    const hoverHandler = () => {
      if (!sharedState.active) eventId.value = dropdownStore.setUid()
      if (mouseHover.value) {
        // 参考 DeepSeek 悬停延迟弹层：加一点悬停意图判定，防止误触
        if (hoverTimer) clearTimeout(hoverTimer)
        hoverTimer = setTimeout(() => {
          sharedState.active = true
        }, 150)
      }
    }
    const leaveHandler = () => {
      if (hoverTimer) {
        clearTimeout(hoverTimer)
        hoverTimer = null
      }
      if (mouseHover.value) {
        sharedState.active = false
        eventId.value = 0
      }
    }
    onBeforeUnmount(() => {
      if (hoverTimer) clearTimeout(hoverTimer)
    })
    return { toggle, onClickAway, hoverHandler, leaveHandler }
  }
})
</script>
