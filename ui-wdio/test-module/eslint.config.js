import eslint from "@eslint/js";
import * as wdio from 'eslint-plugin-wdio';
import globals from 'globals';

export default [
    eslint.configs.recommended,
    wdio.configs['flat/recommended'],
    {
      languageOptions: {
          globals: {
              ...globals.node,
              ...globals.mocha,
          }
      }
    },
];
