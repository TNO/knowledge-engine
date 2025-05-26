# Documentation

This directory contains the documentation for the Knowledge Engine as well as the required settings that are used to generate the documentation website.
The documentation can be found in the `docs/` directory.


## Generating the website

Required:
- [Node.js](https://nodejs.org/en/download/) version 18.0 or above:
  - When installing Node.js, you are recommended to check all checkboxes related to dependencies.
 
If you want to use the commands below and this is your first time running/generating the website locally, you'll need to run the following command to install the dependencies:
```bash
npm install
```

### Generate website files
To statically generate the website, within this directory (`knowledge-engine/docs/`), run:
```bash
npm run build
```
This will generate a `build/` directory containing all files for the website.

Be careful when editing code blocks with tabs.
Currently, it requires you to leave empty lines around the Markdown syntax



### Test the site locally

If you're working on the documentation, you can locally run a serve so you can easily see what your changes look like.
To run the development server, within this directory (`knowledge-engine/docs/`), execute:
```bash
npm run start
```

The `npm run start` command builds your website locally and serves it through a development server, ready for you to view at http://localhost:3000/.
The site will **automatically reload** and display your changes.
