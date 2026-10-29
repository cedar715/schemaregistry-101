package io.github.cedar715.srbackup;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class SrBackupTest {

    record Result(int exitCode, String out, String err) {
    }

    static Result run(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        var cmd = SrBackup.commandLine();
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        int rc = cmd.execute(args);
        return new Result(rc, out.toString(), err.toString());
    }

    @Test
    void helpExitsZero() {
        Result r = run("--help");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).contains("Usage: sr-backup");
    }

    @Test
    void versionIsPrinted() {
        Result r = run("--version");
        assertThat(r.exitCode()).isZero();
        assertThat(r.out()).startsWith("sr-backup ");
    }

    @Test
    void unknownOptionIsAUsageError() {
        Result r = run("--no-such-option");
        assertThat(r.exitCode()).isEqualTo(2);
        assertThat(r.err()).contains("Unknown option");
    }
}
