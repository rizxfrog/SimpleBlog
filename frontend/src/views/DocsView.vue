<template>
  <div class="container docs-layout">
    <aside class="card docs-sidebar">
      <h3>Docs</h3>
      <n-select
        v-model:value="activeSpaceId"
        :options="spaceOptions"
        placeholder="Select space"
        size="small"
      />
      <n-input
        v-model:value="activeRef"
        size="small"
        placeholder="refs/heads/main"
      />
      <div v-if="treeLoading" class="docs-muted">Loading tree...</div>
      <div v-else-if="!treeOptions.length" class="docs-muted">No docs yet.</div>
      <n-tree
        v-else
        :data="treeOptions"
        :selected-keys="selectedKeys"
        selectable
        block-line
        expand-on-click
        default-expand-all
        :on-update:selected-keys="onSelect"
      />
    </aside>

    <section class="card docs-main">
      <div v-if="!selectedDocNode" class="docs-empty">Select a document node.</div>
      <template v-else>
        <header class="docs-main-header">
          <div>
            <p class="docs-muted">Node</p>
            <h2>{{ selectedDocNode.title }}</h2>
          </div>
          <div class="docs-commit-meta" v-if="latestCommit">
            <span>#{{ latestCommit.id }}</span>
            <span>{{ latestCommit.message }}</span>
          </div>
        </header>
        <div v-if="commitLoading" class="docs-muted">Loading commit...</div>
        <div v-else-if="!latestCommit" class="docs-empty">No commit found on current ref.</div>
        <div v-else class="markdown" v-html="html"></div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { marked } from 'marked';
import type { TreeOption } from 'naive-ui';

type DocSpace = {
  id: string;
  name: string;
};

type DocNode = {
  id: string;
  parentId: string | null;
  title: string;
  nodeType: 'FOLDER' | 'DOC';
  sortKey: number;
  docId: string | null;
  deleted: boolean;
  children: DocNode[];
};

type DocCommit = {
  id: string;
  message: string;
  title: string;
  contentMd: string;
};

const route = useRoute();
const router = useRouter();

const mainRef = 'refs/heads/main';

const activeRef = computed({
  get: () => (route.query.ref ? String(route.query.ref) : mainRef),
  set: (value: string) => {
    const query: Record<string, string> = {};
    if (activeSpaceId.value) {
      query.s = activeSpaceId.value;
    }
    if (value && value !== mainRef) {
      query.ref = value;
    }
    void router.replace({ path: route.path, query });
  }
});

const selectedNodeId = computed(() => {
  if (!route.params.id) return null;
  return String(route.params.id);
});

const { result: spacesResult } = useQuery(gql`
  query DocSpaces {
    docSpaces {
      id
      name
    }
  }
`);

const spaceList = computed<DocSpace[]>(() => spacesResult.value?.docSpaces ?? []);
const activeSpaceId = computed({
  get: () => {
    const queryValue = route.query.s ? String(route.query.s) : '';
    if (queryValue && spaceList.value.some(item => item.id === queryValue)) {
      return queryValue;
    }
    return spaceList.value[0]?.id ?? '';
  },
  set: (value: string) => {
    const query: Record<string, string> = {};
    if (value) {
      query.s = value;
    }
    if (activeRef.value !== mainRef) {
      query.ref = activeRef.value;
    }
    void router.push({ path: '/docs', query });
  }
});

const spaceOptions = computed(() => {
  return spaceList.value.map(item => ({
    label: item.name,
    value: item.id
  }));
});

const { result: treeResult, loading: treeLoading } = useQuery(
  gql`
    query DocTree($spaceId: ID!, $includeDeleted: Boolean!) {
      docTree(spaceId: $spaceId, includeDeleted: $includeDeleted) {
        id
        parentId
        title
        nodeType
        sortKey
        docId
        deleted
      }
    }
  `,
  () => ({
    spaceId: activeSpaceId.value,
    includeDeleted: false
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

const selectedDocNode = computed(() => {
  if (!selectedNodeId.value) return null;
  return flatNodes.value.find(item => item.id === selectedNodeId.value && item.nodeType === 'DOC') ?? null;
});

const selectedKeys = computed(() => (selectedNodeId.value ? [selectedNodeId.value] : []));

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

const onSelect = (keys: Array<string | number>) => {
  if (!keys.length) return;
  const id = String(keys[0]);
  const query: Record<string, string> = {};
  if (activeSpaceId.value) {
    query.s = activeSpaceId.value;
  }
  if (activeRef.value !== mainRef) {
    query.ref = activeRef.value;
  }
  void router.push({ path: `/docs/${id}`, query });
};

const { result: commitResult, loading: commitLoading } = useQuery(
  gql`
    query DocLatestCommit($docId: ID!, $refName: String!) {
      docLatestCommit(docId: $docId, refName: $refName) {
        id
        message
        title
        contentMd
      }
    }
  `,
  () => ({
    docId: selectedDocNode.value?.docId,
    refName: activeRef.value || mainRef
  }),
  { enabled: computed(() => !!selectedDocNode.value?.docId) }
);

const latestCommit = computed<DocCommit | null>(() => commitResult.value?.docLatestCommit ?? null);
const html = ref('');

watch(
  () => latestCommit.value?.contentMd,
  (value) => {
    html.value = value ? String(marked.parse(value)) : '';
  },
  { immediate: true }
);

watch(
  () => [spaceList.value.length, activeSpaceId.value, selectedNodeId.value, flatNodes.value.length],
  () => {
    if (!activeSpaceId.value || !flatNodes.value.length) return;
    if (selectedNodeId.value && flatNodes.value.some(item => item.id === selectedNodeId.value)) {
      return;
    }
    const firstDoc = flatNodes.value.find(item => item.nodeType === 'DOC') ?? flatNodes.value[0];
    if (!firstDoc) return;
    const query: Record<string, string> = { s: activeSpaceId.value };
    if (activeRef.value !== mainRef) {
      query.ref = activeRef.value;
    }
    void router.replace({ path: `/docs/${firstDoc.id}`, query });
  },
  { immediate: true }
);
</script>

<style scoped>
.docs-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
}

.docs-sidebar {
  display: grid;
  gap: 12px;
  align-content: start;
  position: sticky;
  top: 96px;
  max-height: calc(100vh - 120px);
  overflow: auto;
}

.docs-main {
  min-height: 420px;
  display: grid;
  gap: 14px;
}

.docs-main-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.docs-main-header h2 {
  margin: 0;
}

.docs-commit-meta {
  display: flex;
  gap: 10px;
  font-size: 0.85rem;
  color: var(--muted);
}

.docs-muted {
  color: var(--muted);
  margin: 0;
}

.docs-empty {
  color: var(--muted);
}

@media (max-width: 980px) {
  .docs-layout {
    grid-template-columns: 1fr;
  }

  .docs-sidebar {
    position: static;
    max-height: none;
  }
}
</style>
