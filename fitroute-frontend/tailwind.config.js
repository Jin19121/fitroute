// tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{js,jsx}'],
    theme: {
        extend: {
            colors: {
                brand: {
                    blue: '#4A7BFF',
                    dark: '#1A1A1A',
                    cream: '#F9F7F5',
                    sand: '#E4E1DC',
                    muted: '#B8B4AE',
                    border: '#EDEAE5',
                },
            },
            borderRadius: {
                phone: '38px',
                inner: '30px',
            },
            animation: {
                'spin-slow': 'spin 2s linear infinite',
            },
        },
    },
    plugins: [],
};

// ─── vite.config.js ───────────────────────────────────────────────────────────
// (separate file in project root)
