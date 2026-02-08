import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
// import { fileURLToPath, URL } from 'node:url'
import path = require('node:path');

export default defineConfig({
	plugins: [vue()],
	resolve: {
		alias: {
			// '@': fileURLToPath(new URL('./src', import.meta.url))
			'@': path.resolve(__dirname, 'src')
		}
	},
	server: {
		port: 5173
	}
});
