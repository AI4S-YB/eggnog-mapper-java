import eggnogmapper.cli.CliArgs;
import eggnogmapper.cli.CliParser;
import eggnogmapper.common.CompatUtils;
import eggnogmapper.emapper.EmapperPipeline;

public class Main {
    public static void main(String[] args) {
        try {
            CliArgs cliArgs = CliParser.parse(args);
            if (cliArgs.help) {
                CliParser.printHelp();
                return;
            }
            if (cliArgs.version) {
                System.out.println(CompatUtils.readVersion());
                return;
            }
            EmapperPipeline pipeline = new EmapperPipeline(cliArgs);
            pipeline.run();
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid arguments: " + ex.getMessage());
            CliParser.printHelp();
            System.exit(2);
        } catch (Exception ex) {
            System.err.println("Execution failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
