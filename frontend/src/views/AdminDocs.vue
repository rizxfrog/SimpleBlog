<template>
  <AdminShell title="文档空间" subtitle="管理新的文档系统空间（doc_space）">
    <template #actions>
      <n-space>
        <n-button secondary @click="refetchSpaces">刷新</n-button>
        <n-button type="primary" @click="showCreate = true">新建空间</n-button>
      </n-space>
    </template>

    <section class="space-grid">
      <article v-for="space in spaces" :key="space.id" class="card space-card">
        <div class="space-head">
          <h3>{{ space.name }}</h3>
          <small>#{{ space.id }}</small>
        </div>
        <p class="space-meta">更新时间：{{ formatDate(space.updateAt) }}</p>
        <div class="space-actions">
          <n-button size="small" @click="visit(space.id)">访问</n-button>
          <n-button size="small" type="primary" @click="edit(space.id)">编辑</n-button>
          <n-button size="small" @click="rename(space)">重命名</n-button>
          <n-button size="small" type="error" @click="remove(space)">删除</n-button>
        </div>
      </article>
    </section>

    <n-modal v-model:show="showCreate" preset="card" title="新建空间" style="width: 420px">
      <n-input v-model:value="createName" placeholder="Space name" />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreate = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="createSpace">创建</n-button>
        </n-space>
      </template>
    </n-modal>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMutation, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import dayjs from 'dayjs';
import AdminShell from '../components/AdminShell.vue';

type DocSpace = {
  id: string;
  name: string;
  updateAt: string;
};

const router = useRouter();

const DOC_SPACES = gql`
  query DocSpacesAdmin {
    docSpaces {
      id
      name
      updateAt
    }
  }
`;

const { result: spacesResult, refetch: refetchSpaces } = useQuery(DOC_SPACES);
const spaces = computed<DocSpace[]>(() => spacesResult.value?.docSpaces ?? []);

const { mutate: createDocSpaceMutation } = useMutation(gql`
  mutation CreateDocSpace($name: String!) {
    createDocSpace(name: $name) {
      id
    }
  }
`);

const { mutate: renameDocSpaceMutation } = useMutation(gql`
  mutation RenameDocSpace($id: ID!, $name: String!) {
    renameDocSpace(id: $id, name: $name) {
      id
    }
  }
`);

const { mutate: deleteDocSpaceMutation } = useMutation(gql`
  mutation DeleteDocSpace($id: ID!) {
    deleteDocSpace(id: $id)
  }
`);

const showCreate = ref(false);
const createName = ref('');
const saving = ref(false);

const createSpace = async () => {
  if (!createName.value.trim()) return;
  saving.value = true;
  try {
    await createDocSpaceMutation({ name: createName.value.trim() });
    createName.value = '';
    showCreate.value = false;
    await refetchSpaces();
  } finally {
    saving.value = false;
  }
};

const rename = async (space: DocSpace) => {
  const next = window.prompt('新的空间名称', space.name)?.trim();
  if (!next || next === space.name) return;
  saving.value = true;
  try {
    await renameDocSpaceMutation({ id: space.id, name: next });
    await refetchSpaces();
  } finally {
    saving.value = false;
  }
};

const remove = async (space: DocSpace) => {
  if (!window.confirm(`确认删除空间 "${space.name}" ?`)) return;
  saving.value = true;
  try {
    await deleteDocSpaceMutation({ id: space.id });
    await refetchSpaces();
  } finally {
    saving.value = false;
  }
};

const visit = (spaceId: string) => {
  void router.push({ path: '/docs', query: { s: spaceId } });
};

const edit = (spaceId: string) => {
  void router.push({ path: '/admin/docs/edit', query: { s: spaceId } });
};

const formatDate = (value: string) => {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
};
</script>

<style scoped>
.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.space-card {
  display: grid;
  gap: 10px;
}

.space-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: baseline;
}

.space-head h3 {
  margin: 0;
}

.space-meta {
  margin: 0;
  color: var(--muted);
}

.space-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
