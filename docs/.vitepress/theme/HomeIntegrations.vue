<script setup lang="ts">
import { withBase } from 'vitepress'
import { DELIGHT_ADDONS, FARM_AND_CHARM, LETS_DO, FARMERS_DELIGHT, INTEGRATIONS, SHOWS_ONLY, type Mod } from '../mods'

// The same mods and icons as the sidebar's Integrations group, plus AppleSkin and Jade.
const groups: { title: string; mods: Mod[] }[] = [
  { title: 'Integrations', mods: [...SHOWS_ONLY, ...INTEGRATIONS] },
  { title: "Farmer's Delight and its addons", mods: [FARMERS_DELIGHT, ...DELIGHT_ADDONS] },
  { title: "Let's Do mods", mods: [FARM_AND_CHARM, ...LETS_DO] }
]
</script>

<template>
  <section class="home-integrations">
    <div class="home-integrations-inner">
      <h2 class="home-integrations-title">Works with</h2>
      <p class="home-integrations-lead">
        Optional. Install any of these and the mod adapts to it: nothing extra to set up.
      </p>
      <div v-for="group in groups" :key="group.title" class="home-integrations-group">
        <h3 class="home-integrations-group-title">{{ group.title }}</h3>
        <div class="home-integrations-grid">
          <a
            v-for="mod in group.mods"
            :key="mod.name"
            class="home-integration"
            :href="withBase(mod.link)"
          >
            <img
              class="home-integration-icon"
              :class="{ 'home-integration-icon-lg': mod.large }"
              :src="mod.icon"
              alt=""
              loading="lazy"
            />
            <span class="home-integration-name">{{ mod.name }}</span>
          </a>
        </div>
      </div>
    </div>
  </section>
</template>
