type ThemeMode = 'light' | 'dark' | 'system';
export declare const useUiStore: import("pinia").StoreDefinition<"ui", {
    theme: ThemeMode;
}, {}, {
    applyTheme(): void;
    setTheme(theme: ThemeMode): void;
    toggleTheme(): void;
}>;
export {};
