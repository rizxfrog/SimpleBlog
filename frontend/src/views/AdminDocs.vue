<template>
	<AdminShell title="文档库" subtitle="按项目管理文档系统">
		<template #actions>
			<n-space size="small">
				<n-button size="small" secondary @click="refreshAll">刷新</n-button>
				<n-button size="small" type="primary" @click="openCreateModal">新建项目</n-button>
			</n-space>
		</template>

		<section class="docs-project-grid">
			<article v-for="project in projectList" :key="project" class="card project-card">
				<div class="project-head">
					<h3>{{ project }}</h3>
					<span v-if="project === defaultProject" class="project-tag">default</span>
				</div>
				<p class="project-meta">共 {{ getDocCount(project) }} 篇文档</p>
				<p class="project-path">/docs/{{ project }}</p>
				<div class="project-actions">
					<n-button size="small" @click="visitProject(project)">访问</n-button>
					<n-button size="small" type="primary" @click="editProject(project)">编辑</n-button>
					<n-button size="small" @click="openSettings(project)">设置</n-button>
				</div>
			</article>

			<button class="project-card project-card-new" type="button" @click="openCreateModal">
				<span class="new-icon">+</span>
				<span>新建项目</span>
			</button>
		</section>

		<p v-if="message" class="docs-message">{{ message }}</p>

		<n-modal v-model:show="showCreateModal" preset="card" title="新建文档项目" class="docs-modal" :mask-closable="false">
			<div class="docs-form-grid">
				<label>
					<span>项目名</span>
					<n-input v-model:value="createForm.targetProject" placeholder="例如：Project3" />
				</label>
				<label>
					<span>复制来源</span>
					<n-select v-model:value="createForm.sourceProject" :options="projectOptions" />
				</label>
			</div>
			<template #footer>
				<div class="docs-modal-actions">
					<n-button @click="showCreateModal = false">取消</n-button>
					<n-button type="primary" :loading="saving" @click="createProject">创建</n-button>
				</div>
			</template>
		</n-modal>

		<n-modal v-model:show="showSettingsModal" preset="card" title="项目设置" class="docs-modal" :mask-closable="false">
			<div v-if="settingsProject" class="docs-settings-grid">
				<p><strong>项目：</strong>{{ settingsProject }}</p>
				<p><strong>版本：</strong>{{ settingVersions.join(', ') }}</p>
			</div>
			<template #footer>
				<div class="docs-modal-actions">
					<n-button @click="settingsProject && visitProject(settingsProject)">访问</n-button>
					<n-button type="primary" @click="settingsProject && editProject(settingsProject)">编辑</n-button>
					<n-button
						v-if="settingsProject && settingsProject !== defaultProject"
						type="error"
						:loading="saving"
						@click="settingsProject && removeProject(settingsProject)"
					>
						删除项目
					</n-button>
				</div>
			</template>
		</n-modal>
	</AdminShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useApolloClient, useMutation, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import AdminShell from '../components/AdminShell.vue';

const defaultProject = 'default';
const defaultVersion = 'default';
const router = useRouter();
const { client } = useApolloClient();

const showCreateModal = ref(false);
const showSettingsModal = ref(false);
const settingsProject = ref<string | null>(null);
const saving = ref(false);
const message = ref('');

const createForm = reactive({
	targetProject: '',
	sourceProject: defaultProject
});

const { result: projectsResult, loading: projectsLoading, refetch: refetchProjects } = useQuery(gql`
	query DocumentProjects {
		documentProjects
	}
`);

const { result: versionsResult, refetch: refetchVersions } = useQuery(
	gql`
		query DocumentVersions($project: String!) {
			documentVersions(project: $project)
		}
	`,
	() => ({ project: settingsProject.value ?? defaultProject }),
	{ enabled: computed(() => !!settingsProject.value) }
);

const { mutate: createDocumentProjectMutation } = useMutation(gql`
	mutation CreateDocumentProject($sourceProject: String, $targetProject: String!) {
		createDocumentProject(sourceProject: $sourceProject, targetProject: $targetProject)
	}
`);

const { mutate: deleteDocumentProjectMutation } = useMutation(gql`
	mutation DeleteDocumentProject($project: String!) {
		deleteDocumentProject(project: $project)
	}
`);

const PROJECT_DOC_COUNT = gql`
	query ProjectDocCount($project: String!, $version: String!) {
		documents(includeHidden: true, project: $project, version: $version) {
			type
		}
	}
`;

const normalizeProject = (value?: string | null) => {
	const raw = (value ?? '').trim().toLowerCase();
	if (!raw) {
		return defaultProject;
	}
	const normalized = raw
		.replace(/[^a-z0-9._-]+/g, '-')
		.replace(/-+/g, '-')
		.replace(/^-+|-+$/g, '');
	return normalized || defaultProject;
};

const normalizeVersion = (value?: string | null) => {
	const raw = (value ?? '').trim().toLowerCase();
	if (!raw) {
		return defaultVersion;
	}
	const normalized = raw
		.replace(/[^a-z0-9._-]+/g, '-')
		.replace(/-+/g, '-')
		.replace(/^-+|-+$/g, '');
	return normalized || defaultVersion;
};

const projectList = computed(() => {
	const raw: string[] = projectsResult.value?.documentProjects ?? [];
	const set = new Set<string>([defaultProject]);
	raw.forEach(item => {
		const normalized = normalizeProject(item);
		if (normalized) {
			set.add(normalized);
		}
	});
	const values = Array.from(set).sort();
	return [defaultProject, ...values.filter(item => item !== defaultProject)];
});

const projectOptions = computed(() => projectList.value.map(item => ({
	label: item,
	value: item
})));

const settingVersions = computed(() => {
	const raw: string[] = versionsResult.value?.documentVersions ?? [];
	const set = new Set<string>([defaultVersion]);
	raw.forEach(item => {
		const normalized = normalizeVersion(item);
		if (normalized) {
			set.add(normalized);
		}
	});
	const values = Array.from(set).sort();
	return [defaultVersion, ...values.filter(item => item !== defaultVersion)];
});

const projectDocCounts = ref<Record<string, number>>({});

const loadProjectCounts = async (projects: string[]) => {
	if (!projects.length) {
		projectDocCounts.value = {};
		return;
	}
	const entries = await Promise.all(
		projects.map(async project => {
			const { data } = await client.query({
				query: PROJECT_DOC_COUNT,
				variables: { project, version: defaultVersion },
				fetchPolicy: 'no-cache'
			});
			const count = (data?.documents ?? []).filter((item: any) => item.type === 'DOC').length;
			return [project, count] as const;
		})
	);
	projectDocCounts.value = Object.fromEntries(entries);
};

watch(
	() => projectList.value,
	list => {
		void loadProjectCounts(list);
	},
	{ immediate: true }
);

const getDocCount = (project: string) => {
	return projectDocCounts.value[project] ?? 0;
};

const openCreateModal = () => {
	createForm.targetProject = '';
	createForm.sourceProject = defaultProject;
	message.value = '';
	showCreateModal.value = true;
};

const openSettings = async (project: string) => {
	settingsProject.value = project;
	message.value = '';
	showSettingsModal.value = true;
	await refetchVersions();
};

const visitProject = (project: string) => {
	const query: Record<string, string> = {};
	if (project !== defaultProject) {
		query.p = project;
	}
	void router.push({ path: '/docs', query });
};

const editProject = (project: string) => {
	const query: Record<string, string> = {};
	if (project !== defaultProject) {
		query.p = project;
	}
	void router.push({ path: '/admin/docs/edit', query });
};

const createProject = async () => {
	const target = normalizeProject(createForm.targetProject);
	if (!target) {
		message.value = '项目名不能为空';
		return;
	}
	if (projectList.value.includes(target)) {
		message.value = `项目已存在：${target}`;
		return;
	}
	saving.value = true;
	message.value = '';
	try {
		await createDocumentProjectMutation({
			sourceProject: normalizeProject(createForm.sourceProject),
			targetProject: target
		});
		await refetchProjects();
		await loadProjectCounts(projectList.value);
		showCreateModal.value = false;
		message.value = `项目创建成功：${target}`;
	} catch (error: any) {
		message.value = error?.message || '创建项目失败';
	} finally {
		saving.value = false;
	}
};

const removeProject = async (project: string) => {
	if (project === defaultProject) {
		message.value = 'default 项目不能删除';
		return;
	}
	if (!window.confirm(`确认删除项目 "${project}" 吗？`)) {
		return;
	}
	saving.value = true;
	message.value = '';
	try {
		await deleteDocumentProjectMutation({ project });
		await refetchProjects();
		await loadProjectCounts(projectList.value);
		showSettingsModal.value = false;
		settingsProject.value = null;
		message.value = `项目已删除：${project}`;
	} catch (error: any) {
		message.value = error?.message || '删除项目失败';
	} finally {
		saving.value = false;
	}
};

const refreshAll = async () => {
	message.value = '';
	await refetchProjects();
	await loadProjectCounts(projectList.value);
	if (showSettingsModal.value && settingsProject.value) {
		await refetchVersions();
	}
};

watch(
	() => projectsLoading.value,
	isLoading => {
		if (isLoading) {
			message.value = '';
		}
	}
);
</script>

<style scoped>
.docs-project-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
	gap: 14px;
}

.project-card {
	display: grid;
	gap: 8px;
	padding: 14px;
	min-height: 150px;
	align-content: start;
}

.project-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 8px;
}

.project-head h3 {
	margin: 0;
	font-size: 1.15rem;
}

.project-tag {
	font-size: 0.72rem;
	padding: 2px 8px;
	border-radius: 999px;
	background: rgba(34, 197, 94, 0.16);
	color: #14532d;
}

.project-meta,
.project-path {
	margin: 0;
	color: var(--muted);
}

.project-actions {
	display: flex;
	gap: 8px;
	margin-top: auto;
	padding-top: 8px;
}

.project-card-new {
	border: 1px dashed var(--line);
	background: transparent;
	border-radius: 12px;
	cursor: pointer;
	display: grid;
	place-items: center;
	gap: 8px;
	color: var(--muted);
}

.project-card-new:hover {
	background: var(--bg-strong);
}

.new-icon {
	width: 34px;
	height: 34px;
	display: grid;
	place-items: center;
	border-radius: 50%;
	border: 1px solid var(--line);
	font-size: 1.15rem;
	color: #4f46e5;
}

.docs-message {
	margin: 14px 0 0;
	color: #0f766e;
}

.docs-modal {
	width: min(560px, 92vw);
}

.docs-form-grid {
	display: grid;
	gap: 12px;
}

.docs-form-grid label {
	display: grid;
	gap: 6px;
}

.docs-settings-grid {
	display: grid;
	gap: 8px;
}

.docs-settings-grid p {
	margin: 0;
}

.docs-modal-actions {
	display: flex;
	justify-content: flex-end;
	gap: 8px;
}
</style>
