import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMutation, useQuery } from '@vue/apollo-composable';
import { gql } from '@apollo/client/core';
import { marked } from 'marked';
import AdminShell from '../components/AdminShell.vue';
const route = useRoute();
const router = useRouter();
const blogId = computed(() => (route.params.id ? Number(route.params.id) : null));
const isEdit = computed(() => blogId.value !== null);
const message = ref('');
const editorRef = ref(null);
const previewRef = ref(null);
const imageInputRef = ref(null);
const videoInputRef = ref(null);
const isUploading = ref(false);
let signTimer = null;
const uploads = ref([]);
const form = reactive({
    title: '',
    summary: '',
    content: '',
    categoryId: null,
    coverUrl: '',
    published: false,
    tagIds: []
});
const { result: metaResult } = useQuery(gql `
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
`);
const categories = computed(() => metaResult.value?.categories ?? []);
const tags = computed(() => metaResult.value?.tags ?? []);
const categoryOptions = computed(() => categories.value.map((cat) => ({ label: cat.name, value: cat.id })));
const tagOptions = computed(() => tags.value.map((tag) => ({ label: tag.name, value: tag.id })));
const blogQueryOptions = computed(() => ({ enabled: isEdit.value }));
const { result: blogResult } = useQuery(gql `
    query Blog($id: ID!) {
      blog(id: $id) {
        id
        title
        summary
        content
        categoryId
        coverUrl
        published
        tags {
          id
        }
      }
    }
  `, () => ({ id: blogId.value }), blogQueryOptions);
watch(() => blogResult.value?.blog, (blog) => {
    if (!blog)
        return;
    form.title = blog.title;
    form.summary = blog.summary ?? '';
    form.content = blog.content;
    form.categoryId = blog.categoryId ? Number(blog.categoryId) : null;
    form.coverUrl = blog.coverUrl ?? '';
    form.published = blog.published;
    form.tagIds = blog.tags?.map((tag) => Number(tag.id)) ?? [];
}, { immediate: true });
const previewHtml = computed(() => {
    if (!form.content)
        return '';
    const renderer = new marked.Renderer();
    renderer.image = (token) => {
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
        const headingHtml = marked.parseInline(token.text);
        return `<h${level}>${headingHtml}</h${level}>`;
    };
    return marked.parse(form.content, { renderer });
});
watch(() => previewHtml.value, () => {
    scheduleSignMedia();
});
const { mutate: createBlog } = useMutation(gql `
  mutation CreateBlog($input: BlogInput!) {
    createBlog(input: $input) {
      id
    }
  }
`);
const { mutate: updateBlog } = useMutation(gql `
  mutation UpdateBlog($id: ID!, $input: BlogInput!) {
    updateBlog(id: $id, input: $input) {
      id
    }
  }
`);
const appendSnippet = (snippet) => {
    insertAtCursor(snippet);
};
const insertAtCursor = (snippet) => {
    const textarea = editorRef.value;
    if (!textarea) {
        const suffix = form.content ? '\n\n' : '';
        form.content += `${suffix}${snippet}`;
        return;
    }
    const start = textarea.selectionStart ?? form.content.length;
    const end = textarea.selectionEnd ?? form.content.length;
    const prefix = form.content.slice(0, start);
    const suffix = form.content.slice(end);
    const spacer = prefix && !prefix.endsWith('\n') ? '\n\n' : '';
    const trail = suffix && !suffix.startsWith('\n') ? '\n\n' : '';
    form.content = `${prefix}${spacer}${snippet}${trail}${suffix}`;
};
const triggerUpload = (kind) => {
    const input = kind === 'image' ? imageInputRef.value : videoInputRef.value;
    input?.click();
};
const onFileChange = async (event) => {
    const input = event.target;
    const files = input.files ? Array.from(input.files) : [];
    if (!files.length)
        return;
    input.value = '';
    await handleUploads(files);
};
const onPaste = async (event) => {
    const items = event.clipboardData?.items;
    if (!items)
        return;
    const files = Array.from(items)
        .filter((item) => item.kind === 'file')
        .map((item) => item.getAsFile())
        .filter((file) => Boolean(file));
    const mediaFiles = files.filter((file) => file.type.startsWith('image/') || file.type.startsWith('video/'));
    if (!mediaFiles.length)
        return;
    event.preventDefault();
    await handleUploads(mediaFiles);
};
const onDrop = async (event) => {
    const files = event.dataTransfer?.files ? Array.from(event.dataTransfer.files) : [];
    const mediaFiles = files.filter((file) => file.type.startsWith('image/') || file.type.startsWith('video/'));
    if (!mediaFiles.length)
        return;
    event.preventDefault();
    await handleUploads(mediaFiles);
};
const handleUploads = async (files) => {
    for (const file of files) {
        const isImage = file.type.startsWith('image/');
        const isVideo = file.type.startsWith('video/');
        if (!isImage && !isVideo) {
            continue;
        }
        await handleUpload(file, isImage ? 'image' : 'video');
    }
};
const handleUpload = async (file, kind) => {
    if (isUploading.value)
        return;
    isUploading.value = true;
    message.value = '正在上传...';
    const uploadId = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const previewUrl = URL.createObjectURL(file);
    uploads.value = [
        {
            id: uploadId,
            name: file.name,
            kind,
            progress: 0,
            previewUrl,
            done: false
        },
        ...uploads.value
    ];
    try {
        const uploadUrl = buildUploadUrl();
        const token = localStorage.getItem('simpleblog_token');
        const formData = new FormData();
        formData.append('file', file);
        if (form.categoryId) {
            formData.append('categoryId', String(form.categoryId));
        }
        if (blogId.value) {
            formData.append('blogId', String(blogId.value));
        }
        const data = await uploadWithProgress(uploadUrl, formData, token, (progress) => {
            updateUploadProgress(uploadId, progress);
        });
        const snippet = buildMediaSnippet(data);
        insertAtCursor(snippet);
        message.value = kind === 'image' ? '图片已插入' : '视频已插入';
        markUploadDone(uploadId);
    }
    catch (error) {
        message.value = error?.message || '上传失败';
        markUploadDone(uploadId, true);
    }
    finally {
        isUploading.value = false;
    }
};
const uploadWithProgress = (url, formData, token, onProgress) => {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', url);
        if (token) {
            xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        }
        xhr.upload.onprogress = (event) => {
            if (!event.lengthComputable)
                return;
            const percent = Math.round((event.loaded / event.total) * 100);
            onProgress(percent);
        };
        xhr.onerror = () => reject(new Error('Upload failed'));
        xhr.onload = () => {
            try {
                const data = JSON.parse(xhr.responseText || '{}');
                if (xhr.status >= 200 && xhr.status < 300) {
                    resolve(data);
                }
                else {
                    reject(new Error(data?.message || 'Upload failed'));
                }
            }
            catch (error) {
                reject(error);
            }
        };
        xhr.send(formData);
    });
};
const updateUploadProgress = (id, progress) => {
    const target = uploads.value.find((item) => item.id === id);
    if (!target)
        return;
    target.progress = progress;
};
const markUploadDone = (id, failed = false) => {
    const target = uploads.value.find((item) => item.id === id);
    if (!target)
        return;
    target.progress = failed ? target.progress : 100;
    target.done = true;
    setTimeout(() => {
        URL.revokeObjectURL(target.previewUrl);
    }, 2000);
};
const buildUploadUrl = () => {
    const api = import.meta.env.VITE_API_URL ?? 'http://localhost:8888/graphql';
    return api.replace(/\/graphql\/?$/, '') + '/api/uploads';
};
const scheduleSignMedia = () => {
    if (signTimer) {
        window.clearTimeout(signTimer);
    }
    signTimer = window.setTimeout(() => {
        signMediaSources();
    }, 180);
};
const signMediaSources = async () => {
    const root = previewRef.value;
    if (!root)
        return;
    const targets = Array.from(root.querySelectorAll('img, video source'));
    const urls = Array.from(new Set(targets
        .map((node) => node.getAttribute('src'))
        .filter((value) => Boolean(value))));
    if (!urls.length)
        return;
    try {
        const signedUrls = await fetchSignedUrls(urls);
        if (!signedUrls)
            return;
        targets.forEach((node) => {
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
        // ignore signing failures
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
const isVideoUrl = (value) => {
    const clean = value.split('?')[0].split('#')[0].toLowerCase();
    return (clean.endsWith('.mp4') ||
        clean.endsWith('.webm') ||
        clean.endsWith('.ogg') ||
        clean.endsWith('.mov') ||
        clean.endsWith('.m4v'));
};
const escapeHtmlAttr = (value) => {
    return value
        .replaceAll('&', '&amp;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
};
const buildMediaSnippet = (data) => {
    const name = data.name || 'media';
    const contentType = data.contentType || '';
    if (contentType.startsWith('image/')) {
        return `![${name}](${data.url})`;
    }
    if (contentType.startsWith('video/')) {
        return `<video controls><source src="${data.url}" type="${contentType}"></video>`;
    }
    return `[${name}](${data.url})`;
};
const onSubmit = async () => {
    message.value = '';
    const payload = {
        title: form.title,
        summary: form.summary,
        content: form.content,
        categoryId: form.categoryId,
        coverUrl: form.coverUrl,
        published: form.published,
        tagIds: form.tagIds
    };
    if (isEdit.value && blogId.value) {
        await updateBlog({ id: blogId.value, input: payload });
        message.value = '已更新';
    }
    else {
        const result = await createBlog({ input: payload });
        const id = result?.data?.createBlog?.id;
        message.value = '已创建';
        if (id) {
            router.push(`/admin/editor/${id}`);
        }
    }
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {[typeof AdminShell, typeof AdminShell, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(AdminShell, new AdminShell({
    title: (__VLS_ctx.isEdit ? '编辑文章' : '新建文章'),
    subtitle: "Markdown 编辑与预览",
}));
const __VLS_1 = __VLS_0({
    title: (__VLS_ctx.isEdit ? '编辑文章' : '新建文章'),
    subtitle: "Markdown 编辑与预览",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
var __VLS_3 = {};
__VLS_2.slots.default;
{
    const { actions: __VLS_thisSlot } = __VLS_2.slots;
    const __VLS_4 = {}.NButton;
    /** @type {[typeof __VLS_components.NButton, typeof __VLS_components.nButton, typeof __VLS_components.NButton, typeof __VLS_components.nButton, ]} */ ;
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        ...{ 'onClick': {} },
        type: "primary",
        ...{ class: "btn btn-primary" },
    }));
    const __VLS_6 = __VLS_5({
        ...{ 'onClick': {} },
        type: "primary",
        ...{ class: "btn btn-primary" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    let __VLS_8;
    let __VLS_9;
    let __VLS_10;
    const __VLS_11 = {
        onClick: (__VLS_ctx.onSubmit)
    };
    __VLS_7.slots.default;
    var __VLS_7;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card editor-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "editor-meta" },
});
const __VLS_12 = {}.NForm;
/** @type {[typeof __VLS_components.NForm, typeof __VLS_components.nForm, typeof __VLS_components.NForm, typeof __VLS_components.nForm, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    labelPlacement: "top",
}));
const __VLS_14 = __VLS_13({
    labelPlacement: "top",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
const __VLS_16 = {}.NFormItem;
/** @type {[typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    label: "标题",
}));
const __VLS_18 = __VLS_17({
    label: "标题",
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
const __VLS_20 = {}.NInput;
/** @type {[typeof __VLS_components.NInput, typeof __VLS_components.nInput, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    value: (__VLS_ctx.form.title),
    placeholder: "文章标题",
}));
const __VLS_22 = __VLS_21({
    value: (__VLS_ctx.form.title),
    placeholder: "文章标题",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
var __VLS_19;
const __VLS_24 = {}.NFormItem;
/** @type {[typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    label: "摘要",
}));
const __VLS_26 = __VLS_25({
    label: "摘要",
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
__VLS_27.slots.default;
const __VLS_28 = {}.NInput;
/** @type {[typeof __VLS_components.NInput, typeof __VLS_components.nInput, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    value: (__VLS_ctx.form.summary),
    type: "textarea",
    placeholder: "一句话概括文章内容",
    autosize: ({ minRows: 2, maxRows: 4 }),
}));
const __VLS_30 = __VLS_29({
    value: (__VLS_ctx.form.summary),
    type: "textarea",
    placeholder: "一句话概括文章内容",
    autosize: ({ minRows: 2, maxRows: 4 }),
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
var __VLS_27;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "meta-grid" },
});
const __VLS_32 = {}.NFormItem;
/** @type {[typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    label: "分类",
}));
const __VLS_34 = __VLS_33({
    label: "分类",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
__VLS_35.slots.default;
const __VLS_36 = {}.NSelect;
/** @type {[typeof __VLS_components.NSelect, typeof __VLS_components.nSelect, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    value: (__VLS_ctx.form.categoryId),
    options: (__VLS_ctx.categoryOptions),
    placeholder: "请选择分类",
}));
const __VLS_38 = __VLS_37({
    value: (__VLS_ctx.form.categoryId),
    options: (__VLS_ctx.categoryOptions),
    placeholder: "请选择分类",
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
var __VLS_35;
const __VLS_40 = {}.NFormItem;
/** @type {[typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
    label: "标签",
}));
const __VLS_42 = __VLS_41({
    label: "标签",
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
__VLS_43.slots.default;
const __VLS_44 = {}.NSelect;
/** @type {[typeof __VLS_components.NSelect, typeof __VLS_components.nSelect, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
    value: (__VLS_ctx.form.tagIds),
    multiple: true,
    options: (__VLS_ctx.tagOptions),
    placeholder: "请选择标签",
}));
const __VLS_46 = __VLS_45({
    value: (__VLS_ctx.form.tagIds),
    multiple: true,
    options: (__VLS_ctx.tagOptions),
    placeholder: "请选择标签",
}, ...__VLS_functionalComponentArgsRest(__VLS_45));
var __VLS_43;
const __VLS_48 = {}.NFormItem;
/** @type {[typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
    label: "封面图",
}));
const __VLS_50 = __VLS_49({
    label: "封面图",
}, ...__VLS_functionalComponentArgsRest(__VLS_49));
__VLS_51.slots.default;
const __VLS_52 = {}.NInput;
/** @type {[typeof __VLS_components.NInput, typeof __VLS_components.nInput, ]} */ ;
// @ts-ignore
const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
    value: (__VLS_ctx.form.coverUrl),
    placeholder: "https://example.com/cover.jpg",
}));
const __VLS_54 = __VLS_53({
    value: (__VLS_ctx.form.coverUrl),
    placeholder: "https://example.com/cover.jpg",
}, ...__VLS_functionalComponentArgsRest(__VLS_53));
var __VLS_51;
const __VLS_56 = {}.NFormItem;
/** @type {[typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, typeof __VLS_components.NFormItem, typeof __VLS_components.nFormItem, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    label: "发布状态",
}));
const __VLS_58 = __VLS_57({
    label: "发布状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
__VLS_59.slots.default;
const __VLS_60 = {}.NSwitch;
/** @type {[typeof __VLS_components.NSwitch, typeof __VLS_components.nSwitch, typeof __VLS_components.NSwitch, typeof __VLS_components.nSwitch, ]} */ ;
// @ts-ignore
const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
    value: (__VLS_ctx.form.published),
}));
const __VLS_62 = __VLS_61({
    value: (__VLS_ctx.form.published),
}, ...__VLS_functionalComponentArgsRest(__VLS_61));
__VLS_63.slots.default;
{
    const { checked: __VLS_thisSlot } = __VLS_63.slots;
}
{
    const { unchecked: __VLS_thisSlot } = __VLS_63.slots;
}
var __VLS_63;
var __VLS_59;
var __VLS_15;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "editor-body" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "editor-toolbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('# 标题');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('## 二级标题');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('**加粗文本**');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('_斜体文本_');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('> 引用内容');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('- 列表项');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('```bash\n\n```');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.appendSnippet('![图片描述](url)');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.triggerUpload('image');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.triggerUpload('video');
        } },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "editor-split" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    ...{ onPaste: (__VLS_ctx.onPaste) },
    ...{ onDrop: (__VLS_ctx.onDrop) },
    ...{ onDragover: () => { } },
    ...{ onDragenter: () => { } },
    ref: "editorRef",
    value: (__VLS_ctx.form.content),
    placeholder: "开始写下你的 Markdown 内容...",
});
/** @type {typeof __VLS_ctx.editorRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "editor-preview markdown" },
    ref: "previewRef",
});
__VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.previewHtml) }, null, null);
/** @type {typeof __VLS_ctx.previewRef} */ ;
if (__VLS_ctx.uploads.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "upload-queue" },
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.uploads))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (item.id),
            ...{ class: "upload-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "upload-preview" },
        });
        if (item.kind === 'image') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
                src: (item.previewUrl),
                alt: (item.name),
            });
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.video, __VLS_intrinsicElements.video)({
                src: (item.previewUrl),
                muted: true,
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "upload-meta" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (item.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "upload-progress" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: ({ width: `${item.progress}%` }) },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
        (item.progress);
    }
}
if (__VLS_ctx.message) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "status-text" },
    });
    (__VLS_ctx.message);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (...[$event]) => {
            __VLS_ctx.onFileChange($event);
        } },
    ref: "imageInputRef",
    type: "file",
    accept: "image/*",
    multiple: true,
    ...{ class: "file-input-hidden" },
});
/** @type {typeof __VLS_ctx.imageInputRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (...[$event]) => {
            __VLS_ctx.onFileChange($event);
        } },
    ref: "videoInputRef",
    type: "file",
    accept: "video/*",
    multiple: true,
    ...{ class: "file-input-hidden" },
});
/** @type {typeof __VLS_ctx.videoInputRef} */ ;
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['editor-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['editor-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['meta-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['editor-body']} */ ;
/** @type {__VLS_StyleScopedClasses['editor-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['editor-split']} */ ;
/** @type {__VLS_StyleScopedClasses['editor-preview']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-queue']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-item']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-preview']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-progress']} */ ;
/** @type {__VLS_StyleScopedClasses['status-text']} */ ;
/** @type {__VLS_StyleScopedClasses['file-input-hidden']} */ ;
/** @type {__VLS_StyleScopedClasses['file-input-hidden']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            AdminShell: AdminShell,
            isEdit: isEdit,
            message: message,
            editorRef: editorRef,
            previewRef: previewRef,
            imageInputRef: imageInputRef,
            videoInputRef: videoInputRef,
            uploads: uploads,
            form: form,
            categoryOptions: categoryOptions,
            tagOptions: tagOptions,
            previewHtml: previewHtml,
            appendSnippet: appendSnippet,
            triggerUpload: triggerUpload,
            onFileChange: onFileChange,
            onPaste: onPaste,
            onDrop: onDrop,
            onSubmit: onSubmit,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
