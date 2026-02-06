import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useLazyQuery, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { RouterLink } from 'vue-router';
import PostCard from '../components/PostCard.vue';
import ThemeToggle from '../components/ThemeToggle.vue';
const pageSize = 12;
const BLOGS_QUERY = gql `
  query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
    blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
      items {
        id
        title
        summary
        coverUrl
        createdAt
        author {
          id
          username
          displayName
        }
        category {
          id
          name
        }
      }
    }
  }
`;
const SEARCH_BLOGS_QUERY = gql `
  query SearchBlogs($query: String!, $page: Int!, $size: Int!) {
    searchBlogs(query: $query, page: $page, size: $size) {
      total
      query
      items {
        id
        title
        summary
        titleHighlight
        summaryHighlight
        rank
        coverUrl
        createdAt
        author {
          id
          username
          displayName
        }
        category {
          id
          name
        }
      }
    }
  }
`;
const SEARCH_BLOGS_ES_QUERY = gql `
  query SearchBlogsEs($query: String!, $page: Int!, $size: Int!) {
    searchBlogsEs(query: $query, page: $page, size: $size) {
      total
      query
      items {
        id
        title
        summary
        titleHighlight
        summaryHighlight
        rank
        coverUrl
        createdAt
        author {
          id
          username
          displayName
        }
        category {
          id
          name
        }
      }
    }
  }
`;
const META_QUERY = gql `
  query Meta {
    categories {
      id
      name
    }
    tags {
      id
      name
    }
  }
`;
const HOT_BLOGS_QUERY = gql `
  query HotBlogs($limit: Int) {
    hotBlogs(limit: $limit) {
      id
      title
      likes
      dislikes
      views
    }
  }
`;
const { result } = useQuery(BLOGS_QUERY, {
    page: 1,
    size: pageSize,
    publishedOnly: true
});
const { load: loadSearch, result: searchResult, loading: searchLoading, refetch: refetchSearch } = useLazyQuery(SEARCH_BLOGS_QUERY);
const { load: loadSearchEs, result: searchEsResult, loading: searchEsLoading, refetch: refetchSearchEs } = useLazyQuery(SEARCH_BLOGS_ES_QUERY);
const { result: metaResult } = useQuery(META_QUERY);
const { result: hotResult } = useQuery(HOT_BLOGS_QUERY, { limit: 6 });
const posts = computed(() => result.value?.blogs?.items ?? []);
const searchInput = ref('');
const searchMode = ref('default');
const debouncedQuery = ref('');
const hasSearched = ref(false);
const hasSearchedEs = ref(false);
let debounceTimer = null;
watch(searchInput, (value) => {
    if (debounceTimer) {
        clearTimeout(debounceTimer);
    }
    debounceTimer = setTimeout(() => {
        debouncedQuery.value = value.trim();
    }, 300);
});
onBeforeUnmount(() => {
    if (debounceTimer) {
        clearTimeout(debounceTimer);
    }
});
const runSearch = async (query) => {
    const variables = { query, page: 1, size: pageSize };
    const useEs = searchMode.value === 'es';
    if (useEs) {
        if (!hasSearchedEs.value) {
            await loadSearchEs(undefined, variables);
            hasSearchedEs.value = true;
            return;
        }
        if (refetchSearchEs) {
            await refetchSearchEs(variables);
        }
        return;
    }
    if (!hasSearched.value) {
        await loadSearch(undefined, variables);
        hasSearched.value = true;
        return;
    }
    if (refetchSearch) {
        await refetchSearch(variables);
    }
};
watch(searchMode, () => {
    if (debouncedQuery.value) {
        runSearch(debouncedQuery.value);
    }
});
watch(debouncedQuery, (query) => {
    if (!query) {
        return;
    }
    runSearch(query);
});
const isSearching = computed(() => debouncedQuery.value.length > 0);
const activeSearchLoading = computed(() => (searchMode.value === 'es' ? searchEsLoading.value : searchLoading.value));
const searchItems = computed(() => {
    if (!isSearching.value) {
        return [];
    }
    if (searchMode.value === 'es') {
        return searchEsResult.value?.searchBlogsEs?.items ?? [];
    }
    return searchResult.value?.searchBlogs?.items ?? [];
});
const searchTotal = computed(() => {
    if (!isSearching.value) {
        return posts.value.length;
    }
    if (searchMode.value === 'es') {
        return searchEsResult.value?.searchBlogsEs?.total ?? 0;
    }
    return searchResult.value?.searchBlogs?.total ?? 0;
});
const activePosts = computed(() => (isSearching.value ? searchItems.value : posts.value));
const featuredPost = computed(() => {
    if (isSearching.value) {
        return null;
    }
    return activePosts.value[0];
});
const listPosts = computed(() => {
    if (featuredPost.value) {
        return activePosts.value.slice(1);
    }
    return activePosts.value;
});
const categories = computed(() => metaResult.value?.categories ?? []);
const tags = computed(() => metaResult.value?.tags ?? []);
const hotPosts = computed(() => hotResult.value?.hotBlogs ?? []);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "content-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "eyebrow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "post-meta" },
});
if (__VLS_ctx.isSearching) {
    (__VLS_ctx.debouncedQuery);
    (__VLS_ctx.searchTotal);
}
else {
    (__VLS_ctx.posts.length);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "content-controls" },
});
/** @type {[typeof ThemeToggle, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(ThemeToggle, new ThemeToggle({}));
const __VLS_1 = __VLS_0({}, ...__VLS_functionalComponentArgsRest(__VLS_0));
const __VLS_3 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
    ...{ class: "btn btn-ghost" },
    to: "/discover",
}));
const __VLS_5 = __VLS_4({
    ...{ class: "btn btn-ghost" },
    to: "/discover",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
var __VLS_6;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "content-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({});
if (__VLS_ctx.featuredPost) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
        ...{ class: "card fade-up" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "eyebrow" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
        ...{ class: "section-title" },
    });
    (__VLS_ctx.featuredPost.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.featuredPost.summary || 'A featured post worth reading first.');
    const __VLS_7 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
        ...{ class: "btn btn-primary" },
        to: (`/post/${__VLS_ctx.featuredPost.id}`),
    }));
    const __VLS_9 = __VLS_8({
        ...{ class: "btn btn-primary" },
        to: (`/post/${__VLS_ctx.featuredPost.id}`),
    }, ...__VLS_functionalComponentArgsRest(__VLS_8));
    __VLS_10.slots.default;
    var __VLS_10;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "post-list" },
});
if (__VLS_ctx.isSearching && __VLS_ctx.activeSearchLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "card" },
    });
}
else if (__VLS_ctx.isSearching && !__VLS_ctx.activePosts.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "card" },
    });
}
for (const [post] of __VLS_getVForSourceType((__VLS_ctx.listPosts))) {
    /** @type {[typeof PostCard, ]} */ ;
    // @ts-ignore
    const __VLS_11 = __VLS_asFunctionalComponent(PostCard, new PostCard({
        key: (post.id),
        post: (post),
        category: (post.category),
    }));
    const __VLS_12 = __VLS_11({
        key: (post.id),
        post: (post),
        category: (post.category),
    }, ...__VLS_functionalComponentArgsRest(__VLS_11));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "sidebar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "search-mode" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "searchMode",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    id: "searchMode",
    value: (__VLS_ctx.searchMode),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "default",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "es",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.searchInput),
    ...{ class: "search-input" },
    type: "text",
    placeholder: "Type keywords...",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
if (!__VLS_ctx.hotPosts.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "hot-empty" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.ol, __VLS_intrinsicElements.ol)({
        ...{ class: "hot-list" },
    });
    for (const [item, index] of __VLS_getVForSourceType((__VLS_ctx.hotPosts))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
            key: (item.id),
            ...{ class: "hot-item" },
        });
        const __VLS_14 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
            ...{ class: "hot-link" },
            to: (`/post/${item.id}`),
        }));
        const __VLS_16 = __VLS_15({
            ...{ class: "hot-link" },
            to: (`/post/${item.id}`),
        }, ...__VLS_functionalComponentArgsRest(__VLS_15));
        __VLS_17.slots.default;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "hot-rank" },
        });
        (index + 1);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "hot-title" },
        });
        (item.title);
        var __VLS_17;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "hot-meta" },
        });
        (item.likes || 0);
        (item.dislikes || 0);
        (item.views || 0);
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chip-list" },
});
for (const [cat] of __VLS_getVForSourceType((__VLS_ctx.categories))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        key: (cat.id),
        ...{ class: "chip" },
    });
    (cat.name);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chip-list" },
});
for (const [tag] of __VLS_getVForSourceType((__VLS_ctx.tags))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        key: (tag.id),
        ...{ class: "chip" },
    });
    (tag.name);
}
/** @type {__VLS_StyleScopedClasses['container']} */ ;
/** @type {__VLS_StyleScopedClasses['content-header']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['post-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['content-controls']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['content-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['fade-up']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['post-list']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['sidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['search-mode']} */ ;
/** @type {__VLS_StyleScopedClasses['search-input']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-list']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-item']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-link']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-rank']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-title']} */ ;
/** @type {__VLS_StyleScopedClasses['hot-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['chip-list']} */ ;
/** @type {__VLS_StyleScopedClasses['chip']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['chip-list']} */ ;
/** @type {__VLS_StyleScopedClasses['chip']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            PostCard: PostCard,
            ThemeToggle: ThemeToggle,
            posts: posts,
            searchInput: searchInput,
            searchMode: searchMode,
            debouncedQuery: debouncedQuery,
            isSearching: isSearching,
            activeSearchLoading: activeSearchLoading,
            searchTotal: searchTotal,
            activePosts: activePosts,
            featuredPost: featuredPost,
            listPosts: listPosts,
            categories: categories,
            tags: tags,
            hotPosts: hotPosts,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
