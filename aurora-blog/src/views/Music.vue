<template>
  <div>
    <Breadcrumb :current="t('menu.music')" />
    <div class="flex flex-col">
      <div class="post-header">
        <h1 class="post-title text-white uppercase">{{ t('titles.music') }}</h1>
      </div>
      <div class="main-grid">
        <div class="relative space-y-5">
          <div class="post-html">
            这里将会存放我制作的AI翻唱内容,有些可能会独占(说白了就是懒得弄视频)，如果喜欢的话还请多多点赞投币吧<br><br>
            前往->👉<a href="https://space.bilibili.com/86937794" target="_blank">bilibili</a>👈<br><br>
            前往->👉<a href="https://v.douyin.com/ie9vt1AY/" target="_blank">抖音</a>👈<br><br>
            前往->👉<a href="https://v.kuaishou.com/7rXWg9" target="_blank">快手</a>👈<br><br>
            同时您也可以通过<a href="https://pan.baidu.com/s/1u1oANsxPSjiWtk-DfbizNA?pwd=ly4q " target="_blank">此处</a>前往下载歌曲,希望您喜欢
          </div>
                                          
          <div class="bg-ob-deep-800 p-4 lg:p-14 rounded-2xl shadow-xl mb-8 lg:mb-0 playlists">
            <div >
              <iframe src="https://js-bed-1302638718.cos.ap-beijing.myqcloud.com/playList.html" scrolling="auto" frameborder="0" class="trend-container2" id="iframe" height="370px" width="100%"></iframe>
          
            </div>
          </div>
          <!-- <Comment /> -->
        </div>
        <div class="col-span-1">
          <Sidebar>
            <Profile />
          </Sidebar>
        </div>
      </div>
    </div>
  </div>
</template>


<script lang="ts">
import { defineComponent, reactive, provide, computed, toRefs, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Sidebar, Profile } from '../components/Sidebar'
import Breadcrumb from '@/components/Breadcrumb.vue'
import { Comment } from '../components/Comment'
import { useCommentStore } from '@/stores/comment'
import emitter from '@/utils/mitt'
import api from '@/api/api'


export default defineComponent({
  name: 'Music',
  components: { Sidebar, Profile, Breadcrumb, Comment },
  
  
  setup() {
    const { t } = useI18n()
    const commentStore = useCommentStore()
    const reactiveData = reactive({
      links: '' as any,
      comments: [] as any,
      haveMore: false as any,
      isReload: false as any
    })
    const pageInfo = reactive({
      current: 1,
      size: 7
    })
    commentStore.type = 4
    onMounted(() => {
      fetchLinks()
      fetchComments()
    })
    provide(
      'comments',
      computed(() => reactiveData.comments)
    )
    provide(
      'haveMore',
      computed(() => reactiveData.haveMore)
    )
    emitter.on('friendLinkFetchComment', () => {
      pageInfo.current = 1
      reactiveData.isReload = true
      fetchComments()
    })
    emitter.on('friendLinkFetchReplies', (index) => {
      fetchReplies(index)
    })
    emitter.on('friendLinkLoadMore', () => {
      fetchComments()
    })
    const fetchLinks = () => {
      api.getFriendLink().then(({ data }) => {
        reactiveData.links = data.data
      })
    }
    const fetchComments = () => {
      const params = {
        type: 4,
        topicId: null,
        current: pageInfo.current,
        size: pageInfo.size
      }
      api.getComments(params).then(({ data }) => {
        if (reactiveData.isReload) {
          reactiveData.comments = data.data.records
          reactiveData.isReload = false
        } else {
          reactiveData.comments.push(...data.data.records)
        }
        if (data.data.count <= reactiveData.comments.length) {
          reactiveData.haveMore = false
        } else {
          reactiveData.haveMore = true
        }
        pageInfo.current++
      })
    }
    const fetchReplies = (index: any) => {
      api.getRepliesByCommentId(reactiveData.comments[index].id).then(({ data }) => {
        reactiveData.comments[index].replyDTOs = data.data
      })
    }
    return {
      ...toRefs(reactiveData),
      t
    }
  }

})
</script>
<!-- <link rel="stylesheet" href="https://chenxi-1302638718.cos.ap-nanjing.myqcloud.com/css/APlayer.min.css"> -->
<style lang="scss" scoped>
.block {
  display: inline-block;
  width: 24%;
}
.info {
  display: inline-block;
  width: 76%;
  height: 100%;
}
.link-name {
  margin-left: 20px;
  margin-bottom: 5px;
  margin-top: 2px;
  color: var(--text-normal);
  font-size: larger;
}
.link-intro {
  margin-left: 20px;
  margin-bottom: 1px;
  color: var(--text-normal);
}
.el-card {
  background: transparent;
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border-radius: 10px;
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
}

</style>
