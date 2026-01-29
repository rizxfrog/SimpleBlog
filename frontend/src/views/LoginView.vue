<template>
	<div class="auth-page">
		<section class="auth-card fade-up">
			<div class="auth-header">
				<span class="auth-title">登入</span>
			</div>
			<n-form label-placement="top">
				<n-form-item>
					<n-input v-model:value="form.username" placeholder="手机号/账号" class="auth-input" size="large">
						<template #prefix>
							<Icon class="input-icon" icon="mdi:account" />
						</template>
					</n-input>
				</n-form-item>
				<n-form-item>
					<n-input v-model:value="form.password" type="password" placeholder="密码" class="auth-input" size="large">
						<template #prefix>
							<Icon class="input-icon" icon="mdi:lock" />
						</template>
					</n-input>
				</n-form-item>
				<n-button type="primary" class="btn btn-primary" @click="onSubmit">登入</n-button>
			</n-form>
			<p v-if="error" class="auth-error">{{ error }}</p>
		</section>
	</div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMutation } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { Icon } from '@iconify/vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const error = ref('');

const form = reactive({
	username: '',
	password: ''
});

const { mutate } = useMutation(gql`
	mutation Login($input: LoginInput!) {
		login(input: $input) {
			token
			user {
				id
				username
			}
		}
	}
`);

const onSubmit = async () => {
	error.value = '';
	try {
		const result = await mutate({ input: form });
		const token = result?.data?.login?.token;
		if (token) {
			auth.setToken(token);
			const redirect = (route.query.redirect as string) || '/admin';
			router.push(redirect);
		}
	} catch (e: any) {
		error.value = e?.message || '登录失败';
	}
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;600&display=swap');

.auth-page {
	//min-height: 100%;
	//display: grid;
	//grid-template-columns: minmax(280px, 420px) minmax(240px, 1fr);
	//gap: 32px;
	//align-items: center;
	//padding: 32px 6vw;
	//background: radial-gradient(circle at 15% 20%, #fff3e6 0%, #ffffff 50%, #eef6ff 100%);
	//font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
}

.auth-card {
	background: #ffffff;
	border-radius: 20px;
	padding: 28px 28px 24px;
	box-shadow: 0 20px 60px rgba(15, 23, 42, 0.12);
	width: min(360px, 90vw);
	justify-self: center;
}

.auth-header {
	display: grid;
	gap: 6px;
	margin-bottom: 20px;
}

.auth-title {
	text-align: center;
	font-size: 22px;
	font-weight: 600;
	color: #0f172a;
}

.eyebrow {
	font-size: 12px;
	letter-spacing: 0.18em;
	text-transform: uppercase;
	color: #f97316;
}

.auth-header h2 {
	margin: 0;
	font-size: 28px;
	font-weight: 600;
	color: #0f172a;
}

.auth-header p {
	margin: 0;
	color: #64748b;
	font-size: 14px;
}

.btn-primary {
	width: 100%;
	height: 44px;
	border-radius: 12px;
	font-weight: 600;
	letter-spacing: 0.04em;
	display: flex;
	justify-content: center;
	align-items: center;
}

.auth-input :deep(.n-input) {
	border-radius: 14px;
	background: #2b3646;
	border: 1px solid #3a4658;
	box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.1);
}

.auth-input :deep(.n-input__input-el) {
	color: #e2e8f0;
}

.auth-input :deep(.n-input__input-el::placeholder) {
	color: #93a4b8;
}

.auth-input :deep(.n-input__prefix) {
	color: #a5b4c3;
}

.input-icon {
	width: 18px;
	height: 18px;
}

.auth-error {
	margin-top: 12px;
	color: #dc2626;
	font-size: 13px;
}

.visual {
	display: grid;
	place-items: center;
	gap: 12px;
	padding: 24px;
	border-radius: 24px;
	background: linear-gradient(135deg, #fff7ed 0%, #eef2ff 100%);
	border: 1px solid rgba(15, 23, 42, 0.08);
}

.visual svg {
	width: min(220px, 64vw);
	height: auto;
}

.visual-text {
	margin: 0;
	color: #1f2937;
	font-size: 14px;
}

@media (max-width: 900px) {
	.auth-page {
		grid-template-columns: 1fr;
		padding: 32px 6vw;
	}

	.visual {
		order: -1;
	}
}
</style>
