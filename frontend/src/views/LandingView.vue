<template>
	<n-config-provider :theme="naiveTheme" :theme-overrides="naiveThemeOverrides">
		<div class="landing container">
			<section class="hero-grid">
				<div class="hero-copy">
					<n-text depth="3" class="eyebrow">SimpleBlog · GraphQL</n-text>
					<n-h1 class="hero-title">SimpleBlog</n-h1>
					<n-p depth="2" class="hero-description">专注 Java、Go、Python 编程语言与计算机基础、计算机网络，分享技术干货</n-p>

					<n-space class="hero-actions" size="large">
						<AwesomeButton label="开始阅读" @click="router.push('/blog')" />
						<n-button size="large" round ghost @click="router.push('/discover')">探索</n-button>
					</n-space>

					<n-grid :x-gap="24" :cols="3" class="hero-metrics">
						<n-gi>
							<n-statistic label="精选文章">
								<span class="metric-value">120+</span>
							</n-statistic>
						</n-gi>
						<n-gi>
							<n-statistic label="主题路径">
								<span class="metric-value">18</span>
							</n-statistic>
						</n-gi>
						<n-gi>
							<n-statistic label="周更新节奏">
								<span class="metric-value">7d</span>
							</n-statistic>
						</n-gi>
					</n-grid>
				</div>

				<div class="hero-panel">
					<div class="mac-window">
						<!--						<div class="mac-bar">-->
						<!--							<div class="mac-dots">-->
						<!--								<span class="dot dot-close"></span>-->
						<!--								<span class="dot dot-min"></span>-->
						<!--								<span class="dot dot-max"></span>-->
						<!--							</div>-->
						<!--							<span class="mac-title">主题</span>-->
						<!--						</div>-->
						<MacPlate class="mac-body" :background-color="macPlateBackground">
							<!--							<n-card hoverable class="topic-card">-->
							<n-space wrap>
								<n-button v-for="item in topics" :key="item.label" secondary round type="info" @click="router.push(item.to)">
									{{ item.label }}
								</n-button>
							</n-space>
							<!--							</n-card>-->
						</MacPlate>
					</div>
				</div>
			</section>
		</div>
	</n-config-provider>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { computed } from 'vue';
import { useUiStore } from '@/stores/ui';
import AwesomeButton from '@/components/bottons/AwesomeButton.vue';
import MacPlate from '@/components/plates/MacPlate.vue';
import { NButton, NConfigProvider, NText, NH1, NP, NSpace, NGrid, NGi, NStatistic, NCard, darkTheme } from 'naive-ui';

const router = useRouter();
const ui = useUiStore();

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

const macPlateBackground = computed(() => (naiveTheme.value === darkTheme ? '#555' : '#f8fbfe'));

const naiveThemeOverrides = computed(() => {
	if (naiveTheme.value !== darkTheme) {
		return null;
	}
	return {
		common: {
			primaryColor: '#66d9d0',
			primaryColorHover: '#7fe3da',
			primaryColorPressed: '#55c6bd',
			bodyColor: 'transparent',
			cardColor: 'rgba(16, 24, 34, 0.65)'
		},
		Card: {
			color: 'rgba(16, 24, 34, 0.65)',
			borderColor: 'rgba(120, 190, 210, 0.16)',
			boxShadow: '0 18px 40px rgba(6, 12, 18, 0.45)'
		},
		Button: {
			color: 'rgba(18, 32, 40, 0.45)',
			colorHover: 'rgba(20, 36, 46, 0.6)',
			colorPressed: 'rgba(14, 28, 36, 0.7)',
			textColor: '#d7f4f0',
			textColorHover: '#e7fbf8',
			textColorPressed: '#bfece6'
		}
	};
});

const topics = [
	{ label: 'Java', to: '/discover' },
	{ label: 'Go', to: '/discover' },
	{ label: 'Python', to: '/discover' },
	{ label: 'GraphQL', to: '/discover' },
	{ label: '计算机网络', to: '/discover' },
	{ label: '计算机基础', to: '/discover' }
];
</script>

<style scoped>
.landing {
	padding: 60px 0;
}

.mac-window {
	border-radius: 9px;
	overflow: hidden;
	/*border: 0px solid var(--line);*/
	background: rgba(255, 255, 255, 0.7);
	box-shadow: 0 10px 10px rgba(6, 12, 18, 0.18);
	backdrop-filter: blur(6px);
}

.mac-bar {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 10px 14px;
	background: rgba(250, 252, 255, 0.85);
	border-bottom: 1px solid rgba(120, 140, 160, 0.2);
}

.mac-dots {
	display: flex;
	gap: 7px;
}

.dot {
	width: 11px;
	height: 11px;
	border-radius: 50%;
	box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12);
}

.dot-close {
	background: #ff5f57;
}

.dot-min {
	background: #febc2e;
}

.dot-max {
	background: #28c840;
}

.mac-title {
	font-size: 0.85rem;
	color: rgba(16, 20, 26, 0.6);
	letter-spacing: 0.04em;
	text-transform: uppercase;
	margin-left: auto;
}

.card.mac-body {
	padding: 10px;
}
.mac-bar {
	background: rgb(5, 39, 64);
}

:global([data-theme='dark']) .mac-window {
	background: rgba(20, 26, 32, 0.95);
	border-color: rgba(120, 170, 190, 0.22);
	box-shadow: 0 30px 70px rgba(4, 10, 16, 0.6);
}

:global([data-theme='dark']) .mac-bar {
	background: rgba(24, 30, 38, 0.95);
	border-bottom-color: rgba(120, 170, 190, 0.2);
}

:global([data-theme='dark']) .mac-title {
	color: rgba(220, 240, 245, 0.78);
}

:global([data-theme='dark']) .mac-window .dot {
	box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}

.hero-grid {
	display: grid;
	grid-template-columns: 1.2fr 0.8fr;
	gap: 40px;
	align-items: center;
}

.hero-title {
	font-size: 3.5rem;
	margin: 12px 0;
	font-weight: 800;
}

.eyebrow {
	letter-spacing: 2px;
	text-transform: uppercase;
	font-weight: 600;
}

.hero-description {
	font-size: 1.2rem;
	margin-bottom: 32px;
}

.hero-metrics {
	margin-top: 48px;
}

.metric-value {
	font-weight: 700;
}

.topic-card {
	border-radius: 16px;
}

/* 暗色主题适配 */
:global([data-theme='dark']) .landing {
	background: radial-gradient(circle at 18% 10%, hsla(185 70% 55% / 0.18), transparent 45%), radial-gradient(circle at 85% 0%, hsla(210 65% 50% / 0.16), transparent 40%), linear-gradient(180deg, hsla(210 24% 12% / 0.85), transparent 70%);
}

:global([data-theme='dark']) .topic-card {
	background: rgba(42, 50, 58, 0.75);
	border: 1px solid rgba(120, 170, 190, 0.18);
	box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

/* 响应式调整 */
@media (max-width: 768px) {
	.hero-grid {
		grid-template-columns: 1fr;
	}
	.hero-title {
		font-size: 2.5rem;
	}
}
</style>
