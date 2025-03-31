import ts from "typescript";
import path from "path";
import chokidar from "chokidar";
import refresh from "react-refresh-typescript";

const isRelease = process.argv.includes("--release");
const isWatchMode = process.argv.includes("-w");

const configPath = path.resolve("tsconfig.json");
const configFile = ts.readConfigFile(configPath, ts.sys.readFile);
const config = ts.parseJsonConfigFileContent(
    configFile.config,
    ts.sys,
    path.dirname(configPath)
);

const host = ts.createIncrementalCompilerHost(config.options);
let program = ts.createIncrementalProgram({
    rootNames: config.fileNames,
    options: config.options,
    host
});

// Function to compile specific files (or all initially)
function compile(changedFiles?: string[]) {
    console.log(changedFiles ? `Recompiling: ${changedFiles.join(", ")}` : "Full compilation...");

    // Reuse the existing program, updating only changed files
    if (changedFiles) {
        program = ts.createIncrementalProgram({
            rootNames: config.fileNames,
            options: config.options,
            host,
        });
    }

    const emitResult = program.emit(
        undefined,
        undefined,
        undefined,
        undefined,
        {before: isRelease ? [] : [refresh()]}
    );

    const allDiagnostics = ts.getPreEmitDiagnostics(program.getProgram()).concat(emitResult.diagnostics);
    allDiagnostics.forEach((diagnostic) => {
        if (diagnostic.file) {
            const {line, character} = ts.getLineAndCharacterOfPosition(diagnostic.file, diagnostic.start!);
            const message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n");
            console.error(`${diagnostic.file.fileName} (${line + 1},${character + 1}): ${message}`);
        } else {
            console.error(ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n"));
        }
    });

    if (emitResult.emitSkipped) {
        console.error("Compilation failed.");
    } else {
        console.log("Compilation successful!");
    }
}

// Run the full compilation initially
compile();

if (isWatchMode) {
    console.log("Watching for file changes...");

    chokidar.watch("src").on("change", (file) => {
        console.log(`File changed: ${file}`);
        compile([file]); // Only compile the changed file
    });
}
