<script setup lang="ts">
const featureList = ['模拟面试问答', '追问打分与反馈', '个性化题库']
</script>

<template>
  <section class="ai-interviewer-panel">
    <header class="panel-header">
      <p class="panel-kicker">新功能入口</p>
      <h1 class="panel-title">AI面试官</h1>
      <p class="panel-subtitle">这里会承载你后续扩建的面试训练能力，当前先预留页面结构。</p>
    </header>

    <div class="panel-body">
      <div class="status-card">
        <p class="status-title">开发状态</p>
        <p class="status-value">准备中</p>
        <p class="status-text">你可以继续在“简历编辑”里完成简历，后续再逐步接入面试流程。</p>
      </div>

      <div class="todo-card">
        <p class="todo-title">预留模块</p>
        <ul class="todo-list">
          <li v-for="item in featureList" :key="item">{{ item }}</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ai-interviewer-panel {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(145deg, #f7f2ec 0%, #f1e5d8 100%);
}

.panel-header {
  padding: 20px;
  border: 1px solid #e4d8cb;
  border-radius: 14px;
  background: #fff;
}

.panel-kicker {
  color: #8a7461;
  font-size: 12px;
  font-weight: 700;
}

.panel-title {
  margin-top: 6px;
  color: #2d2521;
  font-size: 30px;
  line-height: 1.1;
  font-weight: 800;
}

.panel-subtitle {
  margin-top: 10px;
  color: #7b6a5b;
  font-size: 14px;
}

.panel-body {
  margin-top: 14px;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.status-card,
.todo-card {
  border-radius: 12px;
  border: 1px solid #e4d8cb;
  background: #fff;
  padding: 16px;
}

.status-title,
.todo-title {
  color: #8a7461;
  font-size: 12px;
  font-weight: 700;
}

.status-value {
  margin-top: 4px;
  color: #2d2521;
  font-size: 22px;
  line-height: 1;
  font-weight: 800;
}

.status-text {
  margin-top: 10px;
  color: #7b6a5b;
  font-size: 13px;
}

.todo-list {
  margin-top: 10px;
  list-style: none;
  display: grid;
  gap: 8px;
}

.todo-list li {
  border-radius: 8px;
  border: 1px solid #eaddcf;
  background: #f8f3ed;
  color: #4a3f36;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 10px;
}

@media (max-width: 960px) {
  .panel-body {
    grid-template-columns: 1fr;
  }
}
</style>
