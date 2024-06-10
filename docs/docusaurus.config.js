// @ts-check
// `@type` JSDoc annotations allow editor autocompletion and type checking
// (when paired with `@ts-check`).
// There are various equivalent ways to declare your Docusaurus config.
// See: https://docusaurus.io/docs/api/docusaurus-config

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
module.exports = {
  title: 'Knowledge Engine',
  tagline: 'Intelligently exchanging data',
  favicon: 'img/favicon.ico',
  url: 'https://docs.knowledge-engine.eu',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/', // Serve docs at site's root
          sidebarPath: './sidebars.js',
        },
        blog: false, // disable blog plugin
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/docusaurus-social-card.jpg',
      navbar: {
        title: '',
        logo: {
          alt: 'Knowledge Engine logo',
          src: 'img/ke_text.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Documentation',
          },
		  {
		    href: 'https://www.knowledge-engine.eu/blog',
			label: 'Blog',
			position: 'right',
		  },
          {
            href: 'https://github.com/TNO/knowledge-engine',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        copyright: `Copyright © ${new Date().getFullYear()} TNO Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        // Be careful, ordering of languages in array for additionalLanguages seems to matter because of dependencies!
        // See: https://github.com/facebook/docusaurus/issues/5013
        additionalLanguages: ['bash', 'json', 'java', 'turtle', 'sparql'],
      },
    }),

  //  Adds plugin for offline/local search functionality
  themes: [
    [
        require.resolve("@easyops-cn/docusaurus-search-local"),
        /** @type {import("@easyops-cn/docusaurus-search-local").PluginOptions} */
        ({
            hashed: true,
            docsRouteBasePath: "/",
            indexBlog: false,
        }),
    ],
  ],
};