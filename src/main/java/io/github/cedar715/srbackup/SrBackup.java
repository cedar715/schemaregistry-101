package io.github.cedar715.srbackup;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Entry point of the {@code sr-backup} command line tool.
 *
 * Subcommands are added stage by stage (see the README roadmap).
 *
 * Exit codes: 0 ok, 1 failed, 2 bad usage.
 */
@Command(name = "sr-backup",
        mixinStandardHelpOptions = true,
        versionProvider = SrBackup.Version.class,
        description = "Back up Confluent Schema Registry by exporting its storage topic as raw bytes.")
public class SrBackup implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    static CommandLine commandLine() {
        return new CommandLine(new SrBackup())
                // one readable line on stderr instead of a stack trace
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    cmd.getErr().println("error: " + ex.getMessage());
                    return 1;
                });
    }

    /** No subcommand given: show what is available. */
    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    /** Version comes from the jar manifest, which Gradle fills from build.gradle.kts. */
    static final class Version implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String v = SrBackup.class.getPackage().getImplementationVersion();
            return new String[]{"sr-backup " + (v == null ? "dev" : v)};
        }
    }
}
