import { computed, h } from 'vue';
import { RouterLink } from 'vue-router';
import { useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { NTag } from 'naive-ui';
import AdminShell from '../components/AdminShell.vue';
const rowKey = (row) => row.id;
const columns = [
    {
        type: 'selection'
    },
    {
        title: '标题',
        key: 'title'
    },
    {
        title: '状态',
        key: 'published',
        render: (row) => h(NTag, { type: row.published ? 'success' : 'info' }, { default: () => (row.published ? '已发布' : '草稿') })
    },
    {
        title: '操作',
        key: 'actions',
        render: (row) => h(RouterLink, { to: `/admin/editor/${row.id}` }, { default: () => '编辑' })
    }
];
const { result } = useQuery(gql `
    query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
      blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
        items {
          id
          title
          published
        }
      }
    }
  `, { page: 1, size: 20, publishedOnly: false });
const posts = computed(() => result.value?.blogs?.items ?? []);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {[typeof AdminShell, typeof AdminShell, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(AdminShell, new AdminShell({
    title: "文章管理",
    subtitle: "维护站点内容与发布状态",
}));
const __VLS_1 = __VLS_0({
    title: "文章管理",
    subtitle: "维护站点内容与发布状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
var __VLS_3 = {};
__VLS_2.slots.default;
{
    const { actions: __VLS_thisSlot } = __VLS_2.slots;
    const __VLS_4 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        ...{ class: "btn btn-primary" },
        to: "/admin/editor",
    }));
    const __VLS_6 = __VLS_5({
        ...{ class: "btn btn-primary" },
        to: "/admin/editor",
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    __VLS_7.slots.default;
    var __VLS_7;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_8 = {}.NInput;
/** @type {[typeof __VLS_components.NInput, typeof __VLS_components.nInput, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    type: "text",
    placeholder: "输入关键词搜索",
}));
const __VLS_10 = __VLS_9({
    type: "text",
    placeholder: "输入关键词搜索",
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-filters" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "btn btn-soft" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "btn btn-soft" },
});
const __VLS_12 = {}.NDataTable;
/** @type {[typeof __VLS_components.NDataTable, typeof __VLS_components.nDataTable, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    columns: (__VLS_ctx.columns),
    data: (__VLS_ctx.posts),
    pagination: (false),
    rowKey: (__VLS_ctx.rowKey),
    ...{ style: {} },
}));
const __VLS_14 = __VLS_13({
    columns: (__VLS_ctx.columns),
    data: (__VLS_ctx.posts),
    pagination: (false),
    rowKey: (__VLS_ctx.rowKey),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-filters']} */ ;
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
            RouterLink: RouterLink,
            AdminShell: AdminShell,
            rowKey: rowKey,
            columns: columns,
            posts: posts,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
