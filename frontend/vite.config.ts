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
	build: {
		rollupOptions: {
			output: {
				manualChunks(id) {
					if (!id.includes('node_modules')) return;

					if (id.includes('highlight.js') || id.includes('marked')) {
						return 'markdown';
					}
					if (id.includes('naive-ui') || id.includes('vueuc') || id.includes('@css-render')) {
						return 'naive-ui';
					}
					if (id.includes('@apollo/client') || id.includes('@vue/apollo-composable') || id.includes('graphql')) {
						return 'apollo';
					}
					if (id.includes('vue-router')) {
						return 'router';
					}
					if (id.includes('pinia')) {
						return 'pinia';
					}
					if (id.includes('dayjs')) {
						return 'dayjs';
					}

					return 'vendor';
				}
			}
		}
	},
	server: {
		port: 5174,
		proxy: {
			'/graphql': {
				target: 'http://localhost:18888/graphql',
				changeOrigin: true
			}
		}
	}
});
