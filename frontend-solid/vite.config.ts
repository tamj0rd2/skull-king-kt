import {defineConfig} from 'vite'
import fs from 'fs/promises'
import solid from 'vite-plugin-solid'

export default defineConfig({
    plugins: [solid()],
    base: "/frontend-solid/",
    server: {
        port: 5173,
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
