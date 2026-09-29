<script setup lang="ts">
import { nextTick, onMounted, watch } from 'vue'
import { useRoute } from 'vitepress'
import DefaultTheme from 'vitepress/theme'
import HeroSlideshow from './HeroSlideshow.vue'
import HomeIntegrations from './HomeIntegrations.vue'

const { Layout } = DefaultTheme
const route = useRoute()

// VitePress keeps the same page element between routes, so the fade-in (.page-enter in custom.css)
// is restarted by hand on every navigation.
function playPageEnter() {
  const content = document.querySelector('.VPContent')
  if (!content) return
  content.classList.remove('page-enter')
  void (content as HTMLElement).offsetWidth
  content.classList.add('page-enter')
}

onMounted(playPageEnter)
watch(
  () => route.path,
  () => nextTick(playPageEnter)
)
</script>

<template>
  <Layout>
    <template #home-hero-image>
      <HeroSlideshow />
    </template>
    <template #home-features-after>
      <HomeIntegrations />
    </template>
  </Layout>
</template>
