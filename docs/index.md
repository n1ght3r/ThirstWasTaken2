---
layout: home

# Feature icons are the sidebar's Lucide icons for the same pages.
# The hero is theme/HomeHero.vue, not VitePress's own; it reads its words from here.
thirstHero:
  badge: Minecraft 1.20.1 to 26.3
  name: ThirstWasTaken2
  tagline: Adds a survival thirst bar, drinking, and water purity to Minecraft.
  start:
    text: Get Started
    link: /docs/
  stats:
    - value: 3 loaders
      label: Fabric, NeoForge, Forge
    - value: 9 languages
      label: in game
    - integrations: true
      label: it works with

features:
  - title: Thirst and Quenched
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"/></svg>'
    details: A second bar above hunger. It drains as players move and fight, faster in hot biomes and the Nether.
    link: /docs/features/thirst-and-quenched
    linkText: How it drains
  - title: Water Quality
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10 20a1 1 0 0 0 .553.895l2 1A1 1 0 0 0 14 21v-7a2 2 0 0 1 .517-1.341L21.74 4.67A1 1 0 0 0 21 3H3a1 1 0 0 0-.742 1.67l7.225 7.989A2 2 0 0 1 10 14z"/></svg>'
    details: Water is graded by where it comes from. Boiling makes it safe. Sea water never is.
    link: /docs/features/water-purity
    linkText: Grades and salt
  - title: Ways to Drink
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5.116 4.104A1 1 0 0 1 6.11 3h11.78a1 1 0 0 1 .994 1.105L17.19 20.21A2 2 0 0 1 15.2 22H8.8a2 2 0 0 1-2-1.79z"/><path d="M6 12a5 5 0 0 1 6 0 5 5 0 0 0 6 0"/></svg>'
    details: Bottles, terracotta bowls, a three-drink waterskin, or drinking by hand.
    link: /docs/features/drinking
    linkText: Every container
  - title: Configurable
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"/><circle cx="12" cy="12" r="3"/></svg>'
    details: Every value is a setting, in game or in one config file. The server sets the rules.
    link: /docs/configuration
    linkText: All settings
---
