<template>
  <el-card class="main-card">
    <div class="title">{{ this.$route.name }}</div>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="模型的 Base URL / API Key / 模型名在 Nacos 配置（spring.ai.openai），修改后需重启后端；此处配置 AI 助手的角色名、开场白与人设提示词，保存后立即生效。"
      style="margin-bottom: 18px" />
    <el-form label-width="100px" label-position="left" v-loading="loading">
      <el-form-item label="角色名称">
        <el-input
          v-model="personaForm.roleName"
          maxlength="20"
          show-word-limit
          style="width: 400px"
          :placeholder="roleNamePlaceholder" />
      </el-form-item>
      <el-form-item label="开场白">
        <el-input
          v-model="personaForm.greeting"
          maxlength="200"
          show-word-limit
          :placeholder="greetingPlaceholder" />
      </el-form-item>
      <el-form-item label="人设提示词">
        <el-input
          v-model="personaForm.systemPrompt"
          type="textarea"
          :rows="10"
          maxlength="2000"
          show-word-limit
          placeholder="留空则使用 Nacos 默认人设（aurora.ai.chat.system-prompt）" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="medium" icon="el-icon-check" :disabled="saving" @click="updatePersona">
          保存
        </el-button>
        <el-button size="medium" icon="el-icon-refresh-left" :disabled="saving" @click="restoreDefault">
          恢复默认
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
export default {
  data: function () {
    return {
      loading: false,
      saving: false,
      defaultRoleName: '',
      defaultGreeting: '',
      personaForm: {
        roleName: '',
        greeting: '',
        systemPrompt: ''
      }
    }
  },
  computed: {
    roleNamePlaceholder() {
      return this.defaultRoleName
        ? '默认：' + this.defaultRoleName + '（留空即用默认）'
        : '留空则使用 Nacos 默认角色名（aurora.ai.chat.role-name）'
    },
    greetingPlaceholder() {
      return this.defaultGreeting
        ? '默认：' + this.defaultGreeting + '（留空即用默认）'
        : '留空则使用 Nacos 默认开场白（aurora.ai.chat.greeting）'
    }
  },
  methods: {
    getPersona() {
      this.loading = true
      this.axios
        .get('/api/admin/ai/persona')
        .then(({ data }) => {
          if (data.flag) {
            this.personaForm.roleName = (data.data && data.data.roleName) || ''
            this.personaForm.greeting = (data.data && data.data.greeting) || ''
            this.personaForm.systemPrompt = (data.data && data.data.systemPrompt) || ''
            this.defaultRoleName = (data.data && data.data.defaultRoleName) || ''
            this.defaultGreeting = (data.data && data.data.defaultGreeting) || ''
          } else {
            this.$message.error(data.message)
          }
        })
        .catch(() => {
          this.$message.error('获取人设失败，请稍后再试')
        })
        .finally(() => {
          this.loading = false
        })
    },
    updatePersona() {
      this.savePersona(
        {
          roleName: this.personaForm.roleName,
          greeting: this.personaForm.greeting,
          systemPrompt: this.personaForm.systemPrompt
        },
        '保存成功，前台 AI 助手立即按新配置展示与回答'
      )
    },
    restoreDefault() {
      this.$confirm('确定恢复默认吗？将清空角色名、开场白与人设，全部回退到 Nacos 默认配置。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.savePersona({ roleName: '', greeting: '', systemPrompt: '' }, '已恢复默认配置')
        })
        .catch(() => {})
    },
    savePersona(form, successMsg) {
      this.saving = true
      this.axios
        .put('/api/admin/ai/persona', form)
        .then(({ data }) => {
          if (data.flag) {
            this.$message.success(successMsg)
            this.getPersona()
          } else {
            this.$message.error(data.message)
          }
        })
        .catch(() => {
          this.$message.error('保存失败，请稍后再试')
        })
        .finally(() => {
          this.saving = false
        })
    }
  },
  mounted() {
    this.getPersona()
  }
}
</script>
