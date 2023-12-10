import fs from "fs";
import esbuild from "esbuild";
import esbuildSvelte from "esbuild-svelte";
import sveltePreprocess from "svelte-preprocess";

// TODO: build using vite plugin instead. Allows HMR!
esbuild
    .build({
        entryPoints: [`./src/entry.ts`],
        bundle: true,
        outdir: "./build/dist",
        mainFields: ["svelte", "browser", "module", "main"],
        conditions: ["svelte", "browser"],
        // logLevel: `info`,
        minify: false,
        sourcemap: "inline",
        splitting: true,
        write: true,
        format: `esm`,
        plugins: [
            esbuildSvelte({
                preprocess: sveltePreprocess(),
            }),
        ],
    })
    .catch((error, location) => {
        console.warn(`Errors: `, error, location);
        process.exit(1);
    });
