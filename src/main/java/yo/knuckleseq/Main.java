package yo.knuckleseq;

public class Main {

    public static void main(String[] args) {
        var exitCode = new SonologCli(args).run(System.out, System.err);
        System.exit(exitCode);
    }
}
