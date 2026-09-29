<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { withBase } from 'vitepress'

// The home page's hero image: in-game shots of the mod and the mods it works with, cycling on their own.
const slides = [
  { src: '/screenshots/water/iron-hanging-pot.png', alt: 'An Iron Hanging Pot of water boiling over a campfire', label: 'Hanging Pots' },
  { src: '/screenshots/water/jade-water.png', alt: 'Jade showing the grade of the water under the crosshair', label: 'Water grades with Jade' },
  { src: '/screenshots/integrations/create/create-sand-filter-goggles.png', alt: "Engineer's Goggles showing Murky water entering the Sand Filter and Clean water leaving it", label: 'Create Sand Filter' },
  { src: '/screenshots/integrations/supplementaries/supplementaries-jars.png', alt: 'Five jars of water on a lakeshore, with Jade naming the middle one Clean', label: 'Supplementaries Jars' },
  { src: '/screenshots/integrations/kaleidoscope-cookery/kaleidoscope-teapot.png', alt: 'A Teapot on a lit Stove among teacups in a cherry grove, with Jade naming its water Clean', label: 'Kaleidoscope Cookery Teapot' },
  { src: '/screenshots/integrations/cold-sweat/cold-sweat-boiler.png', alt: 'A lit Boiler on the snow between a campfire warming two Waterskins and a water cauldron', label: 'Cold Sweat Boiler' },
  { src: '/screenshots/integrations/brewin-and-chewin/brewin-keg.png', alt: "A Brewin' and Chewin' Keg beside barrels on a lakeshore, with Jade naming its water Dirty", label: "Brewin' and Chewin' Keg" },
  { src: '/screenshots/integrations/sophisticated/sophisticated-drinking-upgrade.png', alt: "A backpack of drinks with the Advanced Drinking Upgrade's settings open", label: 'Sophisticated Drinking Upgrade' },
  { src: '/screenshots/integrations/supplementaries/supplementaries-faucet.png', alt: "A Faucet pouring from a cauldron into a Copper Hanging Pot, with Jade naming the pot's water Murky", label: 'Supplementaries Faucet' }
]

const INTERVAL_MS = 2500

const active = ref(0)
let timer: number | undefined
let reduceMotion: MediaQueryList | undefined

function stop() {
  if (timer !== undefined) {
    window.clearInterval(timer)
    timer = undefined
  }
}

function start() {
  stop()
  // Nothing moves for a reader who asked the system for less motion; the dots still work.
  if (reduceMotion?.matches) return
  timer = window.setInterval(() => {
    active.value = (active.value + 1) % slides.length
  }, INTERVAL_MS)
}

function select(index: number) {
  active.value = index
  start()
}

onMounted(() => {
  reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
  reduceMotion.addEventListener('change', start)
  start()
})

onBeforeUnmount(() => {
  stop()
  reduceMotion?.removeEventListener('change', start)
})
</script>

<template>
  <div
    class="hero-slideshow"
    role="region"
    aria-roledescription="carousel"
    aria-label="ThirstWasTaken2 in game"
    @mouseenter="stop"
    @mouseleave="start"
    @focusin="stop"
    @focusout="start"
  >
    <div class="hero-slideshow-frame">
      <img
        v-for="(slide, index) in slides"
        :key="slide.src"
        :src="withBase(slide.src)"
        :alt="slide.alt"
        class="hero-slideshow-image"
        :class="{ active: index === active }"
        :aria-hidden="index !== active"
        :loading="index === 0 ? 'eager' : 'lazy'"
      >
      <!-- One label per slide, so each fades with its image instead of switching at once. -->
      <span
        v-for="(slide, index) in slides"
        :key="`label-${slide.src}`"
        class="hero-slideshow-label"
        :class="{ active: index === active }"
        :aria-hidden="index !== active"
      >{{ slide.label }}</span>
    </div>

    <div class="hero-slideshow-controls" aria-label="Choose an image">
      <button
        v-for="(slide, index) in slides"
        :key="slide.label"
        type="button"
        :class="{ active: index === active }"
        :aria-label="`Show ${slide.label}`"
        :aria-current="index === active ? 'true' : undefined"
        @click="select(index)"
      />
    </div>
  </div>
</template>
