import { defineConfig } from 'vitepress'
import { CURSEFORGE, CURSEFORGE_ICON, MODRINTH, MODRINTH_ICON, REPO } from './links'
import { DELIGHT_ADDONS, FARM_AND_CHARM, LETS_DO, FARMERS_DELIGHT, INTEGRATIONS, type Mod } from './mods'

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
      },
      {
        // The Let's Do mods under Farm & Charm, the same way.
        ...sidebarMod(FARM_AND_CHARM),
        collapsed: true,
        items: LETS_DO.map(sidebarMod)
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
