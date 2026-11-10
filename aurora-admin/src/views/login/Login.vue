<template>
  <div class="login-container vue-page">
    <div class="rootbox">
      <h2>管理员登录</h2>
      <div class="item">
      <el-form status-icon :model="loginForm" :rules="rules" ref="ruleForm">
        <el-form-item prop="username">
          <input
            type="text"
            v-model="loginForm.username"
            required
            @keyup.enter.native="login" />
          <lable for="" style="color: #c6fafe">UserName</lable>
        </el-form-item>
        <el-form-item prop="password" class="item">
          <input
            type="password"
            required
            v-model="loginForm.password"
            @keyup.enter.native="login" />
          <lable for="" style="color: #c6fafe">PassWord</lable>
        </el-form-item>
      </el-form>
      <button class="btn" @click="login">登录
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>
  </div>
  </div>
</template>
<script>
import { generaMenu } from '@/assets/js/menu'
export default {
  data: function () {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
        password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
      }
    }
  },
  methods: {
    login() {
      this.$refs.ruleForm.validate((valid) => {
        if (valid) {
          const that = this
        //  var captcha = new TencentCaptcha(this.config.TENCENT_CAPTCHA, function (res) {
        //    if (res.ret === 0) {
              let param = new URLSearchParams()
              param.append('username', that.loginForm.username)
              param.append('password', that.loginForm.password)
              that.axios.post('/api/users/login', param).then(({ data }) => {
                if (data.flag) {
                  that.$store.commit('login', data.data)
                  generaMenu()
                  that.$message.success('登录成功')
                  that.$router.push({ path: '/' })
                } else {
                  that.$message.error(data.message)
                }
              })
        //    }
        //  })
        //  captcha.show()
        } else {
          return false
        }
      })
    }
  }
}
</script>
<style scoped>
@import '@/assets/css/xinchenXEstyle/index.css';
@import '@/assets/css/xinchenXEstyle/login.css';
h2 {
  text-align: center;
  color: #fff;
  margin-bottom: 30px;
}
body {
  background:linear-gradient(#c442d7, #e84cce);
  display: flex;
  justify-content: center;
  font-size: 16px;
  align-items: center;/*弹性盒子在纵轴上居中*/
  color: #c6fafe;
}
</style>


