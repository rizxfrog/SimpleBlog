type __VLS_Props = {
    title: string;
    subtitle?: string;
};
declare var __VLS_30: {}, __VLS_32: {};
type __VLS_Slots = {} & {
    actions?: (props: typeof __VLS_30) => any;
} & {
    default?: (props: typeof __VLS_32) => any;
};
declare const __VLS_component: import("vue").DefineComponent<__VLS_Props, {}, {}, {}, {}, import("vue").ComponentOptionsMixin, import("vue").ComponentOptionsMixin, {}, string, import("vue").PublicProps, Readonly<__VLS_Props> & Readonly<{}>, {}, {}, {}, {}, string, import("vue").ComponentProvideOptions, false, {}, any>;
declare const _default: __VLS_WithSlots<typeof __VLS_component, __VLS_Slots>;
export default _default;
type __VLS_WithSlots<T, S> = T & {
    new (): {
        $slots: S;
    };
};
