<template>
  <div class="about">
    <h1>This is an about page</h1>
  </div>
</template>

<script>
export default {
  name: 'AboutView'
}
</script>

<style scoped>
.about { min-height:calc(100vh - 68px); color:var(--frcs-text); }
.about h1, .about h2 { color:var(--frcs-primary); }
.about a { color:var(--frcs-primary-2); }
</style>
