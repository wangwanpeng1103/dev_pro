<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealthStatus, type HealthStatus } from './api'

const loading = ref(true)
const errorMessage = ref('')
const healthStatus = ref<HealthStatus | null>(null)

onMounted(async () => {
  try {
    const response = await getHealthStatus()
    healthStatus.value = response.data
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '服务连接失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="app-shell">
    <section class="workspace">
      <div class="heading">
        <p class="eyebrow">devPro</p>
        <h1>基础框架已就绪</h1>
        <p>Vue 3 前端、Spring Boot 后端、MySQL 8.4 与 Docker Compose 已完成基础连接位。</p>
      </div>

      <div class="status-panel">
        <span class="status-dot" :class="{ online: healthStatus?.status === 'UP' }"></span>
        <div>
          <strong>后端健康状态</strong>
          <p v-if="loading">正在检测服务...</p>
          <p v-else-if="errorMessage">{{ errorMessage }}</p>
          <p v-else>{{ healthStatus?.status }} · {{ healthStatus?.time }}</p>
        </div>
      </div>
    </section>
  </main>
</template>

