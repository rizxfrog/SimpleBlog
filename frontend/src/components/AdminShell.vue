<template>
	<n-config-provider :theme="naiveTheme">
		<div class="admin-shell">
			<aside class="admin-sidebar">
				<RouterLink class="admin-brand" to="/">SimpleBlog</RouterLink>
				<div class="admin-search">
					<input type="text" placeholder="搜索内容" />
					<span>Ctrl + K</span>
				</div>
				<n-menu class="admin-menu" :options="menuOptions" :value="activeMenuKey" @update:value="handleMenuSelect" />
				<div class="admin-profile">
					<div class="avatar"></div>
					<div>
						<strong>Administrator</strong>
						<span>站点管理</span>
					</div>
				</div>
			</aside>

			<section class="admin-main">
				<header class="admin-topbar">
					<div>
						<h1>{{ title }}</h1>
						<p v-if="subtitle">{{ subtitle }}</p>
					</div>
					<div class="admin-actions">
						<slot name="actions" />
					</div>
				</header>
				<div class="admin-content">
					<slot />
				</div>
			</section>
		</div>
	</n-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { darkTheme } from 'naive-ui';
import type { MenuOption } from 'naive-ui';
import { useUiStore } from '@/stores/ui';

defineProps<{ title: string; subtitle?: string }>();

const ui = useUiStore();
const route = useRoute();
const router = useRouter();

const menuOptions: MenuOption[] = [
	{ label: '仪表盘', key: '/admin' },
	{ label: '文章', key: '/admin/posts' },
	{ label: '评论', key: '/admin/comments' },
	{ label: '文档', key: '/admin/docs' },
	{ label: '新建文章', key: '/admin/editor' },
	{ label: '设置', key: '/admin/settings' }
];

const menuKeys = menuOptions.map(option => String(option.key)).sort((a, b) => b.length - a.length);

const activeMenuKey = computed(() => {
	const match = menuKeys.find(key => route.path === key || route.path.startsWith(`${key}/`));
	return match ?? '/admin';
});

const handleMenuSelect = (key: string | number) => {
	const target = String(key);
	if (target !== route.path) {
		void router.push(target);
	}
};

const naiveTheme = computed(() => {
	if (ui.theme === 'dark') {
		return darkTheme;
	}
	if (ui.theme === 'system') {
		const prefersDark = typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches;
		return prefersDark ? darkTheme : null;
	}
	return null;
});
</script>
