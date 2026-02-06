import { computed, h, ref } from 'vue';
import { useMutation, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { NTag, NButton } from 'naive-ui';
import AdminShell from '../components/AdminShell.vue';
const blogIdInput = ref('');
const keyword = ref('');
const selectedIds = ref([]);
const statusFilter = ref(null);
const { result, refetch } = useQuery(gql `
    query AdminComments($blogId: ID, $status: CommentStatus, $keyword: String) {
      adminComments(blogId: $blogId, status: $status, keyword: $keyword) {
        id
        blogId
        content
        status
        upvotes
        downvotes
        authorName
        createdAt
        user {
          username
          displayName
        }
      }
    }
  `, () => ({
    blogId: blogIdInput.value ? Number(blogIdInput.value) : null,
    status: statusFilter.value,
    keyword: keyword.value || null
}));
const { mutate: updateStatus } = useMutation(gql `
    mutation UpdateCommentStatus($id: ID!, $status: CommentStatus!) {
      updateCommentStatus(id: $id, status: $status) {
        id
        status
      }
    }
  `);
const { mutate: batchUpdateStatus } = useMutation(gql `
    mutation BatchUpdateCommentStatus($ids: [ID!]!, $status: CommentStatus!) {
      batchUpdateCommentStatus(ids: $ids, status: $status)
    }
  `);
const { mutate: deleteComment } = useMutation(gql `
    mutation DeleteComment($id: ID!) {
      deleteComment(id: $id)
    }
  `);
const setStatus = (value) => {
    statusFilter.value = value;
    refetch();
};
const approve = async (id) => {
    await updateStatus({ id, status: 'approved' });
    await refetch();
};
const reject = async (id) => {
    await updateStatus({ id, status: 'rejected' });
    await refetch();
};
const bulkApprove = async () => {
    if (!selectedIds.value.length)
        return;
    await batchUpdateStatus({ ids: selectedIds.value, status: 'approved' });
    await refetch();
};
const bulkReject = async () => {
    if (!selectedIds.value.length)
        return;
    await batchUpdateStatus({ ids: selectedIds.value, status: 'rejected' });
    await refetch();
};
const bulkDelete = async () => {
    if (!selectedIds.value.length)
        return;
    await Promise.all(selectedIds.value.map((id) => deleteComment({ id })));
    await refetch();
};
const onSelectionChange = (keys) => {
    selectedIds.value = keys.map((key) => Number(key));
};
const columns = [
    { type: 'selection', width: 48 },
    { title: 'ID', key: 'id', width: 80 },
    { title: '博客', key: 'blogId', width: 90 },
    {
        title: '作者',
        key: 'author',
        width: 140,
        render: (row) => row.user?.displayName || row.user?.username || row.authorName || '匿名用户'
    },
    { title: '内容', key: 'content' },
    {
        title: '状态',
        key: 'status',
        width: 120,
        render: (row) => h(NTag, { type: row.status === 'approved' ? 'success' : row.status === 'rejected' ? 'error' : 'warning' }, { default: () => row.status })
    },
    {
        title: '票数',
        key: 'votes',
        width: 120,
        render: (row) => `👍 ${row.upvotes || 0} / 👎 ${row.downvotes || 0}`
    },
    {
        title: '操作',
        key: 'actions',
        width: 160,
        render: (row) => h('div', { style: 'display:flex; gap:8px;' }, [
            h(NButton, { size: 'small', type: 'success', ghost: true, onClick: () => approve(row.id) }, { default: () => '通过' }),
            h(NButton, { size: 'small', type: 'error', ghost: true, onClick: () => reject(row.id) }, { default: () => '拒绝' })
        ])
    }
];
const comments = computed(() => result.value?.adminComments ?? []);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {[typeof AdminShell, typeof AdminShell, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(AdminShell, new AdminShell({
    title: "评论管理",
    subtitle: "审核与管理评论",
}));
const __VLS_1 = __VLS_0({
    title: "评论管理",
    subtitle: "审核与管理评论",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
var __VLS_3 = {};
__VLS_2.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_4 = {}.NInput;
/** @type {[typeof __VLS_components.NInput, typeof __VLS_components.nInput, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    value: (__VLS_ctx.blogIdInput),
    type: "text",
    placeholder: "博客ID（可选）",
}));
const __VLS_6 = __VLS_5({
    value: (__VLS_ctx.blogIdInput),
    type: "text",
    placeholder: "博客ID（可选）",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
const __VLS_8 = {}.NInput;
/** @type {[typeof __VLS_components.NInput, typeof __VLS_components.nInput, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    value: (__VLS_ctx.keyword),
    type: "text",
    placeholder: "关键词搜索",
}));
const __VLS_10 = __VLS_9({
    value: (__VLS_ctx.keyword),
    type: "text",
    placeholder: "关键词搜索",
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-filters" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.setStatus(null);
        } },
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.setStatus('pending');
        } },
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.setStatus('approved');
        } },
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.setStatus('rejected');
        } },
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.bulkApprove) },
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.bulkReject) },
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.bulkDelete) },
    ...{ class: "btn btn-soft" },
});
const __VLS_12 = {}.NDataTable;
/** @type {[typeof __VLS_components.NDataTable, typeof __VLS_components.nDataTable, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    ...{ 'onUpdate:checkedRowKeys': {} },
    columns: (__VLS_ctx.columns),
    data: (__VLS_ctx.comments),
    pagination: (false),
    rowKey: (row => row.id),
    ...{ style: {} },
}));
const __VLS_14 = __VLS_13({
    ...{ 'onUpdate:checkedRowKeys': {} },
    columns: (__VLS_ctx.columns),
    data: (__VLS_ctx.comments),
    pagination: (false),
    rowKey: (row => row.id),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
let __VLS_16;
let __VLS_17;
let __VLS_18;
const __VLS_19 = {
    'onUpdate:checkedRowKeys': (__VLS_ctx.onSelectionChange)
};
var __VLS_15;
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-filters']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            AdminShell: AdminShell,
            blogIdInput: blogIdInput,
            keyword: keyword,
            setStatus: setStatus,
            bulkApprove: bulkApprove,
            bulkReject: bulkReject,
            bulkDelete: bulkDelete,
            onSelectionChange: onSelectionChange,
            columns: columns,
            comments: comments,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
