import { defineStore } from 'pinia';
const canUseSystem = typeof window !== 'undefined' && typeof window.matchMedia === 'function';
const systemQuery = canUseSystem ? window.matchMedia('(prefers-color-scheme: dark)') : null;
let systemListenerAttached = false;
const getSystemTheme = () => (systemQuery?.matches ? 'dark' : 'light');
export const useUiStore = defineStore('ui', {
    state: () => ({
        theme: localStorage.getItem('simpleblog_theme') || 'light'
    }),
    actions: {
        applyTheme() {
            const resolvedTheme = this.theme === 'system' ? getSystemTheme() : this.theme;
            document.documentElement.setAttribute('data-theme', resolvedTheme);
            localStorage.setItem('simpleblog_theme', this.theme);
            if (systemQuery && !systemListenerAttached) {
                const handler = () => {
                    if (this.theme === 'system') {
                        this.applyTheme();
                    }
                };
                if ('addEventListener' in systemQuery) {
                    systemQuery.addEventListener('change', handler);
                }
                else {
                    systemQuery.addListener(handler);
                }
                systemListenerAttached = true;
            }
        },
        setTheme(theme) {
            this.theme = theme;
            this.applyTheme();
        },
        toggleTheme() {
            this.theme = this.theme === 'dark' ? 'light' : 'dark';
            this.applyTheme();
        }
    }
});
