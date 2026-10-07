<script setup lang="ts">
import { computed } from 'vue'
import { useData, withBase } from 'vitepress'
import HeroSlideshow from './HeroSlideshow.vue'
import { CURSEFORGE, CURSEFORGE_ICON, MODRINTH, MODRINTH_ICON } from '../links'
import { DELIGHT_ADDONS, LETS_DO, INTEGRATIONS, KALEIDOSCOPE_ADDONS, SHOWS_ONLY } from '../mods'

// The home page's hero, in place of VitePress's: its words come from `thirstHero` in index.md.
interface Stat {
  value?: string
  integrations?: boolean
  label: string
}

const { frontmatter } = useData()
const hero = computed(() => frontmatter.value.thirstHero)

// Every mod the "Works with" grid shows, Farmer's Delight and Farm & Charm included.
const integrationCount = SHOWS_ONLY.length + INTEGRATIONS.length + 1 + DELIGHT_ADDONS.length + 1 + LETS_DO.length + 1 + KALEIDOSCOPE_ADDONS.length
const statValue = (stat: Stat) => (stat.integrations ? `${integrationCount} mods` : stat.value)
</script>

<template>
  <section v-if="hero" class="th-hero">
    <div class="th-hero-inner">
      <div class="th-hero-text">
        <p class="th-hero-badge">{{ hero.badge }}</p>

        <h1 class="th-hero-name">{{ hero.name }}</h1>
        <p class="th-hero-tagline">{{ hero.tagline }}</p>

        <div class="th-hero-actions">
          <a class="th-hero-button brand" :href="withBase(hero.start.link)">
            {{ hero.start.text }}
            <span class="vpi-arrow-right th-hero-arrow" />
          </a>
          <a class="th-hero-button alt" :href="MODRINTH" target="_blank" rel="noreferrer">
            <span class="th-hero-button-icon" v-html="MODRINTH_ICON" />
            Modrinth
          </a>
          <a class="th-hero-button alt" :href="CURSEFORGE" target="_blank" rel="noreferrer">
            <span class="th-hero-button-icon" v-html="CURSEFORGE_ICON" />
            CurseForge
          </a>
        </div>

        <dl class="th-hero-stats">
          <div v-for="stat in hero.stats" :key="stat.label" class="th-hero-stat">
            <dt>{{ statValue(stat) }}</dt>
            <dd>{{ stat.label }}</dd>
          </div>
        </dl>
      </div>

      <div class="th-hero-visual">
        <HeroSlideshow />
      </div>
    </div>
  </section>
</template>
