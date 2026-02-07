import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useMutation, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { marked } from 'marked';
import hljs from 'highlight.js';
import dayjs from 'dayjs';
import { Icon } from '@iconify/vue';
import { useAuthStore } from '../stores/auth';
const route = useRoute();
const postId = Number(route.params.id);
const auth = useAuthStore();
const { result, refetch } = useQuery(gql `
		query Blog($id: ID!) {
			blog(id: $id) {
				id
				title
				content
				coverUrl
				createdAt
				views
				likes
				dislikes
				userVote
				category {
					id
					name
				}
				tags {
					id
					name
				}
				author {
					id
					username
					displayName
				}
			}
			comments(blogId: $id) {
				id
				content
				authorName
				upvotes
				downvotes
				replies {
					id
					content
					authorName
					upvotes
					downvotes
					user {
						id
						username
						displayName
					}
				}
				user {
					id
					username
					displayName
				}
			}
		}
	`, { id: postId });
const post = computed(() => result.value?.blog);
const comments = computed(() => result.value?.comments ?? []);
const html = ref('');
const markdownEl = ref(null);
const tocItems = ref([]);
const activeHeadingId = ref('');
let headingObserver = null;
let signTimer = null;
const commentContent = ref('');
const commentAuthorName = ref('');
const submitting = ref(false);
const submitHint = ref('提交后需要审核，通过后显示。');
const { mutate: createComment } = useMutation(gql `
	mutation CreateComment($input: CommentInput!) {
		createComment(input: $input) {
			id
			status
		}
	}
`);
const { mutate: vote } = useMutation(gql `
	mutation VoteComment($id: ID!, $value: Int!) {
		voteComment(id: $id, value: $value) {
			id
			upvotes
			downvotes
		}
	}
`);
const { mutate: recordView } = useMutation(gql `
	mutation RecordBlogView($id: ID!) {
		recordBlogView(id: $id)
	}
`);
const { mutate: voteBlogMutation } = useMutation(gql `
	mutation VoteBlog($id: ID!, $value: Int!) {
		voteBlog(id: $id, value: $value) {
			id
			likes
			dislikes
			views
		}
	}
`);
const { mutate: createReply } = useMutation(gql `
	mutation CreateReply($input: ReplyInput!) {
		createReply(input: $input) {
			id
			status
		}
	}
`);
const TOC_MIN_HEADINGS = 4;
const TOC_MIN_CONTENT_LENGTH = 1200;
const showToc = computed(() => {
    const headingCount = tocItems.value.length;
    if (headingCount < 2)
        return false;
    if (headingCount >= TOC_MIN_HEADINGS)
        return true;
    const contentLength = post.value?.content?.length ?? 0;
    return contentLength >= TOC_MIN_CONTENT_LENGTH;
});
const authorName = computed(() => {
    return post.value?.author?.displayName || post.value?.author?.username || 'Anonymous';
});
const formattedDate = computed(() => {
    return post.value?.createdAt ? dayjs(post.value.createdAt).format('YYYY/MM/DD') : '';
});
const coverStyle = computed(() => ({
    backgroundImage: `url(${post.value?.coverUrl})`
}));
const submitComment = async () => {
    if (!commentContent.value.trim()) {
        submitHint.value = '评论内容不能为空。';
        return;
    }
    if (!auth.isAuthenticated && !commentAuthorName.value.trim()) {
        submitHint.value = '匿名评论请填写昵称。';
        return;
    }
    submitting.value = true;
    submitHint.value = '提交中...';
    try {
        await createComment({
            input: {
                blogId: postId,
                content: commentContent.value.trim(),
                authorName: auth.isAuthenticated ? null : commentAuthorName.value.trim()
            }
        });
        commentContent.value = '';
        submitHint.value = '已提交，等待审核通过后显示。';
        await refetch();
    }
    catch (error) {
        submitHint.value = error?.message || '提交失败，请稍后重试。';
    }
    finally {
        submitting.value = false;
    }
};
const voteBlog = async (value) => {
    try {
        await voteBlogMutation({ id: postId, value });
        await refetch();
    }
    catch {
        // ignore
    }
};
const voteComment = async (id, value) => {
    try {
        await vote({ id, value });
        await refetch();
    }
    catch {
        // ignore
    }
};
const replyContent = ref({});
const showReplyForm = ref({});
const toggleReplyForm = (id) => {
    showReplyForm.value = { ...showReplyForm.value, [id]: !showReplyForm.value[id] };
};
const submitReply = async (commentId) => {
    const content = (replyContent.value[commentId] || '').trim();
    if (!content)
        return;
    try {
        await createReply({
            input: {
                commentId,
                content,
                authorName: auth.isAuthenticated ? null : commentAuthorName.value.trim()
            }
        });
        replyContent.value = { ...replyContent.value, [commentId]: '' };
        showReplyForm.value = { ...showReplyForm.value, [commentId]: false };
        await refetch();
    }
    catch {
        // ignore
    }
};
const slugify = (value) => {
    const normalized = value
        .replace(/<[^>]+>/g, '')
        .trim()
        .toLowerCase()
        .replace(/[\u0000-\u001f]/g, '')
        .replace(/[^a-z0-9\u4e00-\u9fa5\s-]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-');
    return normalized || 'section';
};
const disconnectObserver = () => {
    if (headingObserver) {
        headingObserver.disconnect();
        headingObserver = null;
    }
};
const setupHeadingObserver = () => {
    disconnectObserver();
    const root = markdownEl.value;
    if (!root)
        return;
    const headings = Array.from(root.querySelectorAll('h2[id], h3[id], h4[id]'));
    if (!headings.length)
        return;
    headingObserver = new IntersectionObserver(entries => {
        const visible = entries.filter(entry => entry.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]?.target) {
            activeHeadingId.value = visible[0].target.id;
        }
    }, {
        root: null,
        rootMargin: '-96px 0px -70% 0px',
        threshold: [0, 1]
    });
    headings.forEach(heading => headingObserver?.observe(heading));
    activeHeadingId.value = headings[0].id;
};
const highlightBlocks = async () => {
    await nextTick();
    const blocks = markdownEl.value?.querySelectorAll('pre code') ?? [];
    blocks.forEach(block => {
        hljs.highlightElement(block);
    });
};
const renderMarkdown = async (content) => {
    if (!content) {
        html.value = '';
        tocItems.value = [];
        activeHeadingId.value = '';
        disconnectObserver();
        return;
    }
    const renderer = new marked.Renderer();
    const slugCounts = new Map();
    const nextToc = [];
    renderer.image = token => {
        const src = token.href ?? '';
        const title = token.title ? ` title="${escapeHtmlAttr(token.title)}"` : '';
        const alt = token.text ? escapeHtmlAttr(token.text) : 'image';
        if (isVideoUrl(src)) {
            return `<video controls preload="metadata"${title}><source src="${escapeHtmlAttr(src)}"></video>`;
        }
        return `<img src="${escapeHtmlAttr(src)}" alt="${alt}" loading="lazy"${title} />`;
    };
    renderer.heading = (token) => {
        const level = token.depth;
        const plainText = token.text;
        const headingHtml = marked.parseInline(token.text);
        const base = slugify(plainText);
        const count = (slugCounts.get(base) ?? 0) + 1;
        slugCounts.set(base, count);
        const id = count === 1 ? base : `${base}-${count}`;
        if (level >= 2 && level <= 4) {
            nextToc.push({ id, text: plainText, level });
        }
        return `<h${level} id="${id}">${headingHtml}</h${level}>`;
    };
    html.value = marked.parse(content, { renderer });
    tocItems.value = nextToc;
    await nextTick();
    scheduleSignMedia();
    await highlightBlocks();
    setupHeadingObserver();
};
const isVideoUrl = (value) => {
    const clean = value.split('?')[0].split('#')[0].toLowerCase();
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.ogg') || clean.endsWith('.mov') || clean.endsWith('.m4v');
};
const escapeHtmlAttr = (value) => {
    return value
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
};
const scheduleSignMedia = () => {
    if (signTimer) {
        window.clearTimeout(signTimer);
    }
    signTimer = window.setTimeout(() => {
        signMediaSources();
    }, 120);
};
const signMediaSources = async () => {
    const root = markdownEl.value;
    if (!root)
        return;
    const targets = Array.from(root.querySelectorAll('img, video source'));
    const urls = Array.from(new Set(targets.map(node => node.getAttribute('src')).filter((value) => Boolean(value))));
    if (!urls.length)
        return;
    try {
        const signedUrls = await fetchSignedUrls(urls);
        if (!signedUrls)
            return;
        targets.forEach(node => {
            const src = node.getAttribute('src');
            if (!src)
                return;
            const signed = signedUrls[src];
            if (!signed)
                return;
            node.setAttribute('src', signed);
            if (node instanceof HTMLSourceElement) {
                const parent = node.parentElement;
                parent?.load();
            }
        });
    }
    catch {
        // ignore signing failures to avoid breaking render
    }
};
const fetchSignedUrls = async (urls) => {
    const api = import.meta.env.VITE_API_URL ?? 'http://localhost:8888/graphql';
    const signUrl = api.replace(/\/graphql\/?$/, '') + '/api/media/sign';
    const response = await fetch(signUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ urls })
    });
    if (!response.ok)
        return null;
    const data = await response.json();
    return data?.signedUrls;
};
const scrollToHeading = (id) => {
    const selector = `#${CSS.escape(id)}`;
    const target = markdownEl.value?.querySelector(selector);
    if (!target)
        return;
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    activeHeadingId.value = id;
};
watch(() => post.value?.content, content => {
    renderMarkdown(content);
}, { immediate: true });
onMounted(() => {
    setupHeadingObserver();
    recordView({ id: postId }).catch(() => null);
});
onBeforeUnmount(() => {
    if (signTimer) {
        window.clearTimeout(signTimer);
        signTimer = null;
    }
    disconnectObserver();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "container" },
});
if (__VLS_ctx.post) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "blog-shell" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "blog-article" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "article-hero" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "eyebrow" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
    (__VLS_ctx.post.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "article-meta" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.authorName);
    if (__VLS_ctx.formattedDate) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.formattedDate);
    }
    if (__VLS_ctx.post.category) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.post.category.name);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "article-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.post))
                    return;
                __VLS_ctx.voteBlog(1);
            } },
        ...{ class: "article-action vote-btn" },
        type: "button",
        ...{ class: ({ 'is-active': __VLS_ctx.post.userVote === 1 }) },
        'aria-label': "Like",
    });
    const __VLS_0 = {}.Icon;
    /** @type {[typeof __VLS_components.Icon, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        ...{ class: "vote-icon" },
        icon: (__VLS_ctx.post.userVote === 1 ? 'mdi:thumb-up' : 'mdi:thumb-up-outline'),
    }));
    const __VLS_2 = __VLS_1({
        ...{ class: "vote-icon" },
        icon: (__VLS_ctx.post.userVote === 1 ? 'mdi:thumb-up' : 'mdi:thumb-up-outline'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.post.likes || 0);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.post))
                    return;
                __VLS_ctx.voteBlog(-1);
            } },
        ...{ class: "article-action vote-btn" },
        type: "button",
        ...{ class: ({ 'is-active': __VLS_ctx.post.userVote === -1 }) },
        'aria-label': "Dislike",
    });
    const __VLS_4 = {}.Icon;
    /** @type {[typeof __VLS_components.Icon, ]} */ ;
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        ...{ class: "vote-icon" },
        icon: (__VLS_ctx.post.userVote === -1 ? 'mdi:thumb-down' : 'mdi:thumb-down-outline'),
    }));
    const __VLS_6 = __VLS_5({
        ...{ class: "vote-icon" },
        icon: (__VLS_ctx.post.userVote === -1 ? 'mdi:thumb-down' : 'mdi:thumb-down-outline'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.post.dislikes || 0);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "article-action meta" },
    });
    (__VLS_ctx.post.views || 0);
    if (__VLS_ctx.post.coverUrl) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "article-cover" },
            ...{ style: (__VLS_ctx.coverStyle) },
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "article-body" },
    });
    if (__VLS_ctx.post.tags?.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "chip-list" },
        });
        for (const [tag] of __VLS_getVForSourceType((__VLS_ctx.post.tags))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                key: (tag.id),
                ...{ class: "chip" },
            });
            (tag.name);
        }
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "markdown" },
        ref: "markdownEl",
    });
    __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.html) }, null, null);
    /** @type {typeof __VLS_ctx.markdownEl} */ ;
    if (__VLS_ctx.showToc) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
            ...{ class: "article-toc card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({
            ...{ class: "toc-list" },
            'aria-label': "Article table of contents",
        });
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.tocItems))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.post))
                            return;
                        if (!(__VLS_ctx.showToc))
                            return;
                        __VLS_ctx.scrollToHeading(item.id);
                    } },
                key: (item.id),
                type: "button",
                ...{ class: "toc-link" },
                ...{ class: ([`level-${item.level}`, { active: item.id === __VLS_ctx.activeHeadingId }]) },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "toc-dot" },
                'aria-hidden': "true",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "toc-text" },
            });
            (item.text);
        }
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card comment-form" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "comment-form-body" },
});
if (!__VLS_ctx.auth.isAuthenticated) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "comment-form-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        value: (__VLS_ctx.commentAuthorName),
        type: "text",
        placeholder: "你的昵称",
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "comment-form-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.commentContent),
    rows: "4",
    placeholder: "写下你的评论...",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "comment-form-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "comment-form-hint" },
});
(__VLS_ctx.submitHint);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.submitComment) },
    ...{ class: "btn btn-primary" },
    disabled: (__VLS_ctx.submitting),
});
(__VLS_ctx.submitting ? '提交中...' : '提交评论');
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
    ...{ style: {} },
});
if (__VLS_ctx.comments.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "comment-list" },
    });
    for (const [comment] of __VLS_getVForSourceType((__VLS_ctx.comments))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (comment.id),
            ...{ class: "comment-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "comment-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (comment.user?.displayName || comment.user?.username || comment.authorName || '匿名用户');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        (comment.content);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "comment-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.comments.length))
                        return;
                    __VLS_ctx.voteComment(comment.id, 1);
                } },
            ...{ class: "comment-action" },
            type: "button",
        });
        (comment.upvotes || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.comments.length))
                        return;
                    __VLS_ctx.voteComment(comment.id, -1);
                } },
            ...{ class: "comment-action" },
            type: "button",
        });
        (comment.downvotes || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.comments.length))
                        return;
                    __VLS_ctx.toggleReplyForm(comment.id);
                } },
            ...{ class: "comment-action" },
            type: "button",
        });
        if (__VLS_ctx.showReplyForm[comment.id]) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "comment-reply-form" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
                value: (__VLS_ctx.replyContent[comment.id]),
                rows: "3",
                placeholder: "写下你的回复...",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "comment-reply-actions" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.comments.length))
                            return;
                        if (!(__VLS_ctx.showReplyForm[comment.id]))
                            return;
                        __VLS_ctx.toggleReplyForm(comment.id);
                    } },
                ...{ class: "btn btn-soft" },
                type: "button",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.comments.length))
                            return;
                        if (!(__VLS_ctx.showReplyForm[comment.id]))
                            return;
                        __VLS_ctx.submitReply(comment.id);
                    } },
                ...{ class: "btn btn-primary" },
                type: "button",
            });
        }
        if (comment.replies?.length) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "comment-replies" },
            });
            for (const [reply] of __VLS_getVForSourceType((comment.replies))) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    key: (reply.id),
                    ...{ class: "comment-item reply-item" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "comment-header" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
                (reply.user?.displayName || reply.user?.username || reply.authorName || '匿名用户');
                __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
                (reply.content);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "comment-actions" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!(__VLS_ctx.comments.length))
                                return;
                            if (!(comment.replies?.length))
                                return;
                            __VLS_ctx.voteComment(reply.id, 1);
                        } },
                    ...{ class: "comment-action" },
                    type: "button",
                });
                (reply.upvotes || 0);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!(__VLS_ctx.comments.length))
                                return;
                            if (!(comment.replies?.length))
                                return;
                            __VLS_ctx.voteComment(reply.id, -1);
                        } },
                    ...{ class: "comment-action" },
                    type: "button",
                });
                (reply.downvotes || 0);
            }
        }
    }
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "comment-empty" },
    });
}
/** @type {__VLS_StyleScopedClasses['container']} */ ;
/** @type {__VLS_StyleScopedClasses['blog-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['blog-article']} */ ;
/** @type {__VLS_StyleScopedClasses['article-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['article-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['article-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['article-action']} */ ;
/** @type {__VLS_StyleScopedClasses['vote-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['vote-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['article-action']} */ ;
/** @type {__VLS_StyleScopedClasses['vote-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['vote-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['article-action']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['article-cover']} */ ;
/** @type {__VLS_StyleScopedClasses['article-body']} */ ;
/** @type {__VLS_StyleScopedClasses['chip-list']} */ ;
/** @type {__VLS_StyleScopedClasses['chip']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown']} */ ;
/** @type {__VLS_StyleScopedClasses['article-toc']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['toc-list']} */ ;
/** @type {__VLS_StyleScopedClasses['toc-link']} */ ;
/** @type {__VLS_StyleScopedClasses['toc-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['toc-text']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-form']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-form-body']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-form-row']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-form-row']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-form-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-form-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-list']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-item']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-header']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-action']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-action']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-action']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-reply-form']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-reply-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-soft']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-replies']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-item']} */ ;
/** @type {__VLS_StyleScopedClasses['reply-item']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-header']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-action']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-action']} */ ;
/** @type {__VLS_StyleScopedClasses['comment-empty']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            Icon: Icon,
            auth: auth,
            post: post,
            comments: comments,
            html: html,
            markdownEl: markdownEl,
            tocItems: tocItems,
            activeHeadingId: activeHeadingId,
            commentContent: commentContent,
            commentAuthorName: commentAuthorName,
            submitting: submitting,
            submitHint: submitHint,
            showToc: showToc,
            authorName: authorName,
            formattedDate: formattedDate,
            coverStyle: coverStyle,
            submitComment: submitComment,
            voteBlog: voteBlog,
            voteComment: voteComment,
            replyContent: replyContent,
            showReplyForm: showReplyForm,
            toggleReplyForm: toggleReplyForm,
            submitReply: submitReply,
            scrollToHeading: scrollToHeading,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
