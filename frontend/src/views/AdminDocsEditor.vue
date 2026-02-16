<template>
	<AdminShell title="文档编辑器" subtitle="基于 commit/ref 的文档写入流程">
		<template #actions>
			<n-space>
				<n-select v-model:value="activeSpaceId" :options="spaceOptions" placeholder="选择空间" style="width: 220px" />
				<n-button secondary @click="openCreate('FOLDER')">新建文件夹</n-button>
				<n-button type="primary" @click="openCreate('DOC')">新建文档</n-button>
				<n-button @click="reloadAll">刷新</n-button>
			</n-space>
		</template>

		<section class="editor-layout">
			<aside class="card tree-panel">
				<h3>目录树</h3>
				<n-tree v-if="treeOptions.length" :data="treeOptions" :selected-keys="selectedKeys" selectable block-line default-expand-all expand-on-click :on-update:selected-keys="onSelectTree" />
				<p v-else class="muted">当前空间没有节点。</p>
			</aside>

			<section class="card detail-panel">
				<template v-if="selectedNode">
					<header class="detail-header">
						<div>
							<h3>{{ selectedNode.title }}</h3>
							<p class="muted">Node #{{ selectedNode.id }} · {{ selectedNode.nodeType }}</p>
						</div>
						<n-space>
							<n-button type="error" @click="removeNode">软删除</n-button>
						</n-space>
					</header>

					<!-- 创建节点表单 -->
					<n-form label-placement="top" class="meta-form">
						<n-form-item label="标题">
							<n-input v-model:value="nodeForm.title" />
						</n-form-item>
						<n-form-item label="父目录">
							<n-select v-model:value="nodeForm.parentId" :options="parentOptions" clearable placeholder="根目录" />
						</n-form-item>
						<n-form-item label="排序">
							<n-input-number v-model:value="nodeForm.sortKey" :min="0" />
						</n-form-item>
						<n-form-item label="是否删除">
							<n-switch v-model:value="nodeForm.deleted" />
						</n-form-item>
					</n-form>
					<n-space>
						<n-button type="primary" :loading="saving" @click="saveNodeMeta">保存节点信息</n-button>
						<n-button @click="resetNodeForm">重置</n-button>
					</n-space>

					<div v-if="selectedNode.nodeType === 'DOC' && selectedNode.docId" class="commit-panel">
						<n-divider />
						<h4>提交内容（commit）</h4>
						<n-space vertical :size="10">
							<n-space>
								<n-select v-model:value="selectedRef" :options="branchOptions" placeholder="分支" style="width: 260px" />
								<n-input v-model:value="newBranchName" placeholder="new-branch" style="width: 200px" />
								<n-button :disabled="!latestCommit" @click="createBranch">创建分支</n-button>
							</n-space>
							<n-space>
								<n-select v-model:value="mergeSourceRef" :options="mergeSourceOptions" placeholder="选择来源分支" style="width: 260px" />
								<n-button :disabled="!mergeSourceRef || !latestCommit" @click="mergeFromBranch">合并到当前分支</n-button>
							</n-space>
							<n-input v-model:value="commitForm.title" placeholder="提交标题" />
							<n-input v-model:value="commitForm.message" placeholder="提交说明" />
							<n-input v-model:value="commitForm.contentMd" type="textarea" :autosize="{ minRows: 14, maxRows: 24 }" placeholder="Markdown 内容" />
							<n-button type="primary" :loading="saving" @click="commitCurrent">提交到 {{ selectedRef || 'refs/heads/main' }}</n-button>
						</n-space>
					</div>
				</template>
				<p v-else class="muted">请选择一个节点开始编辑。</p>
			</section>
		</section>

		<n-modal v-model:show="showCreateModal" preset="card" title="创建节点" style="width: 520px">
			<n-form label-placement="top">
				<n-form-item label="类型">
					<n-select v-model:value="createForm.nodeType" :options="nodeTypeOptions" />
				</n-form-item>
				<n-form-item label="标题">
					<n-input v-model:value="createForm.title" />
				</n-form-item>
				<n-form-item label="父目录">
					<n-select v-model:value="createForm.parentId" :options="createParentOptions" clearable placeholder="根目录" />
				</n-form-item>
				<n-form-item label="排序">
					<n-input-number v-model:value="createForm.sortKey" :min="0" />
				</n-form-item>
				<n-form-item v-if="createForm.nodeType === 'DOC'" label="初始内容">
					<n-input v-model:value="createForm.contentMd" type="textarea" :autosize="{ minRows: 8, maxRows: 14 }" />
				</n-form-item>
			</n-form>
			<template #footer>
				<n-space justify="end">
					<n-button @click="showCreateModal = false">取消</n-button>
					<n-button type="primary" :loading="saving" @click="createNode">创建</n-button>
				</n-space>
			</template>
		</n-modal>
	</AdminShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMutation, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import type { TreeOption } from 'naive-ui';
import AdminShell from '../components/AdminShell.vue';

type Space = { id: string; name: string };
type NodeType = 'FOLDER' | 'DOC';
type DocNode = {
	id: string;
	parentId: string | null;
	title: string;
	nodeType: NodeType;
	sortKey: number;
	deleted: boolean;
	docId: string | null;
	children: DocNode[];
};
type DocRef = { refName: string; refType: 'BRANCH' | 'TAG'; commitId: string };
type DocCommit = { id: string; title: string; contentMd: string; message: string };

const route = useRoute();
const router = useRouter();
const mainRef = 'refs/heads/main';
const ROOT_PARENT_VALUE = '__ROOT__';

const { result: spacesResult, refetch: refetchSpaces } = useQuery(gql`
	query DocSpacesForEditor {
		docSpaces {
			id
			name
		}
	}
`);

const spaces = computed<Space[]>(() => spacesResult.value?.docSpaces ?? []);
const spaceOptions = computed(() => spaces.value.map(item => ({ label: item.name, value: item.id })));

const activeSpaceId = computed({
	get: () => {
		const queryValue = route.query.s ? String(route.query.s) : '';
		if (queryValue && spaces.value.some(item => item.id === queryValue)) {
			return queryValue;
		}
		return spaces.value[0]?.id ?? '';
	},
	set: (value: string) => {
		const nextQuery: Record<string, string> = {};
		if (value) nextQuery.s = value;
		void router.replace({ path: route.path, query: nextQuery });
	}
});

const selectedNodeId = computed({
	get: () => (route.query.n ? String(route.query.n) : ''),
	set: (value: string) => {
		const nextQuery: Record<string, string> = {};
		if (activeSpaceId.value) nextQuery.s = activeSpaceId.value;
		if (value) nextQuery.n = value;
		void router.replace({ path: route.path, query: nextQuery });
	}
});

const { result: treeResult, refetch: refetchTree } = useQuery(
	gql`
		query DocTreeForEditor($spaceId: ID!, $includeDeleted: Boolean!) {
			docTree(spaceId: $spaceId, includeDeleted: $includeDeleted) {
				id
				parentId
				title
				nodeType
				sortKey
				deleted
				docId
			}
		}
	`,
	() => ({
		spaceId: activeSpaceId.value,
		includeDeleted: true
	}),
	{ enabled: computed(() => !!activeSpaceId.value) }
);

const rawNodes = computed<DocNode[]>(() => treeResult.value?.docTree ?? []);

const buildTree = (nodes: DocNode[]) => {
	const map = new Map<string, DocNode>();
	nodes.forEach(node => {
		map.set(node.id, { ...node, children: [] });
	});
	const roots: DocNode[] = [];
	map.forEach(node => {
		if (node.parentId && map.has(node.parentId)) {
			map.get(node.parentId)?.children.push(node);
		} else {
			roots.push(node);
		}
	});
	const sort = (items: DocNode[]) => {
		items.sort((a, b) => {
			if (a.sortKey !== b.sortKey) return a.sortKey - b.sortKey;
			return a.id.localeCompare(b.id);
		});
		items.forEach(item => sort(item.children));
	};
	sort(roots);
	return roots;
};

const treeNodes = computed<DocNode[]>(() => buildTree(rawNodes.value));
const flatNodes = computed(() => rawNodes.value);
const selectedNode = computed(() => flatNodes.value.find(item => item.id === selectedNodeId.value) ?? null);
const selectedDocId = computed(() => selectedNode.value?.docId ?? '');

const treeOptions = computed<TreeOption[]>(() => {
	const convert = (nodes: DocNode[]): TreeOption[] => {
		return nodes.map(node => ({
			key: node.id,
			label: node.deleted ? `${node.title} (deleted)` : node.title,
			isLeaf: node.nodeType === 'DOC',
			children: node.children?.length ? convert(node.children) : []
		}));
	};
	return convert(treeNodes.value);
});

const selectedKeys = computed(() => (selectedNodeId.value ? [selectedNodeId.value] : []));

const onSelectTree = (keys: Array<string | number>) => {
	if (!keys.length) return;
	selectedNodeId.value = String(keys[0]);
};

const nodeForm = reactive({
	title: '',
	parentId: null as string | null,
	sortKey: 0,
	deleted: false
});

const resetNodeForm = () => {
	if (!selectedNode.value) return;
	nodeForm.title = selectedNode.value.title;
	nodeForm.parentId = selectedNode.value.parentId;
	nodeForm.sortKey = selectedNode.value.sortKey;
	nodeForm.deleted = selectedNode.value.deleted;
};

watch(
	() => selectedNode.value,
	() => {
		resetNodeForm();
	},
	{ immediate: true }
);

const parentOptions = computed(() => {
	return flatNodes.value.filter(item => item.nodeType === 'FOLDER' && item.id !== selectedNodeId.value).map(item => ({ label: item.title, value: item.id }));
});
const createParentOptions = computed(() => [{ label: '根目录', value: ROOT_PARENT_VALUE }, ...parentOptions.value]);

const showCreateModal = ref(false);
const createForm = reactive({
	nodeType: 'DOC' as NodeType,
	title: '',
	parentId: null as string | null,
	sortKey: 0,
	contentMd: ''
});

const nodeTypeOptions = [
	{ label: '文档', value: 'DOC' },
	{ label: '目录', value: 'FOLDER' }
];

const openCreate = (type: NodeType) => {
	createForm.nodeType = type;
	createForm.title = '';
	createForm.parentId = selectedNode.value?.nodeType === 'FOLDER' ? selectedNode.value.id : (selectedNode.value?.parentId ?? null);
	createForm.sortKey = 0;
	createForm.contentMd = '';
	showCreateModal.value = true;
};

const { mutate: createNodeMutation } = useMutation(gql`
	mutation CreateDocNode($input: DocNodeCreateInput!) {
		createDocNode(input: $input) {
			id
			docId
		}
	}
`);

const { mutate: updateNodeMutation } = useMutation(gql`
	mutation UpdateDocNode($id: ID!, $input: DocNodeUpdateInput!) {
		updateDocNode(id: $id, input: $input) {
			id
		}
	}
`);

const { mutate: moveNodeMutation } = useMutation(gql`
	mutation MoveDocNode($id: ID!, $input: DocNodeMoveInput!) {
		moveDocNode(id: $id, input: $input) {
			id
		}
	}
`);

const { mutate: deleteNodeMutation } = useMutation(gql`
	mutation DeleteDocNode($id: ID!) {
		deleteDocNode(id: $id)
	}
`);

const { mutate: createRefMutation } = useMutation(gql`
	mutation CreateDocRef($docId: ID!, $refName: String!, $fromCommitId: ID!) {
		createDocRef(docId: $docId, refName: $refName, fromCommitId: $fromCommitId) {
			refName
		}
	}
`);

const { mutate: commitDocMutation } = useMutation(gql`
	mutation CommitDoc($input: DocCommitInput!) {
		commitDoc(input: $input) {
			id
			title
			contentMd
			message
		}
	}
`);

const { mutate: mergeDocMutation } = useMutation(gql`
	mutation MergeDoc($input: DocMergeInput!) {
		mergeDoc(input: $input) {
			id
		}
	}
`);

const saving = ref(false);

const createNode = async () => {
	if (!activeSpaceId.value || !createForm.title.trim()) return;
	saving.value = true;
	try {
		const parentId = createForm.parentId === ROOT_PARENT_VALUE ? null : createForm.parentId;
		const result = await createNodeMutation({
			input: {
				spaceId: activeSpaceId.value,
				parentId,
				nodeType: createForm.nodeType,
				title: createForm.title,
				sortKey: Number(createForm.sortKey) || 0,
				contentMd: createForm.nodeType === 'DOC' ? createForm.contentMd : null
			}
		});
		const createdId = String(result?.data?.createDocNode?.id ?? '');
		await refetchTree();
		if (createdId) selectedNodeId.value = createdId;
		showCreateModal.value = false;
	} finally {
		saving.value = false;
	}
};

const saveNodeMeta = async () => {
	if (!selectedNode.value) return;
	saving.value = true;
	try {
		const parentChanged = (nodeForm.parentId ?? null) !== (selectedNode.value.parentId ?? null);
		const sortChanged = Number(nodeForm.sortKey) !== Number(selectedNode.value.sortKey);
		if (parentChanged || sortChanged) {
			await moveNodeMutation({
				id: selectedNode.value.id,
				input: {
					parentId: nodeForm.parentId,
					sortKey: Number(nodeForm.sortKey) || 0
				}
			});
		}
		await updateNodeMutation({
			id: selectedNode.value.id,
			input: {
				title: nodeForm.title,
				sortKey: Number(nodeForm.sortKey) || 0,
				deleted: nodeForm.deleted
			}
		});
		await refetchTree();
	} finally {
		saving.value = false;
	}
};

const removeNode = async () => {
	if (!selectedNode.value) return;
	if (!window.confirm('确认软删除这个节点及其子树吗？')) return;
	saving.value = true;
	try {
		await deleteNodeMutation({ id: selectedNode.value.id });
		selectedNodeId.value = '';
		await refetchTree();
	} finally {
		saving.value = false;
	}
};

const selectedRef = ref(mainRef);
const { result: refsResult, refetch: refetchRefs } = useQuery(
	gql`
		query DocRefsForEditor($docId: ID!) {
			docRefs(docId: $docId) {
				refName
				refType
				commitId
			}
		}
	`,
	() => ({ docId: selectedDocId.value }),
	{ enabled: computed(() => !!selectedDocId.value) }
);

const refs = computed<DocRef[]>(() => refsResult.value?.docRefs ?? []);
const branchRefs = computed(() => refs.value.filter(item => item.refType === 'BRANCH'));
const branchOptions = computed(() => branchRefs.value.map(item => ({ label: item.refName, value: item.refName })));

watch(
	() => branchRefs.value.map(item => item.refName),
	names => {
		if (!names.length) {
			selectedRef.value = mainRef;
			return;
		}
		if (!names.includes(selectedRef.value)) {
			selectedRef.value = names.includes(mainRef) ? mainRef : names[0];
		}
	},
	{ immediate: true }
);

const { result: commitResult, refetch: refetchCommit } = useQuery(
	gql`
		query DocLatestCommitForEditor($docId: ID!, $refName: String!) {
			docLatestCommit(docId: $docId, refName: $refName) {
				id
				title
				contentMd
				message
			}
		}
	`,
	() => ({
		docId: selectedDocId.value,
		refName: selectedRef.value || mainRef
	}),
	{ enabled: computed(() => !!selectedDocId.value && !!selectedRef.value) }
);

const latestCommit = computed<DocCommit | null>(() => commitResult.value?.docLatestCommit ?? null);

const commitForm = reactive({
	title: '',
	contentMd: '',
	message: ''
});

watch(
	() => latestCommit.value,
	commit => {
		if (!commit) {
			commitForm.title = selectedNode.value?.title ?? '';
			commitForm.contentMd = '';
			commitForm.message = '';
			return;
		}
		commitForm.title = commit.title;
		commitForm.contentMd = commit.contentMd;
		commitForm.message = '';
	},
	{ immediate: true }
);

const newBranchName = ref('');
const createBranch = async () => {
	if (!selectedDocId.value || !latestCommit.value || !newBranchName.value.trim()) return;
	const refName = `refs/heads/${newBranchName.value.trim()}`;
	saving.value = true;
	try {
		await createRefMutation({
			docId: selectedDocId.value,
			refName,
			fromCommitId: latestCommit.value.id
		});
		newBranchName.value = '';
		await refetchRefs();
	} finally {
		saving.value = false;
	}
};

const commitCurrent = async () => {
	if (!selectedDocId.value) return;
	saving.value = true;
	try {
		await commitDocMutation({
			input: {
				docId: selectedDocId.value,
				refName: selectedRef.value || mainRef,
				baseCommitId: latestCommit.value?.id ?? null,
				title: commitForm.title,
				contentMd: commitForm.contentMd,
				message: commitForm.message || 'Update document'
			}
		});
		await refetchCommit();
		await refetchTree();
	} finally {
		saving.value = false;
	}
};

const mergeSourceRef = ref('');
const mergeSourceOptions = computed(() => {
	return branchRefs.value.filter(item => item.refName !== selectedRef.value).map(item => ({ label: item.refName, value: item.refName }));
});

const mergeFromBranch = async () => {
	if (!selectedDocId.value || !mergeSourceRef.value || !latestCommit.value) return;
	saving.value = true;
	try {
		await mergeDocMutation({
			input: {
				docId: selectedDocId.value,
				targetRef: selectedRef.value || mainRef,
				sourceRef: mergeSourceRef.value,
				targetBaseCommitId: latestCommit.value.id,
				title: commitForm.title,
				contentMd: commitForm.contentMd,
				message: `Merge ${mergeSourceRef.value} into ${selectedRef.value || mainRef}`
			}
		});
		mergeSourceRef.value = '';
		await refetchCommit();
		await refetchRefs();
	} finally {
		saving.value = false;
	}
};

const reloadAll = async () => {
	await refetchSpaces();
	await refetchTree();
	if (selectedDocId.value) {
		await refetchRefs();
		await refetchCommit();
	}
};

watch(
	() => [spaces.value.length, activeSpaceId.value],
	() => {
		if (activeSpaceId.value || !spaces.value.length) return;
		activeSpaceId.value = spaces.value[0].id;
	},
	{ immediate: true }
);

watch(
	() => [flatNodes.value.length, selectedNodeId.value],
	() => {
		if (!flatNodes.value.length) return;
		if (selectedNodeId.value && flatNodes.value.some(item => item.id === selectedNodeId.value)) {
			return;
		}
		selectedNodeId.value = flatNodes.value[0].id;
	},
	{ immediate: true }
);
</script>

<style scoped>
.editor-layout {
	display: grid;
	grid-template-columns: 300px minmax(0, 1fr);
	gap: 18px;
	align-items: start;
}

.tree-panel {
	position: sticky;
	top: 96px;
	max-height: calc(100vh - 120px);
	overflow: auto;
	display: grid;
	gap: 12px;
}

.detail-panel {
	display: grid;
	gap: 14px;
}

.detail-header {
	display: flex;
	justify-content: space-between;
	gap: 12px;
	align-items: flex-start;
}

.detail-header h3 {
	margin: 0;
}

.meta-form {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 12px;
}

.commit-panel {
	display: grid;
	gap: 12px;
}

.muted {
	color: var(--muted);
}

@media (max-width: 1024px) {
	.editor-layout {
		grid-template-columns: 1fr;
	}

	.tree-panel {
		position: static;
		max-height: none;
	}

	.meta-form {
		grid-template-columns: 1fr;
	}
}
</style>
