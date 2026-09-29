import { defineConfig } from 'vitepress'
import { DELIGHT_ADDONS, FARMERS_DELIGHT, INTEGRATIONS, type Mod } from './mods'

const REPO = 'https://github.com/n1ght3r/ThirstWasTaken2'
const MODRINTH = 'https://modrinth.com/mod/thirst-was-taken-2'
// Simple Icons' Modrinth mark, the same one the SmartSpawner docs use.
const MODRINTH_ICON = '<svg role="img" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><title>Modrinth</title><path d="M12.252.004a11.78 11.768 0 0 0-8.92 3.73 11 10.999 0 0 0-2.17 3.11 11.37 11.359 0 0 0-1.16 5.169c0 1.42.17 2.5.6 3.77.24.759.77 1.899 1.17 2.529a12.3 12.298 0 0 0 8.85 5.639c.44.05 2.54.07 2.76.02.2-.04.22.1-.26-1.7l-.36-1.37-1.01-.06a8.5 8.489 0 0 1-5.18-1.8 5.34 5.34 0 0 1-1.3-1.26c0-.05.34-.28.74-.5a37.572 37.545 0 0 1 2.88-1.629c.03 0 .5.45 1.06.98l1 .97 2.07-.43 2.06-.43 1.47-1.47c.8-.8 1.48-1.5 1.48-1.52 0-.09-.42-1.63-.46-1.7-.04-.06-.2-.03-1.02.18-.53.13-1.2.3-1.45.4l-.48.15-.53.53-.53.53-.93.1-.93.07-.52-.5a2.7 2.7 0 0 1-.96-1.7l-.13-.6.43-.57c.68-.9.68-.9 1.46-1.1.4-.1.65-.2.83-.33.13-.099.65-.579 1.14-1.069l.9-.9-.7-.7-.7-.7-1.95.54c-1.07.3-1.96.53-1.97.53-.03 0-2.23 2.48-2.63 2.97l-.29.35.28 1.03c.16.56.3 1.16.31 1.34l.03.3-.34.23c-.37.23-2.22 1.3-2.84 1.63-.36.2-.37.2-.44.1-.08-.1-.23-.6-.32-1.03-.18-.86-.17-2.75.02-3.73a8.84 8.839 0 0 1 7.9-6.93c.43-.03.77-.08.78-.1.06-.17.5-2.999.47-3.039-.01-.02-.1-.02-.2-.03Zm3.68.67c-.2 0-.3.1-.37.38-.06.23-.46 2.42-.46 2.52 0 .04.1.11.22.16a8.51 8.499 0 0 1 2.99 2 8.38 8.379 0 0 1 2.16 3.449 6.9 6.9 0 0 1 .4 2.8c0 1.07 0 1.27-.1 1.73a9.37 9.369 0 0 1-1.76 3.769c-.32.4-.98 1.06-1.37 1.38-.38.32-1.54 1.1-1.7 1.14-.1.03-.1.06-.07.26.03.18.64 2.56.7 2.78l.06.06a12.07 12.058 0 0 0 7.27-9.4c.13-.77.13-2.58 0-3.4a11.96 11.948 0 0 0-5.73-8.578c-.7-.42-2.05-1.06-2.25-1.06Z"/></svg>'
const CURSEFORGE = 'https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2'
// Simple Icons' CurseForge mark.
const CURSEFORGE_ICON = '<svg role="img" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><title>CurseForge</title><path d="M18.326 9.2145S23.2261 8.4418 24 6.1882h-7.5066V4.4H0l2.0318 2.3576V9.173s5.1267-.2665 7.1098 1.2372c2.7146 2.516-3.053 5.917-3.053 5.917L5.0995 19.6c1.5465-1.4726 4.494-3.3775 9.8983-3.2857-2.0565.65-4.1245 1.6651-5.7344 3.2857h10.9248l-1.0288-3.2726s-7.918-4.6688-.8336-7.1127z"/></svg>'
const BASE = process.env.VITEPRESS_BASE || '/'

// A sidebar entry with the mod's icon before its name. VitePress renders sidebar text as HTML.
const sidebarMod = (mod: Mod) => ({
  text: `<img class="sidebar-mod-icon${mod.large ? ' sidebar-mod-icon-lg' : ''}" src="${mod.icon.replace(/&/g, '&amp;')}" alt=""><span>${mod.name}</span>`,
  link: mod.link
})

// A page of this mod with one of its own textures before the name, from public/icons/sidebar/. Sidebar
// HTML is not rewritten for the base path, so the base is added here.
const pageIcon = (name: string, file: string) =>
  `<img class="sidebar-mod-icon sidebar-pixel-icon" src="${BASE.replace(/\/?$/, '/')}icons/sidebar/${file}" alt=""><span>${name}</span>`

// Lucide icons (lucide.dev, ISC), inline so they take the text colour. Only the paths are kept.
const LUCIDE = {
  home: '<path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
  overview: '<path d="M12 5v16"/><path d="M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z"/>',
  download: '<path d="M12 15V3"/><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5"/>',
  help: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/>',
  terminal: '<path d="M12 19h8"/><path d="m4 17 6-6-6-6"/>',
  settings: '<path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"/><circle cx="12" cy="12" r="3"/>',
  package: '<path d="M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"/><path d="M12 22V12"/><polyline points="3.29 7 12 12 20.71 7"/><path d="m7.5 4.27 9 5.15"/>',
  code: '<path d="m16 18 6-6-6-6"/><path d="m8 6-6 6 6 6"/>'
}
const lucide = (name: string, icon: keyof typeof LUCIDE) =>
  `<svg class="sidebar-mod-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${LUCIDE[icon]}</svg><span>${name}</span>`

const manualSidebar = [
  {
    text: 'Getting Started',
    items: [
      { text: lucide('Overview', 'overview'), link: '/docs/' },
      { text: lucide('Installation', 'download'), link: '/docs/installation' },
      { text: lucide('FAQ', 'help'), link: '/docs/faq' }
    ]
  },
  {
    text: 'Features',
    items: [
      { text: pageIcon('Thirst and Quenched', 'thirst.png'), link: '/docs/features/thirst-and-quenched' },
      { text: pageIcon('Drinking', 'drinking.png'), link: '/docs/features/drinking' },
      { text: pageIcon('Water Purity', 'water-purity.png'), link: '/docs/features/water-purity' }
    ]
  },
  {
    // Mods that change what this one does get a page each. AppleSkin and Jade only show what is
    // already there, so they are covered on the feature pages instead.
    // The class lets custom.css space this group's icon rows apart from the others.
    text: '<span class="sidebar-integrations">Integrations</span>',
    items: [
      ...INTEGRATIONS.map(sidebarMod),
      {
        // Farmer's Delight and its addons, together. Closed until opened, then open until closed.
        ...sidebarMod(FARMERS_DELIGHT),
        collapsed: true,
        items: DELIGHT_ADDONS.map(sidebarMod)
      }
    ]
  },
  {
    text: 'Server Guide',
    items: [
      { text: lucide('Commands', 'terminal'), link: '/docs/commands' },
      { text: lucide('Configuration', 'settings'), link: '/docs/configuration' }
    ]
  },
  {
    text: 'For Developers',
    items: [
      { text: lucide('Data Packs', 'package'), link: '/docs/developers/data-packs' },
      { text: lucide('Java API', 'code'), link: '/docs/developers/java-api' }
    ]
  }
]

export default defineConfig({
  base: BASE,
  lang: 'en',
  title: 'ThirstWasTaken2',
  description: 'Adds a survival thirst bar, drinking, and water purity.',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: `${BASE}logo.png` }]
  ],
  // Notes, paste sources and developer planning for maintainers, not pages on the site.
  // 'dev/**' keeps docs/dev out of the build: those pages link into src/ and the repo root,
  // which the dead-link check cannot follow.
  srcExclude: ['AGENTS.md', 'MODRINTH.md', 'CURSEFORGE.md', 'dev/**'],
  themeConfig: {
    logo: '/logo.png',
    externalLinkIcon: true,
    socialLinks: [
      { icon: 'github', link: REPO },
      { icon: { svg: MODRINTH_ICON }, link: MODRINTH, ariaLabel: 'Modrinth' },
      { icon: { svg: CURSEFORGE_ICON }, link: CURSEFORGE, ariaLabel: 'CurseForge' }
    ],
    search: {
      provider: 'local'
    },
    nav: [
      { text: lucide('Home', 'home'), link: '/', activeMatch: '^/$' },
      { text: lucide('Docs', 'overview'), link: '/docs/', activeMatch: '^/docs/' }
    ],
    sidebar: {
      '/docs/': manualSidebar
    },
    editLink: {
      pattern: `${REPO}/edit/main/docs/:path`,
      text: 'Edit this page on GitHub'
    },
    outline: {
      level: [2, 3],
      label: 'On this page'
    },
    docFooter: {
      prev: 'Previous page',
      next: 'Next page'
    },
    lastUpdated: {
      text: 'Last updated',
      formatOptions: {
        dateStyle: 'medium',
        timeStyle: 'short'
      }
    }
  }
})
