import {defineConfig} from 'vite'

export default defineConfig({
    base: "/frontend-vanilla/",
    server: {
        port: 5172,
        strictPort: true,
    },
    build: {
        outDir: "build/dist",
        rollupOptions: {
            output: {
                entryFileNames: `[name].js`,
                chunkFileNames: `[name].js`,
                assetFileNames: `[name].[ext]`
            }
        }
    }
})
