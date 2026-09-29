<script setup lang="ts">
import { computed } from 'vue'
import { useData, withBase } from 'vitepress'
import HeroSlideshow from './HeroSlideshow.vue'
import { CURSEFORGE, CURSEFORGE_ICON, MODRINTH, MODRINTH_ICON } from '../links'
import { DELIGHT_ADDONS, INTEGRATIONS, SHOWS_ONLY } from '../mods'

// The home page's hero, in place of VitePress's: its words come from `thirstHero` in index.md.
interface Stat {
  value?: string
  integrations?: boolean
  label: string
}

const { frontmatter } = useData()
const hero = computed(() => frontmatter.value.thirstHero)

// Every mod the "Works with" grid shows, Farmer's Delight included.
const integrationCount = SHOWS_ONLY.length + INTEGRATIONS.length + 1 + DELIGHT_ADDONS.length
const statValue = (stat: Stat) => (stat.integrations ? `${integrationCount} mods` : stat.value)

// A thirst bar of ten droplets that fills from the right, as it does in game.
const DROPLETS = 10

// Items of the mod that float around the slideshow.
const floaters = [
  { src: '/icons/hero/copper-canteen.png', class: 'floater-a' },
  { src: '/icons/hero/iron-flask.png', class: 'floater-b' },
  { src: '/icons/hero/water-bowl.png', class: 'floater-c' },
  { src: '/icons/hero/waterskin.png', class: 'floater-d' }
]
</script>

<template>
  <section v-if="hero" class="th-hero">
    <div class="th-hero-backdrop" aria-hidden="true" />

    <div class="th-hero-inner">
      <div class="th-hero-text">
        <p class="th-hero-badge">
          <img :src="withBase('/logo-small.png')" alt="" class="th-hero-badge-logo" />
          <span>{{ hero.badge }}</span>
        </p>

        <h1 class="th-hero-name">{{ hero.name }}</h1>
        <p class="th-hero-tagline">{{ hero.tagline }}</p>

        <div class="th-hero-bar" role="img" aria-label="A thirst bar filling up">
          <span
            v-for="n in DROPLETS"
            :key="n"
            class="th-hero-droplet"
            :style="{ '--i': DROPLETS - n }"
          >
            <img :src="withBase('/icons/hero/droplet-empty.png')" alt="" class="empty" />
            <img :src="withBase('/icons/hero/droplet-full.png')" alt="" class="full" />
          </span>
        </div>

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
        <div class="th-hero-glow" aria-hidden="true" />
        <img
          v-for="item in floaters"
          :key="item.src"
          :src="withBase(item.src)"
          alt=""
          aria-hidden="true"
          class="th-hero-floater"
          :class="item.class"
        />
        <HeroSlideshow />
      </div>
    </div>
  </section>
</template>
