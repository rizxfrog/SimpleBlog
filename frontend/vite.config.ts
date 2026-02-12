import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
	plugins: [vue()],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url))
		}
	},
	server: {
		port: 5173,
		proxy: {
			'/graphql': {
				target: 'http://142.171.2.134:18888/graphql',
				changeOrigin: true
			}
		}
	}
});
