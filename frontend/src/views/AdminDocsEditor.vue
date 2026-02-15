<template>
	<AdminShell title="Documents" subtitle="Manage the document tree">
		<template #actions>
			<n-button type="default" class="btn btn-soft" @click="goProjectList">Projects</n-button>
			<n-select :value="activeProject" :options="projectOptions" size="small" class="docs-version-select" @update:value="onProjectChange" />
			<n-select :value="activeVersion" :options="versionOptions" size="small" class="docs-version-select" @update:value="onVersionChange" />
			<n-button type="default" class="btn btn-soft" @click="openProjectManager" :disabled="saving">Project Settings</n-button>
			<n-button type="default" class="btn btn-soft" @click="openVersionManager" :disabled="saving">Versions</n-button>
			<n-button type="default" class="btn btn-soft" @click="startEdit" :disabled="saving || !selectedId">Edit</n-button>
		</template>

		<section class="admin-docs">
			<aside class="card docs-tree">
				<div class="docs-tree-header">
					<h3>目录</h3>
					<span v-if="loading" class="docs-loading">Loading</span>
				</div>
				<div class="docs-tree-actions">
					<n-button type="primary" strong secondary round @click="startCreate('FOLDER')" :disabled="saving">新建文件夹</n-button>
					<n-button type="info" strong secondary round @click="startCreate('DOC')" :disabled="saving">新建文档</n-button>
					<n-button type="default" strong secondary round @click="openVersionManager" :disabled="saving">版本管理</n-button>
				</div>
				<div v-if="!flatNodes.length" class="docs-empty">No documents yet.</div>
				<n-tree v-else :data="treeOptions" draggable block-line expand-on-click selectable :selected-keys="selectedKeys" :default-expand-all="true" :on-update:selected-keys="onSelect" :on-drop="onTreeDrop" />
			</aside>

			<section class="card docs-editor">
				<div class="docs-editor-header">
					<div>
						<h3>{{ selectedNode || docResult?.document ? 'Document Details' : 'Select Document' }}</h3>
						<p v-if="selectedNode || docResult?.document" class="docs-muted">Selected: {{ selectedNode?.title ?? docResult?.document?.title }}</p>
					</div>
					<div class="docs-editor-actions" v-if="selectedNode">
						<n-button type="default" class="btn btn-soft" @click="startEdit" :disabled="saving">Edit</n-button>
						<n-button type="default" class="btn btn-soft" @click="moveUp" :disabled="saving || !canMoveUp">Move Up</n-button>
						<n-button type="default" class="btn btn-soft" @click="moveDown" :disabled="saving || !canMoveDown">Move Down</n-button>
						<n-button type="default" class="btn btn-soft" @click="toggleHidden" :disabled="saving">
							{{ selectedNode.hidden ? 'Show' : 'Hide' }}
						</n-button>
						<n-button type="error" class="btn" @click="deleteCurrent" :disabled="saving">Delete</n-button>
					</div>
				</div>

				<div v-if="selectedNode || docResult?.document" class="docs-summary">
					<p class="docs-muted">Project: {{ activeProject }}</p>
					<p class="docs-muted">Version: {{ activeVersion }}</p>
					<p class="docs-muted">Path: {{ selectedNode?.path ?? docResult?.document?.path }}</p>
					<p v-if="message" class="docs-message">{{ message }}</p>
				</div>
				<div v-else class="docs-empty">Select a document to edit.</div>

				<div v-if="selectedNode" class="docs-revisions">
					<div class="docs-revisions-header">
						<h4>Revisions</h4>
						<span v-if="revisionsLoading" class="docs-muted">Loading...</span>
					</div>
					<div v-if="!revisions.length" class="docs-empty">No revisions yet.</div>
					<ul v-else class="docs-revision-list">
						<li v-for="rev in revisions" :key="rev.id" class="docs-revision-item">
							<div>
								<strong>#{{ rev.revisionNumber }}</strong>
								<span class="docs-revision-time">{{ formatTime(rev.createdAt) }}</span>
							</div>
							<div class="docs-revision-actions">
								<n-button size="small" class="btn btn-soft" @click="selectRevision(rev.id)" :disabled="saving">View Diff</n-button>
								<n-button size="small" class="btn btn-soft" @click="restoreRevision(rev.id)" :disabled="saving">Restore</n-button>
							</div>
						</li>
					</ul>
				</div>
				<div v-if="selectedRevision" class="docs-diff">
					<div class="docs-diff-header">
						<h4>Diff vs Current (Revision #{{ selectedRevision.revisionNumber }})</h4>
						<n-button size="small" class="btn btn-soft" @click="clearRevision">Close</n-button>
					</div>
					<div class="docs-diff-grid">
						<div class="docs-diff-col">
							<div class="docs-diff-label">Revision</div>
							<pre class="docs-diff-body">
                <code>
                  <span
                    v-for="(line, idx) in diffLeft"
                    :key="`L-${idx}`"
                    :class="['diff-line', `diff-${line.type}`]"
                  >
                    {{ line.type === 'remove' ? '-' : ' ' }} {{ line.text }}
                  </span>
                </code>
              </pre>
						</div>
						<div class="docs-diff-col">
							<div class="docs-diff-label">Current</div>
							<pre class="docs-diff-body">
                <code>
                  <span
                    v-for="(line, idx) in diffRight"
                    :key="`R-${idx}`"
                    :class="['diff-line', `diff-${line.type}`]"
                  >
                    {{ line.type === 'add' ? '+' : ' ' }} {{ line.text }}
                  </span>
                </code>
              </pre>
						</div>
					</div>
				</div>
			</section>
		</section>

		<n-modal v-model:show="showEditorModal">
			<div class="card docs-modal">
				<div class="docs-modal-header">
					<h3>{{ isCreating ? 'Create Document' : 'Edit Document' }}</h3>
					<n-button type="default" class="btn btn-soft" @click="closeEditorModal">Close</n-button>
				</div>
				<n-form label-placement="top" v-if="isCreating || selectedId">
					<n-form-item label="Version">
						<n-input :value="activeVersion" disabled />
					</n-form-item>
					<n-form-item label="Type">
						<n-select v-model:value="form.type" :options="typeOptions" />
					</n-form-item>
					<n-form-item label="Title">
						<n-input v-model:value="form.title" placeholder="Document title" />
					</n-form-item>
					<n-form-item label="Slug">
						<n-input v-model:value="form.slug" placeholder="Optional slug" />
					</n-form-item>
					<n-form-item label="Parent Folder">
						<n-select v-model:value="form.parentId" :options="folderOptions" placeholder="Root" clearable />
					</n-form-item>
					<n-form-item label="Sort Order">
						<n-input-number v-model:value="form.sortOrder" :min="0" />
					</n-form-item>
					<n-form-item label="Hidden">
						<n-switch v-model:value="form.hidden">
							<template #checked>Hidden</template>
							<template #unchecked>Visible</template>
						</n-switch>
					</n-form-item>
					<n-form-item v-if="form.type === 'DOC'" label="Content">
						<n-input v-model:value="form.content" type="textarea" placeholder="Write your markdown content..." :autosize="{ minRows: 8, maxRows: 20 }" />
					</n-form-item>
				</n-form>
				<p v-if="message" class="docs-message">{{ message }}</p>
				<div class="docs-modal-actions">
					<n-button type="default" class="btn btn-soft" @click="closeEditorModal">Cancel</n-button>
					<n-button type="primary" class="btn btn-primary" @click="saveDocument" :disabled="saving">
						{{ isCreating ? 'Create' : 'Save' }}
					</n-button>
				</div>
			</div>
		</n-modal>

		<n-modal v-model:show="showVersionModal">
			<div class="card docs-modal docs-version-modal">
				<div class="docs-modal-header">
					<h3>Version Manager</h3>
					<n-button type="default" class="btn btn-soft" @click="showVersionModal = false">Close</n-button>
				</div>
				<n-form label-placement="top">
					<n-form-item label="New Version">
						<n-input v-model:value="versionForm.targetVersion" placeholder="v1, v2, release-2026" />
					</n-form-item>
					<n-form-item label="Copy Existing Tree">
						<n-switch v-model:value="versionForm.copyFromCurrent">
							<template #checked>From Current</template>
							<template #unchecked>From Default</template>
						</n-switch>
					</n-form-item>
				</n-form>
				<n-button type="primary" class="btn btn-primary" :disabled="saving" @click="createVersion">Create Version</n-button>
				<div class="docs-version-list">
					<div v-for="version in versionList" :key="version" class="docs-version-item">
						<strong>{{ version }}</strong>
						<div class="docs-version-item-actions">
							<n-button size="small" class="btn btn-soft" @click="switchToVersion(version)">Use</n-button>
							<n-button v-if="version !== defaultVersion" size="small" type="error" class="btn" :disabled="saving" @click="removeVersion(version)">Delete</n-button>
						</div>
					</div>
				</div>
			</div>
		</n-modal>

		<n-modal v-model:show="showProjectModal">
			<div class="card docs-modal docs-version-modal">
				<div class="docs-modal-header">
					<h3>Project Manager</h3>
					<n-button type="default" class="btn btn-soft" @click="showProjectModal = false">Close</n-button>
				</div>
				<n-form label-placement="top">
					<n-form-item label="New Project">
						<n-input v-model:value="projectForm.targetProject" placeholder="project1, project2" />
					</n-form-item>
					<n-form-item label="Copy Existing Tree">
						<n-switch v-model:value="projectForm.copyFromCurrent">
							<template #checked>From Current</template>
							<template #unchecked>From Default</template>
						</n-switch>
					</n-form-item>
				</n-form>
				<n-button type="primary" class="btn btn-primary" :disabled="saving" @click="createProject">Create Project</n-button>
				<div class="docs-version-list">
					<div v-for="project in projectList" :key="project" class="docs-version-item">
						<strong>{{ project }}</strong>
						<div class="docs-version-item-actions">
							<n-button size="small" class="btn btn-soft" @click="switchToProject(project)">Use</n-button>
							<n-button v-if="project !== defaultProject" size="small" type="error" class="btn" :disabled="saving" @click="removeProject(project)">Delete</n-button>
						</div>
					</div>
				</div>
			</div>
		</n-modal>
	</AdminShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useQuery, useMutation } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { useRoute, useRouter } from 'vue-router';
import AdminShell from '../components/AdminShell.vue';
import dayjs from 'dayjs';
import type { TreeOption } from 'naive-ui';

type DocumentNode = {
	id: number;
	title: string;
	parentId: number | null;
	type: 'FOLDER' | 'DOC';
	project?: string;
	version?: string;
	path: string;
	sortOrder: number;
	hidden: boolean;
	depth: number;
	children: DocumentNode[];
};

const defaultProject = 'default';
const defaultVersion = 'default';
const route = useRoute();
const router = useRouter();
const saving = ref(false);
const message = ref('');
const selectedId = ref<number | null>(null);
const isCreating = ref(false);
const selectedRevisionId = ref<number | null>(null);
const showEditorModal = ref(false);
const showVersionModal = ref(false);
const showProjectModal = ref(false);
const activeProject = ref(route.query.p ? String(route.query.p) : defaultProject);
const activeVersion = ref(route.query.v ? String(route.query.v) : defaultVersion);

const projectForm = reactive({
	targetProject: '',
	copyFromCurrent: true
});

const versionForm = reactive({
	targetVersion: '',
	copyFromCurrent: true
});

const { result: projectsResult, refetch: refetchProjects } = useQuery(gql`
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
	() => ({ project: activeProject.value })
);

const { result, loading, refetch } = useQuery(
	gql`
		query DocumentsAdmin($project: String!, $version: String!) {
			documents(includeHidden: true, project: $project, version: $version) {
				id
				title
				parentId
				type
				project
				version
				path
				sortOrder
				hidden
				depth
			}
		}
	`,
	() => ({ project: activeProject.value, version: activeVersion.value })
);

const { result: docResult, refetch: refetchDoc } = useQuery(
	gql`
		query DocumentAdmin($id: ID!, $includeHidden: Boolean, $project: String!, $version: String!) {
			document(id: $id, includeHidden: $includeHidden, project: $project, version: $version) {
				id
				title
				content
				type
				project
				version
				parentId
				sortOrder
				hidden
				path
			}
		}
	`,
	() => ({ id: selectedId.value, includeHidden: true, project: activeProject.value, version: activeVersion.value }),
	{ enabled: computed(() => !!selectedId.value) }
);

const {
	result: revisionsResult,
	loading: revisionsLoading,
	refetch: refetchRevisions
} = useQuery(
	gql`
		query DocumentRevisions($documentId: ID!) {
			documentRevisions(documentId: $documentId) {
				id
				revisionNumber
				createdAt
			}
		}
	`,
	() => ({ documentId: selectedId.value }),
	{ enabled: computed(() => !!selectedId.value) }
);

const { result: revisionDetailResult } = useQuery(
	gql`
		query DocumentRevision($id: ID!) {
			documentRevision(id: $id) {
				id
				title
				content
				revisionNumber
			}
		}
	`,
	() => ({ id: selectedRevisionId.value }),
	{ enabled: computed(() => !!selectedRevisionId.value) }
);

const { mutate: createDocument } = useMutation(gql`
	mutation CreateDocument($input: DocumentCreateInput!) {
		createDocument(input: $input) {
			id
		}
	}
`);

const { mutate: updateDocument } = useMutation(gql`
	mutation UpdateDocument($id: ID!, $input: DocumentUpdateInput!) {
		updateDocument(id: $id, input: $input) {
			id
		}
	}
`);

const { mutate: moveDocument } = useMutation(gql`
	mutation MoveDocument($id: ID!, $input: DocumentMoveInput!) {
		moveDocument(id: $id, input: $input) {
			id
		}
	}
`);

const { mutate: deleteDocument } = useMutation(gql`
	mutation DeleteDocument($id: ID!) {
		deleteDocument(id: $id)
	}
`);

const { mutate: toggleHiddenMutation } = useMutation(gql`
	mutation ToggleDocumentHidden($id: ID!, $hidden: Boolean!) {
		toggleDocumentHidden(id: $id, hidden: $hidden) {
			id
			hidden
		}
	}
`);

const { mutate: restoreRevisionMutation } = useMutation(gql`
	mutation RestoreDocumentRevision($revisionId: ID!) {
		restoreDocumentRevision(revisionId: $revisionId) {
			id
		}
	}
`);

const { mutate: createDocumentVersionMutation } = useMutation(gql`
	mutation CreateDocumentVersion($project: String!, $sourceVersion: String, $targetVersion: String!) {
		createDocumentVersion(project: $project, sourceVersion: $sourceVersion, targetVersion: $targetVersion)
	}
`);

const { mutate: deleteDocumentVersionMutation } = useMutation(gql`
	mutation DeleteDocumentVersion($project: String!, $version: String!) {
		deleteDocumentVersion(project: $project, version: $version)
	}
`);

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

const nodes = computed<DocumentNode[]>(() => result.value?.documents ?? []);
const tree = computed(() => buildTree(nodes.value));
const flatNodes = computed(() => flattenTree(tree.value));
const selectedNode = computed(() => flatNodes.value.find(node => node.id === selectedId.value) || null);
const revisions = computed(() => revisionsResult.value?.documentRevisions ?? []);
const selectedRevision = computed(() => revisionDetailResult.value?.documentRevision ?? null);
const diffLines = computed(() => buildDiffLines(selectedRevision.value?.content ?? '', docResult.value?.document?.content ?? ''));
const diffLeft = computed(() => buildColumnDiff(diffLines.value).left);
const diffRight = computed(() => buildColumnDiff(diffLines.value).right);
const selectedKeys = computed(() => (selectedId.value ? [selectedId.value] : []));
const treeOptions = computed<TreeOption[]>(() => buildTreeOptions(tree.value));
const projectList = computed(() => {
	const raw: string[] = projectsResult.value?.documentProjects ?? [];
	const set = new Set<string>([defaultProject]);
	raw.forEach(value => {
		const normalized = normalizeProject(value);
		if (normalized) {
			set.add(normalized);
		}
	});
	const values = Array.from(set).sort();
	return [defaultProject, ...values.filter(value => value !== defaultProject)];
});
const projectOptions = computed(() =>
	projectList.value.map(value => ({
		label: value,
		value
	}))
);
const versionList = computed(() => {
	const raw: string[] = versionsResult.value?.documentVersions ?? [];
	const set = new Set<string>([defaultVersion]);
	raw.forEach(value => {
		const normalized = normalizeVersion(value);
		if (normalized) {
			set.add(normalized);
		}
	});
	const values = Array.from(set).sort();
	return [defaultVersion, ...values.filter(value => value !== defaultVersion)];
});
const versionOptions = computed(() =>
	versionList.value.map(value => ({
		label: value,
		value
	}))
);

const form = reactive({
	title: '',
	slug: '',
	content: '',
	parentId: null as number | null,
	sortOrder: 0,
	hidden: false,
	type: 'DOC' as 'DOC' | 'FOLDER'
});

const typeOptions = [
	{ label: 'Document', value: 'DOC' },
	{ label: 'Folder', value: 'FOLDER' }
];

const folderOptions = computed(() => {
	const folders = flatNodes.value.filter(node => node.type === 'FOLDER');
	const options = folders.map(node => ({
		label: `${'  '.repeat(Math.max(node.depth - 1, 0))}${node.title}`,
		value: node.id
	}));
	const parentId = form.parentId;
	if (parentId != null && !options.some(option => option.value === parentId)) {
		const parentNode = flatNodes.value.find(node => node.id === parentId);
		const fallbackLabel = parentNode?.title ?? deriveParentName(docResult.value?.document?.path) ?? 'Root';
		options.unshift({
			label: fallbackLabel,
			value: parentId
		});
	}
	return options;
});

const siblings = computed(() => {
	if (!selectedNode.value) return [];
	const parentId = selectedNode.value.parentId ?? null;
	return flatNodes.value.filter(node => (node.parentId ?? null) === parentId);
});

const canMoveUp = computed(() => {
	if (!selectedNode.value) return false;
	const index = siblings.value.findIndex(node => node.id === selectedNode.value?.id);
	return index > 0;
});

const canMoveDown = computed(() => {
	if (!selectedNode.value) return false;
	const index = siblings.value.findIndex(node => node.id === selectedNode.value?.id);
	return index >= 0 && index < siblings.value.length - 1;
});

const goProjectList = () => {
	void router.push('/admin/docs');
};

const syncScopeToRoute = () => {
	const query: Record<string, string> = {};
	if (activeProject.value !== defaultProject) {
		query.p = activeProject.value;
	}
	if (activeVersion.value !== defaultVersion) {
		query.v = activeVersion.value;
	}
	void router.replace({ path: '/admin/docs/edit', query });
};

const onProjectChange = (value: string | number) => {
	switchToProject(String(value));
};

const switchToProject = (project: string) => {
	const next = normalizeProject(project);
	if (!next || next === activeProject.value) {
		return;
	}
	activeProject.value = next;
	activeVersion.value = defaultVersion;
	selectedId.value = null;
	selectedRevisionId.value = null;
	isCreating.value = false;
	showEditorModal.value = false;
	message.value = '';
	syncScopeToRoute();
};

const onVersionChange = (value: string | number) => {
	switchToVersion(String(value));
};

const switchToVersion = (version: string) => {
	const next = normalizeVersion(version);
	if (!next || next === activeVersion.value) {
		return;
	}
	activeVersion.value = next;
	selectedId.value = null;
	selectedRevisionId.value = null;
	isCreating.value = false;
	showEditorModal.value = false;
	message.value = '';
	syncScopeToRoute();
};

const openProjectManager = () => {
	projectForm.targetProject = '';
	projectForm.copyFromCurrent = true;
	showProjectModal.value = true;
	message.value = '';
};

const createProject = async () => {
	const target = normalizeProject(projectForm.targetProject);
	if (!target) {
		message.value = 'Project name is required.';
		return;
	}
	if (projectList.value.includes(target)) {
		message.value = `Project already exists: ${target}`;
		return;
	}
	saving.value = true;
	message.value = '';
	try {
		await createDocumentProjectMutation({
			sourceProject: projectForm.copyFromCurrent ? activeProject.value : defaultProject,
			targetProject: target
		});
		await refetchProjects();
		switchToProject(target);
		await refetchVersions();
		await refetch();
		showProjectModal.value = false;
		message.value = `Project created: ${target}`;
	} catch (error: any) {
		message.value = error?.message || 'Create project failed';
	} finally {
		saving.value = false;
	}
};

const removeProject = async (project: string) => {
	if (project === defaultProject) {
		message.value = 'Default project cannot be deleted.';
		return;
	}
	if (!window.confirm(`Delete project "${project}"?`)) {
		return;
	}
	saving.value = true;
	message.value = '';
	try {
		await deleteDocumentProjectMutation({ project });
		await refetchProjects();
		if (activeProject.value === project) {
			switchToProject(defaultProject);
		}
		await refetchVersions();
		await refetch();
		message.value = `Project deleted: ${project}`;
	} catch (error: any) {
		message.value = error?.message || 'Delete project failed';
	} finally {
		saving.value = false;
	}
};

const openVersionManager = () => {
	versionForm.targetVersion = '';
	versionForm.copyFromCurrent = true;
	showVersionModal.value = true;
	message.value = '';
};

const createVersion = async () => {
	const target = normalizeVersion(versionForm.targetVersion);
	if (!target) {
		message.value = 'Version name is required.';
		return;
	}
	if (versionList.value.includes(target)) {
		message.value = `Version already exists: ${target}`;
		return;
	}
	saving.value = true;
	message.value = '';
	try {
		await createDocumentVersionMutation({
			project: activeProject.value,
			sourceVersion: versionForm.copyFromCurrent ? activeVersion.value : defaultVersion,
			targetVersion: target
		});
		await refetchVersions();
		switchToVersion(target);
		await refetch();
		showVersionModal.value = false;
		message.value = `Version created: ${target}`;
	} catch (error: any) {
		message.value = error?.message || 'Create version failed';
	} finally {
		saving.value = false;
	}
};

const removeVersion = async (version: string) => {
	if (version === defaultVersion) {
		message.value = 'Default version cannot be deleted.';
		return;
	}
	if (!window.confirm(`Delete version "${version}"?`)) {
		return;
	}
	saving.value = true;
	message.value = '';
	try {
		await deleteDocumentVersionMutation({ project: activeProject.value, version });
		await refetchVersions();
		if (activeVersion.value === version) {
			switchToVersion(defaultVersion);
		}
		await refetch();
		message.value = `Version deleted: ${version}`;
	} catch (error: any) {
		message.value = error?.message || 'Delete version failed';
	} finally {
		saving.value = false;
	}
};

const selectNode = (node: DocumentNode) => {
	selectedId.value = node.id;
	isCreating.value = false;
	message.value = '';
};

const startCreate = (type: 'DOC' | 'FOLDER') => {
	isCreating.value = true;
	message.value = '';
	const selected = selectedNode.value ?? (docResult.value?.document as DocumentNode | undefined);
	const parentId = selected ? (selected.type === 'FOLDER' ? selected.id : (selected.parentId ?? null)) : null;
	form.title = '';
	form.slug = '';
	form.content = '';
	form.parentId = parentId != null ? Number(parentId) : null;
	form.sortOrder = 0;
	form.hidden = false;
	form.type = type;
	showEditorModal.value = true;
};

const startEdit = async () => {
	if (!selectedId.value) return;
	isCreating.value = false;
	message.value = '';
	await refetchDoc();
	loadForm();
	showEditorModal.value = true;
};

const closeEditorModal = () => {
	showEditorModal.value = false;
};

const loadForm = () => {
	const doc = docResult.value?.document;
	if (!doc) return;
	form.title = doc.title;
	form.slug = deriveSlug(doc.path);
	form.content = doc.content ?? '';
	form.parentId = doc.parentId != null ? Number(doc.parentId) : null;
	form.sortOrder = doc.sortOrder ?? 0;
	form.hidden = Boolean(doc.hidden);
	form.type = doc.type;
};

const onSelect = (keys: Array<string | number>) => {
	const next = keys.length ? Number(keys[0]) : null;
	if (next) {
		const node = flatNodes.value.find(item => item.id === next);
		if (node) {
			selectNode(node);
			return;
		}
		selectedId.value = next;
		isCreating.value = false;
		message.value = '';
	}
};

const onTreeDrop = async (info: { node: TreeOption; dragNode: TreeOption; dropPosition: 'before' | 'inside' | 'after' | number }) => {
	const targetId = Number(info.node.key);
	const draggedId = Number(info.dragNode.key);
	if (!targetId || !draggedId || targetId === draggedId) return;
	const target = flatNodes.value.find(node => node.id === targetId);
	const dragged = flatNodes.value.find(node => node.id === draggedId);
	if (!target || !dragged) return;

	const dropPosition = typeof info.dropPosition === 'number' ? (info.dropPosition < 0 ? 'before' : info.dropPosition > 0 ? 'after' : 'inside') : info.dropPosition;

	if (dropPosition === 'inside') {
		if (target.type === 'FOLDER' && dragged.parentId !== target.id) {
			await moveToParent(dragged, target.id);
		}
		return;
	}

	const placeAfter = dropPosition === 'after';
	const sameParent = (dragged.parentId ?? null) === (target.parentId ?? null);
	if (sameParent) {
		await reorderWithinParent(dragged, target, placeAfter);
	} else {
		await moveToParentAt(dragged, target.parentId ?? null, target, placeAfter);
	}
};

const selectRevision = (revisionId: number) => {
	selectedRevisionId.value = revisionId;
};

const clearRevision = () => {
	selectedRevisionId.value = null;
};

const moveToParent = async (node: DocumentNode, parentId: number | null) => {
	const siblingOrders = flatNodes.value.filter(item => (item.parentId ?? null) === (parentId ?? null)).map(item => item.sortOrder);
	const nextOrder = siblingOrders.length ? Math.max(...siblingOrders) + 1 : 0;
	saving.value = true;
	message.value = '';
	try {
		await moveDocument({
			id: node.id,
			input: {
				parentId,
				sortOrder: nextOrder,
				slug: null
			}
		});
		await refetch();
	} catch (error: any) {
		message.value = error?.message || 'Move failed';
	} finally {
		saving.value = false;
	}
};

const moveToParentAt = async (node: DocumentNode, parentId: number | null, target: DocumentNode, placeAfter: boolean) => {
	const siblings = flatNodes.value.filter(item => (item.parentId ?? null) === (parentId ?? null) && item.id !== node.id);
	const targetIndex = siblings.findIndex(item => item.id === target.id);
	const insertAt = targetIndex >= 0 ? targetIndex + (placeAfter ? 1 : 0) : siblings.length;
	siblings.splice(insertAt, 0, { ...node, parentId });
	saving.value = true;
	message.value = '';
	try {
		await moveDocument({
			id: node.id,
			input: {
				parentId,
				sortOrder: insertAt,
				slug: null
			}
		});
		await reorderSiblings(siblings);
		await refetch();
	} catch (error: any) {
		message.value = error?.message || 'Move failed';
	} finally {
		saving.value = false;
	}
};

const saveDocument = async () => {
	if (!form.title.trim()) {
		message.value = 'Title is required';
		return;
	}
	message.value = '';
	saving.value = true;
	try {
		if (isCreating.value) {
			const createResult = await createDocument({
				input: {
					title: form.title,
					content: form.type === 'DOC' ? form.content : null,
					parentId: form.parentId,
					sortOrder: Number(form.sortOrder) || 0,
					hidden: form.hidden,
					slug: form.slug || null,
					project: activeProject.value,
					version: activeVersion.value,
					type: form.type
				}
			});
			const createdId = Number(createResult?.data?.createDocument?.id ?? 0);
			if (createdId) {
				selectedId.value = createdId;
			}
			message.value = 'Created';
			isCreating.value = false;
		} else if (selectedId.value && selectedNode.value) {
			const current = docResult.value?.document;
			const parentChanged = (form.parentId ?? null) !== (selectedNode.value.parentId ?? null);
			const slugChanged = (form.slug || '') !== deriveSlug(selectedNode.value.path);
			if (parentChanged || slugChanged) {
				await moveDocument({
					id: selectedId.value,
					input: {
						parentId: form.parentId,
						sortOrder: Number(form.sortOrder) || 0,
						slug: form.slug || null
					}
				});
			}
			const needsUpdate = !current || form.title !== current.title || (form.type === 'DOC' ? form.content : null) !== (current.content ?? null) || Number(form.sortOrder) !== Number(current.sortOrder ?? 0) || Boolean(form.hidden) !== Boolean(current.hidden) || form.type !== current.type;
			if (needsUpdate) {
				await updateDocument({
					id: selectedId.value,
					input: {
						title: form.title,
						content: form.type === 'DOC' ? form.content : null,
						sortOrder: Number(form.sortOrder) || 0,
						hidden: form.hidden,
						slug: slugChanged ? form.slug || null : null,
						type: form.type
					}
				});
			}
			message.value = parentChanged || slugChanged || needsUpdate ? 'Saved' : 'No changes';
		}
		await refetch();
		if (selectedId.value) {
			await refetchDoc();
		}
		await refetchRevisions();
		showEditorModal.value = false;
	} catch (error: any) {
		message.value = error?.message || 'Failed';
	} finally {
		saving.value = false;
	}
};

const deleteCurrent = async () => {
	if (!selectedId.value) return;
	saving.value = true;
	message.value = '';
	try {
		await deleteDocument({ id: selectedId.value });
		selectedId.value = null;
		selectedRevisionId.value = null;
		isCreating.value = false;
		showEditorModal.value = false;
		message.value = 'Deleted';
		await refetch();
		await refetchRevisions();
	} catch (error: any) {
		message.value = error?.message || 'Delete failed';
	} finally {
		saving.value = false;
	}
};

const toggleHidden = async () => {
	if (!selectedNode.value) return;
	saving.value = true;
	message.value = '';
	try {
		await toggleHiddenMutation({
			id: selectedNode.value.id,
			hidden: !selectedNode.value.hidden
		});
		await refetch();
		await refetchRevisions();
	} catch (error: any) {
		message.value = error?.message || 'Update failed';
	} finally {
		saving.value = false;
	}
};

const moveUp = async () => {
	if (!selectedNode.value || !canMoveUp.value) return;
	const index = siblings.value.findIndex(node => node.id === selectedNode.value?.id);
	const prev = siblings.value[index - 1];
	await swapSortOrder(selectedNode.value, prev);
};

const moveDown = async () => {
	if (!selectedNode.value || !canMoveDown.value) return;
	const index = siblings.value.findIndex(node => node.id === selectedNode.value?.id);
	const next = siblings.value[index + 1];
	await swapSortOrder(selectedNode.value, next);
};

const swapSortOrder = async (a: DocumentNode, b: DocumentNode) => {
	saving.value = true;
	message.value = '';
	try {
		await updateDocument({
			id: a.id,
			input: { sortOrder: b.sortOrder }
		});
		await updateDocument({
			id: b.id,
			input: { sortOrder: a.sortOrder }
		});
		await refetch();
	} catch (error: any) {
		message.value = error?.message || 'Reorder failed';
	} finally {
		saving.value = false;
	}
};

const reorderWithinParent = async (dragged: DocumentNode, target: DocumentNode, placeAfter: boolean) => {
	const siblings = flatNodes.value.filter(item => (item.parentId ?? null) === (target.parentId ?? null));
	const ordered = siblings.filter(item => item.id !== dragged.id);
	const targetIndex = ordered.findIndex(item => item.id === target.id);
	const insertAt = targetIndex >= 0 ? targetIndex + (placeAfter ? 1 : 0) : ordered.length;
	ordered.splice(insertAt, 0, dragged);
	saving.value = true;
	message.value = '';
	try {
		await reorderSiblings(ordered);
		await refetch();
	} catch (error: any) {
		message.value = error?.message || 'Reorder failed';
	} finally {
		saving.value = false;
	}
};

const reorderSiblings = async (ordered: DocumentNode[]) => {
	for (let i = 0; i < ordered.length; i += 1) {
		const node = ordered[i];
		await updateDocument({
			id: node.id,
			input: {
				sortOrder: i
			}
		});
	}
};

const shouldPlaceAfter = (event: DragEvent) => {
	const element = event.currentTarget as HTMLElement | null;
	if (!element) return true;
	const rect = element.getBoundingClientRect();
	return event.clientY > rect.top + rect.height / 2;
};

const restoreRevision = async (revisionId: number) => {
	saving.value = true;
	message.value = '';
	try {
		await restoreRevisionMutation({ revisionId });
		await refetch();
		await refetchDoc();
		await refetchRevisions();
		message.value = 'Restored';
	} catch (error: any) {
		message.value = error?.message || 'Restore failed';
	} finally {
		saving.value = false;
	}
};

const formatTime = (value: string) => {
	return value ? dayjs(value).format('YYYY/MM/DD HH:mm') : '';
};

const buildDiffLines = (oldText: string, newText: string) => {
	const oldLines = (oldText || '').split('\n');
	const newLines = (newText || '').split('\n');
	const m = oldLines.length;
	const n = newLines.length;
	const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));
	for (let i = 1; i <= m; i += 1) {
		for (let j = 1; j <= n; j += 1) {
			if (oldLines[i - 1] === newLines[j - 1]) {
				dp[i][j] = dp[i - 1][j - 1] + 1;
			} else {
				dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
			}
		}
	}
	const result: Array<{ type: 'context' | 'add' | 'remove'; text: string }> = [];
	let i = m;
	let j = n;
	while (i > 0 && j > 0) {
		if (oldLines[i - 1] === newLines[j - 1]) {
			result.unshift({ type: 'context', text: oldLines[i - 1] });
			i -= 1;
			j -= 1;
		} else if (dp[i - 1][j] >= dp[i][j - 1]) {
			result.unshift({ type: 'remove', text: oldLines[i - 1] });
			i -= 1;
		} else {
			result.unshift({ type: 'add', text: newLines[j - 1] });
			j -= 1;
		}
	}
	while (i > 0) {
		result.unshift({ type: 'remove', text: oldLines[i - 1] });
		i -= 1;
	}
	while (j > 0) {
		result.unshift({ type: 'add', text: newLines[j - 1] });
		j -= 1;
	}
	return result;
};

const buildColumnDiff = (lines: Array<{ type: 'context' | 'add' | 'remove'; text: string }>) => {
	const left: Array<{ type: 'context' | 'add' | 'remove'; text: string }> = [];
	const right: Array<{ type: 'context' | 'add' | 'remove'; text: string }> = [];
	lines.forEach(line => {
		if (line.type === 'context') {
			left.push(line);
			right.push(line);
		} else if (line.type === 'remove') {
			left.push(line);
			right.push({ type: 'context', text: '' });
		} else if (line.type === 'add') {
			left.push({ type: 'context', text: '' });
			right.push(line);
		}
	});
	return { left, right };
};

const buildTree = (items: DocumentNode[]) => {
	const map = new Map<number, DocumentNode>();
	items.forEach(item => {
		map.set(item.id, { ...item, children: [] });
	});
	const roots: DocumentNode[] = [];
	map.forEach(item => {
		if (item.parentId && map.has(item.parentId)) {
			map.get(item.parentId)!.children.push(item);
		} else {
			roots.push(item);
		}
	});
	const sortNodes = (nodes: DocumentNode[]) => {
		nodes.sort((a, b) => {
			if (a.sortOrder !== b.sortOrder) return a.sortOrder - b.sortOrder;
			return a.title.localeCompare(b.title);
		});
		nodes.forEach(node => sortNodes(node.children));
	};
	sortNodes(roots);
	return roots;
};

const flattenTree = (nodes: DocumentNode[]) => {
	const result: DocumentNode[] = [];
	const walk = (items: DocumentNode[]) => {
		items.forEach(item => {
			result.push(item);
			if (item.children?.length) {
				walk(item.children);
			}
		});
	};
	walk(nodes);
	return result;
};

const buildTreeOptions = (nodes: DocumentNode[]): TreeOption[] => {
	return nodes.map(node => ({
		key: node.id,
		label: node.title,
		children: node.children?.length ? buildTreeOptions(node.children) : [],
		isLeaf: node.type === 'DOC'
	}));
};

const deriveSlug = (path: string) => {
	if (!path) return '';
	const parts = path.split('.');
	return parts[parts.length - 1] ?? '';
};

const deriveParentName = (path?: string | null) => {
	if (!path) return null;
	const parts = path.split('.');
	if (parts.length < 2) return null;
	return parts[parts.length - 2] ?? null;
};

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

watch(
	() => [route.query.p, route.query.v],
	([projectQuery, versionQuery]) => {
		const nextProject = normalizeProject(projectQuery ? String(projectQuery) : defaultProject);
		const nextVersion = normalizeVersion(versionQuery ? String(versionQuery) : defaultVersion);
		const changed = nextProject !== activeProject.value || nextVersion !== activeVersion.value;
		if (!changed) {
			return;
		}
		activeProject.value = nextProject;
		activeVersion.value = nextVersion;
		selectedId.value = null;
		selectedRevisionId.value = null;
		isCreating.value = false;
		showEditorModal.value = false;
		message.value = '';
	},
	{ immediate: true }
);

watch(
	() => projectList.value,
	list => {
		if (!list.includes(activeProject.value)) {
			activeProject.value = defaultProject;
			activeVersion.value = defaultVersion;
			selectedId.value = null;
			selectedRevisionId.value = null;
			isCreating.value = false;
			showEditorModal.value = false;
			syncScopeToRoute();
		}
	},
	{ immediate: true }
);

watch(
	() => versionList.value,
	list => {
		if (!list.includes(activeVersion.value)) {
			activeVersion.value = defaultVersion;
			selectedId.value = null;
			selectedRevisionId.value = null;
			isCreating.value = false;
			showEditorModal.value = false;
			syncScopeToRoute();
		}
	},
	{ immediate: true }
);

watch(() => docResult.value?.document, loadForm);
watch(
	() => selectedId.value,
	() => {
		selectedRevisionId.value = null;
	}
);
</script>

<style scoped>
.admin-docs {
	display: grid;
	grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
	gap: 24px;
	align-items: start;
}

.docs-tree {
	max-height: calc(100vh - 220px);
	overflow: auto;
	padding: 18px;
}

.docs-tree-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.docs-version-select {
	min-width: 180px;
}

.docs-tree-actions {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
	gap: 8px;
	margin-bottom: 12px;
}

.docs-tree-actions :deep(.n-button) {
	white-space: nowrap;
}

/* Tree styles are handled by Naive UI */

.docs-editor {
	display: grid;
	gap: 16px;
	padding: 24px;
}

.docs-editor-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
}

.docs-editor-actions {
	display: flex;
	gap: 10px;
	flex-wrap: wrap;
}

.docs-summary {
	display: grid;
	gap: 6px;
}

.docs-revisions {
	margin-top: 18px;
	padding-top: 12px;
	border-top: 1px dashed var(--line);
	display: grid;
	gap: 12px;
}

.docs-revisions-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.docs-revision-list {
	list-style: none;
	padding: 0;
	margin: 0;
	display: grid;
	gap: 8px;
}

.docs-revision-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	padding: 8px 10px;
	border-radius: 10px;
	border: 1px solid var(--line);
	background: var(--bg-strong);
}

.docs-revision-actions {
	display: flex;
	gap: 8px;
}

.docs-revision-time {
	margin-left: 8px;
	color: var(--muted);
	font-size: 0.85rem;
}

.docs-diff {
	border: 1px solid var(--line);
	border-radius: 12px;
	padding: 12px;
	background: var(--panel);
}

.docs-diff-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
}

.docs-diff-body {
	margin: 12px 0 0;
	max-height: 320px;
	overflow: auto;
	background: var(--bg-strong);
	border-radius: 12px;
	padding: 12px;
	border: 1px solid var(--line);
}

.docs-diff-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 12px;
}

.docs-diff-col {
	display: grid;
	gap: 6px;
}

.docs-diff-label {
	font-size: 0.85rem;
	color: var(--muted);
}

.diff-line {
	display: block;
	white-space: pre-wrap;
	font-family: var(--font-mono);
	font-size: 0.85rem;
}

.diff-add {
	color: #1b7f4b;
	background: rgba(27, 127, 75, 0.12);
}

.diff-remove {
	color: #b42318;
	background: rgba(180, 35, 24, 0.12);
}

.docs-modal {
	width: min(760px, 94vw);
	max-height: 90vh;
	overflow: auto;
	margin: 5vh auto;
	padding: 18px;
	display: grid;
	gap: 14px;
}

.docs-modal-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
}

.docs-modal-actions {
	display: flex;
	justify-content: flex-end;
	gap: 8px;
}

.docs-version-modal {
	width: min(620px, 94vw);
}

.docs-version-list {
	display: grid;
	gap: 8px;
}

.docs-version-item {
	border: 1px solid var(--line);
	border-radius: 10px;
	background: var(--bg-strong);
	padding: 10px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
}

.docs-version-item-actions {
	display: flex;
	gap: 8px;
}

.docs-muted {
	color: var(--muted);
	margin: 6px 0 0;
}

.docs-message {
	color: #2c7a7b;
	margin: 0;
}

.docs-empty {
	color: var(--muted);
}

@media (max-width: 980px) {
	.admin-docs {
		grid-template-columns: 1fr;
	}
}
</style>
