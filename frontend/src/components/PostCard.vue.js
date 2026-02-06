import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import dayjs from 'dayjs';
const props = defineProps();
const titleHighlight = computed(() => props.post?.titleHighlight);
const summaryHighlight = computed(() => props.post?.summaryHighlight);
const coverStyle = computed(() => {
    const cover = props.post?.coverUrl;
    if (cover) {
        return { backgroundImage: `url(${cover})` };
    }
    return { backgroundImage: 'linear-gradient(135deg, #ffd3b1, #ffe7c7)' };
});
const authorName = computed(() => {
    return props.post?.author?.displayName || props.post?.author?.username || 'Anonymous';
});
const formattedDate = computed(() => {
    return props.post?.createdAt ? dayjs(props.post.createdAt).format('YYYY/MM/DD') : '';
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "post-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "post-cover" },
    ...{ style: (__VLS_ctx.coverStyle) },
});
if (__VLS_ctx.category) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "post-tag" },
    });
    (__VLS_ctx.category.name);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "post-body" },
});
if (__VLS_ctx.titleHighlight) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.titleHighlight) }, null, null);
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.post.title);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "post-meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.authorName);
if (__VLS_ctx.formattedDate) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.formattedDate);
}
if (__VLS_ctx.summaryHighlight) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.summaryHighlight) }, null, null);
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.post.summary || 'No summary yet.');
}
const __VLS_0 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ class: "post-link" },
    to: (`/post/${__VLS_ctx.post.id}`),
}));
const __VLS_2 = __VLS_1({
    ...{ class: "post-link" },
    to: (`/post/${__VLS_ctx.post.id}`),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['post-card']} */ ;
/** @type {__VLS_StyleScopedClasses['post-cover']} */ ;
/** @type {__VLS_StyleScopedClasses['post-tag']} */ ;
/** @type {__VLS_StyleScopedClasses['post-body']} */ ;
/** @type {__VLS_StyleScopedClasses['post-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['post-link']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            titleHighlight: titleHighlight,
            summaryHighlight: summaryHighlight,
            coverStyle: coverStyle,
            authorName: authorName,
            formattedDate: formattedDate,
        };
    },
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
