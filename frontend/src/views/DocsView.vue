<template>
	<div class="container docs-shell">
		<aside class="card docs-tree">
			<div class="docs-tree-header">
				<h3>Docs</h3>
				<span v-if="loading" class="docs-loading">Loading</span>
			</div>
			<div class="docs-search">
				<input v-model="searchText" type="text" class="docs-search-input" placeholder="Search documents..." />
				<button v-if="searchQuery" class="docs-search-clear" type="button" @click="clearSearch">Clear</button>
			</div>
			<div v-if="showEmpty" class="docs-empty">{{ emptyText }}</div>
			<n-tree
				v-else
				:data="treeOptions"
				block-line
				expand-on-click
				selectable
				:selected-keys="selectedKeys"
				:default-expand-all="true"
				:on-update:selected-keys="onSelect"
				:render-label="searchQuery.trim() ? renderLabel : undefined"
			/>
		</aside>

		<section class="docs-content" v-if="doc">
			<div class="card docs-article">
				<div class="docs-header">
					<span class="eyebrow">Document</span>
					<h1>{{ doc.title }}</h1>
				</div>
				<div v-if="doc.type === 'FOLDER'" class="docs-folder">This is a folder. Select a document to read.</div>
				<div v-else class="markdown" ref="markdownEl" v-html="html"></div>
			</div>

			<aside v-if="showToc" class="card docs-toc">
				<h4>Contents</h4>
				<nav class="toc-list" aria-label="Document table of contents">
					<button v-for="item in tocItems" :key="item.id" type="button" class="toc-link" :class="[`level-${item.level}`, { active: item.id === activeHeadingId }]" @click="scrollToHeading(item.id)">
						<span class="toc-dot" aria-hidden="true"></span>
						<span class="toc-text">{{ item.text }}</span>
					</button>
				</nav>
			</aside>
		</section>

		<section v-else class="docs-content">
			<div class="card docs-article docs-empty">Select a document to start reading.</div>
		</section>
	</div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { marked, type Tokens } from 'marked';
import hljs from 'highlight.js';
import type { TreeOption } from 'naive-ui';

type TocItem = {
	id: string;
	text: string;
	level: number;
};

type DocumentNode = {
	id: number;
	title: string;
	parentId: number | null;
	type: 'FOLDER' | 'DOC';
	path: string;
	sortOrder: number;
	hidden: boolean;
	depth: number;
	children: DocumentNode[];
	content?: string | null;
};

type DocTreeOption = TreeOption & {
	content?: string;
};

const route = useRoute();
const router = useRouter();
const selectedId = computed(() => (route.params.id ? Number(route.params.id) : null));
const activeQuery = computed(() => (route.query.q ? String(route.query.q) : ''));
const searchText = ref('');
const searchQuery = ref('');
let searchTimer: number | null = null;

const { result, loading } = useQuery(gql`
	query Documents {
		documents {
			id
			title
			parentId
			type
			path
			sortOrder
			hidden
			depth
		}
	}
`);

const { result: docResult } = useQuery(
	gql`
		query Document($id: ID!) {
			document(id: $id) {
				id
				title
				content
				type
			}
		}
	`,
	() => ({ id: selectedId.value }),
	{ enabled: computed(() => !!selectedId.value) }
);

const { result: searchResult, loading: searchLoading } = useQuery(
	gql`
		query SearchDocuments($query: String!, $page: Int!, $size: Int!) {
			searchDocuments(query: $query, page: $page, size: $size) {
				items {
					id
					title
					type
					hidden
					path
				}
				total
			}
		}
	`,
	() => ({ query: searchQuery.value, page: 1, size: 50 }),
	{ enabled: computed(() => searchQuery.value.trim().length > 0) }
);

const html = ref('');
const markdownEl = ref<HTMLElement | null>(null);
const tocItems = ref<TocItem[]>([]);
const activeHeadingId = ref('');
let headingObserver: IntersectionObserver | null = null;
let signTimer: number | null = null;

const nodes = computed<DocumentNode[]>(() => result.value?.documents ?? []);

const tree = computed(() => buildTree(nodes.value));
const flatNodes = computed(() => flattenTree(tree.value));
const doc = computed(() => docResult.value?.document ?? null);
const selectedKeys = computed(() => (selectedId.value ? [selectedId.value] : []));
const searchItems = computed<DocumentNode[]>(() => {
	const items = searchResult.value?.searchDocuments?.items ?? [];
	return items.map((item: any) => ({
		id: Number(item.id),
		title: item.title,
		parentId: null,
		type: item.type,
		path: item.path,
		sortOrder: 0,
		hidden: Boolean(item.hidden),
		depth: 0,
		children: [],
		content: item.content ?? null
	}));
});

const displayNodes = computed(() => {
	if (searchQuery.value.trim()) {
		return searchItems.value;
	}
	return flatNodes.value;
});

const treeOptions = computed<DocTreeOption[]>(() => {
	if (searchQuery.value.trim()) {
		return searchItems.value.map(node => ({
			key: node.id,
			label: node.title,
			isLeaf: node.type === 'DOC',
			children: undefined,
			content: node.content
		}));
	}
	return buildTreeOptions(tree.value);
});

const showEmpty = computed(() => {
	if (searchQuery.value.trim()) {
		return !searchLoading.value && searchItems.value.length === 0;
	}
	return flatNodes.value.length === 0;
});

const emptyText = computed(() => {
	if (searchQuery.value.trim()) {
		return 'No results.';
	}
	return 'No documents yet.';
});

const showToc = computed(() => {
	const headingCount = tocItems.value.length;
	if (headingCount >= 3) return true;
	const length = doc.value?.content?.length ?? 0;
	return headingCount >= 2 && length >= 800;
});

const selectNode = (node: DocumentNode) => {
	if (searchQuery.value.trim()) {
		router.push({ path: `/docs/${node.id}`, query: { q: searchQuery.value.trim() } });
	} else {
		router.push(`/docs/${node.id}`);
	}
};

const onSelect = (keys: Array<string | number>) => {
	const next = keys.length ? Number(keys[0]) : null;
	if (next) {
		const node = displayNodes.value.find(item => item.id === next);
		if (node) {
			selectNode(node);
			return;
		}
		if (searchQuery.value.trim()) {
			router.push({ path: `/docs/${next}`, query: { q: searchQuery.value.trim() } });
		} else {
			router.push(`/docs/${next}`);
		}
	}
};

const renderLabel = (info: { option: DocTreeOption }) => {
	const option = info.option;
	const label = option.label as string;
	const content = option.content;
	if (searchQuery.value.trim()) {
		return h('div', { class: 'docs-tree-label' }, [
			h('span', { class: 'docs-title', style: { color: 'var(--text)' }, innerHTML: label }),
			content ? h('div', { class: 'docs-snippet', innerHTML: content }) : null
		]);
	}
	return h('span', { class: 'docs-title', style: { color: 'var(--text)' } }, label);
};

const ensureDefaultSelection = () => {
	if (searchQuery.value.trim()) {
		return;
	}
	if (selectedId.value || flatNodes.value.length === 0) {
		return;
	}
	const firstDoc = flatNodes.value.find(node => node.type === 'DOC') ?? flatNodes.value[0];
	if (firstDoc) {
		router.replace(`/docs/${firstDoc.id}`);
	}
};

const scheduleSearch = () => {
	if (searchTimer) {
		window.clearTimeout(searchTimer);
	}
	searchTimer = window.setTimeout(() => {
		searchQuery.value = searchText.value.trim();
	}, 250);
};

const clearSearch = () => {
	searchText.value = '';
	searchQuery.value = '';
};

const renderMarkdown = async (content: string | undefined) => {
	if (!content) {
		html.value = '';
		tocItems.value = [];
		activeHeadingId.value = '';
		disconnectObserver();
		return;
	}
	const renderer = new marked.Renderer();
	const slugCounts = new Map<string, number>();
	const nextToc: TocItem[] = [];

	renderer.image = token => {
		const src = token.href ?? '';
		const title = token.title ? ` title="${escapeHtmlAttr(token.title)}"` : '';
		const alt = token.text ? escapeHtmlAttr(token.text) : 'image';
		if (isVideoUrl(src)) {
			return `<video controls preload="metadata"${title}><source src="${escapeHtmlAttr(src)}"></video>`;
		}
		return `<img src="${escapeHtmlAttr(src)}" alt="${alt}" loading="lazy"${title} />`;
	};

	renderer.heading = (token: Tokens.Heading) => {
		const level = token.depth;
		const plainText = token.text;
		const headingHtml = marked.parseInline(token.text) as string;
		const base = slugify(plainText);
		const count = (slugCounts.get(base) ?? 0) + 1;
		slugCounts.set(base, count);
		const id = count === 1 ? base : `${base}-${count}`;

		if (level >= 2 && level <= 4) {
			nextToc.push({ id, text: plainText, level });
		}

		return `<h${level} id="${id}">${headingHtml}</h${level}>`;
	};

	html.value = marked.parse(content, { renderer }) as string;
	tocItems.value = nextToc;

	await nextTick();
	scheduleSignMedia();
	highlightBlocks();
	setupHeadingObserver();
	highlightSearchMatches(activeQuery.value);
};

const highlightBlocks = () => {
	const blocks = markdownEl.value?.querySelectorAll('pre code') ?? [];
	blocks.forEach(block => {
		hljs.highlightElement(block as HTMLElement);
	});
};

const isVideoUrl = (value: string) => {
	const clean = value.split('?')[0].split('#')[0].toLowerCase();
	return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.ogg') || clean.endsWith('.mov') || clean.endsWith('.m4v');
};

const escapeHtmlAttr = (value: string) => {
	return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
};

const slugify = (value: string) => {
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
	if (!root) return;

	const headings = Array.from(root.querySelectorAll<HTMLElement>('h2[id], h3[id], h4[id]'));
	if (!headings.length) return;

	headingObserver = new IntersectionObserver(
		entries => {
			const visible = entries.filter(entry => entry.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
			if (visible[0]?.target) {
				activeHeadingId.value = (visible[0].target as HTMLElement).id;
			}
		},
		{
			root: null,
			rootMargin: '-96px 0px -70% 0px',
			threshold: [0, 1]
		}
	);

	headings.forEach(heading => headingObserver?.observe(heading));
	activeHeadingId.value = headings[0].id;
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
	if (!root) return;
	const targets = Array.from(root.querySelectorAll<HTMLImageElement | HTMLSourceElement>('img, video source'));
	const urls = Array.from(new Set(targets.map(node => node.getAttribute('src')).filter((value): value is string => Boolean(value))));
	if (!urls.length) return;
	try {
		const signedUrls = await fetchSignedUrls(urls);
		if (!signedUrls) return;
		targets.forEach(node => {
			const src = node.getAttribute('src');
			if (!src) return;
			const signed = signedUrls[src];
			if (!signed) return;
			node.setAttribute('src', signed);
			if (node instanceof HTMLSourceElement) {
				const parent = node.parentElement as HTMLVideoElement | null;
				parent?.load();
			}
		});
	} catch {
		// ignore signing failures
	}
};

const fetchSignedUrls = async (urls: string[]) => {
	const api = import.meta.env.VITE_API_URL ?? '/graphql';
	const signUrl = api.replace(/\/graphql\/?$/, '') + '/api/media/sign';
	const response = await fetch(signUrl, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ urls })
	});
	if (!response.ok) return null;
	const data = await response.json();
	return data?.signedUrls as Record<string, string> | null;
};

const scrollToHeading = (id: string) => {
	const selector = `#${CSS.escape(id)}`;
	const target = markdownEl.value?.querySelector<HTMLElement>(selector);
	if (!target) return;
	target.scrollIntoView({ behavior: 'smooth', block: 'start' });
	activeHeadingId.value = id;
};

const highlightSearchMatches = (query: string) => {
	const root = markdownEl.value;
	if (!root) return;
	clearMarks(root);
	const tokens = query.trim().split(/\s+/).filter(Boolean);
	if (!tokens.length) return;
	const regex = new RegExp(`(${tokens.map(escapeRegExp).join('|')})`, 'gi');
	const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
	const textNodes: Text[] = [];
	let node = walker.nextNode();
	while (node) {
		const parent = (node as Text).parentElement;
		if (parent) {
			const tag = parent.tagName.toLowerCase();
			if (tag === 'code' || tag === 'pre' || tag === 'a' || tag === 'mark') {
				node = walker.nextNode();
				continue;
			}
			if (parent.closest('.docs-toc')) {
				node = walker.nextNode();
				continue;
			}
		}
		textNodes.push(node as Text);
		node = walker.nextNode();
	}

	textNodes.forEach(textNode => {
		const text = textNode.nodeValue ?? '';
		if (!regex.test(text)) return;
		const frag = document.createDocumentFragment();
		let lastIndex = 0;
		text.replace(regex, (match, _group, offset) => {
			if (offset > lastIndex) {
				frag.appendChild(document.createTextNode(text.slice(lastIndex, offset)));
			}
			const mark = document.createElement('mark');
			mark.textContent = match;
			frag.appendChild(mark);
			lastIndex = offset + match.length;
			return match;
		});
		if (lastIndex < text.length) {
			frag.appendChild(document.createTextNode(text.slice(lastIndex)));
		}
		textNode.parentNode?.replaceChild(frag, textNode);
	});
};

const clearMarks = (root: HTMLElement) => {
	const marks = Array.from(root.querySelectorAll('mark'));
	marks.forEach(mark => {
		const parent = mark.parentNode;
		if (!parent) return;
		parent.replaceChild(document.createTextNode(mark.textContent ?? ''), mark);
		parent.normalize();
	});
};

const escapeRegExp = (value: string) => {
	return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

const buildTree = (items: DocumentNode[]) => {
	const map = new Map<number, DocumentNode>();
	items.forEach(item => {
		map.set(item.id, { ...item, children: [] });
	});
	const roots: DocumentNode[] = [];
	map.forEach(item => {
		if (item.parentId != null && map.has(item.parentId)) {
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

watch(() => flatNodes.value, ensureDefaultSelection, { immediate: true });
watch(
	() => doc.value?.content,
	content => {
		renderMarkdown(content);
	},
	{ immediate: true }
);
watch(
	() => searchQuery.value,
	value => {
		if (!value.trim()) {
			router.replace({ query: {} });
		}
	}
);
watch(
	() => activeQuery.value,
	query => {
		if (!doc.value?.content) return;
		highlightSearchMatches(query);
	}
);
watch(() => searchText.value, scheduleSearch);

onBeforeUnmount(() => {
	if (signTimer) {
		window.clearTimeout(signTimer);
		signTimer = null;
	}
	if (searchTimer) {
		window.clearTimeout(searchTimer);
		searchTimer = null;
	}
	disconnectObserver();
});
</script>

<style scoped>
.docs-shell {
	display: grid;
	grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
	gap: 24px;
	align-items: start;
}

.docs-tree {
	position: sticky;
	top: 96px;
	max-height: calc(100vh - 140px);
	overflow: auto;
	padding: 18px;
	color: var(--text);
}

.docs-tree-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.docs-tree-header h3 {
	margin: 0;
}

.docs-search {
	display: flex;
	gap: 8px;
	align-items: center;
	margin-bottom: 12px;
}

.docs-search-input {
	flex: 1;
	border-radius: 999px;
	padding: 8px 12px;
	border: 1px solid var(--line);
	background: var(--bg-strong);
	color: var(--text);
}

.docs-search-clear {
	border-radius: 999px;
	border: 1px solid var(--line);
	background: var(--panel-strong);
	color: var(--text);
	padding: 6px 12px;
	cursor: pointer;
}

.docs-search-clear:hover {
	border-color: var(--theme);
	color: var(--theme);
}

.docs-loading {
	color: var(--muted);
	font-size: 0.85rem;
}

.docs-title {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.docs-tree-label {
	display: grid;
	gap: 2px;
}

.docs-snippet {
	margin: 0;
	font-size: 0.85rem;
	color: var(--text-p2);
}

.docs-content {
	display: grid;
	gap: 16px;
	grid-template-columns: minmax(0, 1fr) minmax(200px, 240px);
	align-items: start;
}

.docs-article {
	padding: 26px;
	min-height: 360px;
}

.docs-header h1 {
	margin: 8px 0 0;
	font-family: var(--font-display);
}

.docs-folder {
	margin-top: 16px;
	color: var(--muted);
}

.docs-empty {
	color: var(--muted);
}

.docs-toc {
	position: sticky;
	top: 96px;
	max-height: calc(100vh - 140px);
	overflow: auto;
	padding: 18px 16px;
}

@media (max-width: 980px) {
	.docs-shell {
		grid-template-columns: 1fr;
	}

	.docs-tree {
		position: static;
		max-height: none;
	}

	.docs-content {
		grid-template-columns: 1fr;
	}

	.docs-toc {
		position: static;
	}
}

/* Dark mode tree text */
:global([data-theme='dark'] .n-tree .n-tree-node-content__text) {
	color: #fff;
}

:global(.docs-tree .n-tree .n-tree-node-content),
:global(.docs-tree .n-tree .n-tree-node-content__text),
:global(.docs-tree .n-tree .n-tree-node-content__text .docs-title) {
	color: var(--text);
}

:global([data-theme='dark'] .docs-tree .n-tree .n-tree-node-content),
:global([data-theme='dark'] .docs-tree .n-tree .n-tree-node-content__text),
:global([data-theme='dark'] .docs-tree .n-tree .n-tree-node-content__text .docs-title) {
	color: #fff;
}

:global(.docs-tree .n-tree .n-tree-node-content:hover) {
	background: var(--block);
}

:global(.docs-tree .n-tree .n-tree-node--selected .n-tree-node-content) {
	background: var(--block);
}
</style>
