import { useRouter } from 'vue-router';
import { computed } from 'vue';
import { useUiStore } from '@/stores/ui';
import AwesomeButton from '@/components/bottons/AwesomeButton.vue';
import MacPlate from '@/components/plates/MacPlate.vue';
import { NButton, NConfigProvider, NText, NH1, NP, NSpace, NGrid, NGi, NStatistic, darkTheme } from 'naive-ui';
const router = useRouter();
const ui = useUiStore();
const naiveTheme = computed(() => {
    if (ui.theme === 'dark') {
        return darkTheme;
    }
    if (ui.theme === 'system') {
        const prefersDark = typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches;
        return prefersDark ? darkTheme : null;
    }
    return null;
});
const macPlateBackground = computed(() => (naiveTheme.value === darkTheme ? '#555' : '#f8fbfe'));
const naiveThemeOverrides = computed(() => {
    if (naiveTheme.value !== darkTheme) {
        return null;
    }
    return {
        common: {
            primaryColor: '#66d9d0',
            primaryColorHover: '#7fe3da',
            primaryColorPressed: '#55c6bd',
            bodyColor: 'transparent',
            cardColor: 'rgba(16, 24, 34, 0.65)'
        },
        Card: {
            color: 'rgba(16, 24, 34, 0.65)',
            borderColor: 'rgba(120, 190, 210, 0.16)',
            boxShadow: '0 18px 40px rgba(6, 12, 18, 0.45)'
        },
        Button: {
            color: 'rgba(18, 32, 40, 0.45)',
            colorHover: 'rgba(20, 36, 46, 0.6)',
            colorPressed: 'rgba(14, 28, 36, 0.7)',
            textColor: '#d7f4f0',
            textColorHover: '#e7fbf8',
            textColorPressed: '#bfece6'
        }
    };
});
const topics = [
    { label: 'Java', to: '/discover' },
    { label: 'Go', to: '/discover' },
    { label: 'Python', to: '/discover' },
    { label: 'GraphQL', to: '/discover' },
    { label: '计算机网络', to: '/discover' },
    { label: '计算机基础', to: '/discover' }
];
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['mac-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['mac-window']} */ ;
/** @type {__VLS_StyleScopedClasses['mac-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['mac-title']} */ ;
/** @type {__VLS_StyleScopedClasses['mac-window']} */ ;
/** @type {__VLS_StyleScopedClasses['dot']} */ ;
/** @type {__VLS_StyleScopedClasses['landing']} */ ;
/** @type {__VLS_StyleScopedClasses['topic-card']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-title']} */ ;
// CSS variable injection 
// CSS variable injection end 
const __VLS_0 = {}.NConfigProvider;
/** @type {[typeof __VLS_components.NConfigProvider, typeof __VLS_components.nConfigProvider, typeof __VLS_components.NConfigProvider, typeof __VLS_components.nConfigProvider, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    theme: (__VLS_ctx.naiveTheme),
    themeOverrides: (__VLS_ctx.naiveThemeOverrides),
}));
const __VLS_2 = __VLS_1({
    theme: (__VLS_ctx.naiveTheme),
    themeOverrides: (__VLS_ctx.naiveThemeOverrides),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
var __VLS_4 = {};
__VLS_3.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "landing container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "hero-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "hero-copy" },
});
const __VLS_5 = {}.NText;
/** @type {[typeof __VLS_components.NText, typeof __VLS_components.nText, typeof __VLS_components.NText, typeof __VLS_components.nText, ]} */ ;
// @ts-ignore
const __VLS_6 = __VLS_asFunctionalComponent(__VLS_5, new __VLS_5({
    depth: "3",
    ...{ class: "eyebrow" },
}));
const __VLS_7 = __VLS_6({
    depth: "3",
    ...{ class: "eyebrow" },
}, ...__VLS_functionalComponentArgsRest(__VLS_6));
__VLS_8.slots.default;
var __VLS_8;
const __VLS_9 = {}.NH1;
/** @type {[typeof __VLS_components.NH1, typeof __VLS_components.nH1, typeof __VLS_components.NH1, typeof __VLS_components.nH1, ]} */ ;
// @ts-ignore
const __VLS_10 = __VLS_asFunctionalComponent(__VLS_9, new __VLS_9({
    ...{ class: "hero-title" },
}));
const __VLS_11 = __VLS_10({
    ...{ class: "hero-title" },
}, ...__VLS_functionalComponentArgsRest(__VLS_10));
__VLS_12.slots.default;
var __VLS_12;
const __VLS_13 = {}.NP;
/** @type {[typeof __VLS_components.NP, typeof __VLS_components.nP, typeof __VLS_components.NP, typeof __VLS_components.nP, ]} */ ;
// @ts-ignore
const __VLS_14 = __VLS_asFunctionalComponent(__VLS_13, new __VLS_13({
    depth: "2",
    ...{ class: "hero-description" },
}));
const __VLS_15 = __VLS_14({
    depth: "2",
    ...{ class: "hero-description" },
}, ...__VLS_functionalComponentArgsRest(__VLS_14));
__VLS_16.slots.default;
var __VLS_16;
const __VLS_17 = {}.NSpace;
/** @type {[typeof __VLS_components.NSpace, typeof __VLS_components.nSpace, typeof __VLS_components.NSpace, typeof __VLS_components.nSpace, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
    ...{ class: "hero-actions" },
    size: "large",
}));
const __VLS_19 = __VLS_18({
    ...{ class: "hero-actions" },
    size: "large",
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
__VLS_20.slots.default;
/** @type {[typeof AwesomeButton, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(AwesomeButton, new AwesomeButton({
    ...{ 'onClick': {} },
    label: "开始阅读",
}));
const __VLS_22 = __VLS_21({
    ...{ 'onClick': {} },
    label: "开始阅读",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
let __VLS_24;
let __VLS_25;
let __VLS_26;
const __VLS_27 = {
    onClick: (...[$event]) => {
        __VLS_ctx.router.push('/blog');
    }
};
var __VLS_23;
const __VLS_28 = {}.NButton;
/** @type {[typeof __VLS_components.NButton, typeof __VLS_components.nButton, typeof __VLS_components.NButton, typeof __VLS_components.nButton, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    ...{ 'onClick': {} },
    size: "large",
    round: true,
    ghost: true,
}));
const __VLS_30 = __VLS_29({
    ...{ 'onClick': {} },
    size: "large",
    round: true,
    ghost: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
let __VLS_32;
let __VLS_33;
let __VLS_34;
const __VLS_35 = {
    onClick: (...[$event]) => {
        __VLS_ctx.router.push('/discover');
    }
};
__VLS_31.slots.default;
var __VLS_31;
var __VLS_20;
const __VLS_36 = {}.NGrid;
/** @type {[typeof __VLS_components.NGrid, typeof __VLS_components.nGrid, typeof __VLS_components.NGrid, typeof __VLS_components.nGrid, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    xGap: (24),
    cols: (3),
    ...{ class: "hero-metrics" },
}));
const __VLS_38 = __VLS_37({
    xGap: (24),
    cols: (3),
    ...{ class: "hero-metrics" },
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
__VLS_39.slots.default;
const __VLS_40 = {}.NGi;
/** @type {[typeof __VLS_components.NGi, typeof __VLS_components.nGi, typeof __VLS_components.NGi, typeof __VLS_components.nGi, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({}));
const __VLS_42 = __VLS_41({}, ...__VLS_functionalComponentArgsRest(__VLS_41));
__VLS_43.slots.default;
const __VLS_44 = {}.NStatistic;
/** @type {[typeof __VLS_components.NStatistic, typeof __VLS_components.nStatistic, typeof __VLS_components.NStatistic, typeof __VLS_components.nStatistic, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
    label: "精选文章",
}));
const __VLS_46 = __VLS_45({
    label: "精选文章",
}, ...__VLS_functionalComponentArgsRest(__VLS_45));
__VLS_47.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "metric-value" },
});
var __VLS_47;
var __VLS_43;
const __VLS_48 = {}.NGi;
/** @type {[typeof __VLS_components.NGi, typeof __VLS_components.nGi, typeof __VLS_components.NGi, typeof __VLS_components.nGi, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({}));
const __VLS_50 = __VLS_49({}, ...__VLS_functionalComponentArgsRest(__VLS_49));
__VLS_51.slots.default;
const __VLS_52 = {}.NStatistic;
/** @type {[typeof __VLS_components.NStatistic, typeof __VLS_components.nStatistic, typeof __VLS_components.NStatistic, typeof __VLS_components.nStatistic, ]} */ ;
// @ts-ignore
const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
    label: "主题路径",
}));
const __VLS_54 = __VLS_53({
    label: "主题路径",
}, ...__VLS_functionalComponentArgsRest(__VLS_53));
__VLS_55.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "metric-value" },
});
var __VLS_55;
var __VLS_51;
const __VLS_56 = {}.NGi;
/** @type {[typeof __VLS_components.NGi, typeof __VLS_components.nGi, typeof __VLS_components.NGi, typeof __VLS_components.nGi, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({}));
const __VLS_58 = __VLS_57({}, ...__VLS_functionalComponentArgsRest(__VLS_57));
__VLS_59.slots.default;
const __VLS_60 = {}.NStatistic;
/** @type {[typeof __VLS_components.NStatistic, typeof __VLS_components.nStatistic, typeof __VLS_components.NStatistic, typeof __VLS_components.nStatistic, ]} */ ;
// @ts-ignore
const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
    label: "周更新节奏",
}));
const __VLS_62 = __VLS_61({
    label: "周更新节奏",
}, ...__VLS_functionalComponentArgsRest(__VLS_61));
__VLS_63.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "metric-value" },
});
var __VLS_63;
var __VLS_59;
var __VLS_39;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "hero-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mac-window" },
});
/** @type {[typeof MacPlate, typeof MacPlate, ]} */ ;
// @ts-ignore
const __VLS_64 = __VLS_asFunctionalComponent(MacPlate, new MacPlate({
    ...{ class: "mac-body" },
    backgroundColor: (__VLS_ctx.macPlateBackground),
}));
const __VLS_65 = __VLS_64({
    ...{ class: "mac-body" },
    backgroundColor: (__VLS_ctx.macPlateBackground),
}, ...__VLS_functionalComponentArgsRest(__VLS_64));
__VLS_66.slots.default;
const __VLS_67 = {}.NSpace;
/** @type {[typeof __VLS_components.NSpace, typeof __VLS_components.nSpace, typeof __VLS_components.NSpace, typeof __VLS_components.nSpace, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({
    wrap: true,
}));
const __VLS_69 = __VLS_68({
    wrap: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_68));
__VLS_70.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.topics))) {
    const __VLS_71 = {}.NButton;
    /** @type {[typeof __VLS_components.NButton, typeof __VLS_components.nButton, typeof __VLS_components.NButton, typeof __VLS_components.nButton, ]} */ ;
    // @ts-ignore
    const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
        ...{ 'onClick': {} },
        key: (item.label),
        secondary: true,
        round: true,
        type: "info",
    }));
    const __VLS_73 = __VLS_72({
        ...{ 'onClick': {} },
        key: (item.label),
        secondary: true,
        round: true,
        type: "info",
    }, ...__VLS_functionalComponentArgsRest(__VLS_72));
    let __VLS_75;
    let __VLS_76;
    let __VLS_77;
    const __VLS_78 = {
        onClick: (...[$event]) => {
            __VLS_ctx.router.push(item.to);
        }
    };
    __VLS_74.slots.default;
    (item.label);
    var __VLS_74;
}
var __VLS_70;
var __VLS_66;
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['landing']} */ ;
/** @type {__VLS_StyleScopedClasses['container']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-copy']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-title']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-description']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-metrics']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-value']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-value']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-value']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['mac-window']} */ ;
/** @type {__VLS_StyleScopedClasses['mac-body']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            AwesomeButton: AwesomeButton,
            MacPlate: MacPlate,
            NButton: NButton,
            NConfigProvider: NConfigProvider,
            NText: NText,
            NH1: NH1,
            NP: NP,
            NSpace: NSpace,
            NGrid: NGrid,
            NGi: NGi,
            NStatistic: NStatistic,
            router: router,
            naiveTheme: naiveTheme,
            macPlateBackground: macPlateBackground,
            naiveThemeOverrides: naiveThemeOverrides,
            topics: topics,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
