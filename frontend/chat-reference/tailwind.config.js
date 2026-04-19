/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        dark: {
          bg: '#1a1d23',
          sidebar: '#20232a',
          card: '#282c34',
          hover: '#2f3542',
          border: '#3a3f4b',
          text: '#e4e6eb',
          muted: '#8b949e',
          accent: '#58a6ff',
          sent: '#0084ff',
          received: '#3a3f4b',
        },
      },
    },
  },
  plugins: [],
};
