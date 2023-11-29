package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Command 不能为空");
            System.exit(0);
        }
        String firstArg = args[0];
        if (!firstArg.equals("init")) {
            Repository.checkIfInitialized();
        }
        switch (firstArg) {
            case "init" -> {
                assertArgsNum(args, 1);
                Repository.init();
            }
            case "add" -> {
                assertArgsNum(args, 2);
                Repository.add(args[1]);
            }
            case "commit" -> {
                assertArgsNum(args, 2);
                Repository.commit(args[1]);
            }
            case "rm" -> {
                assertArgsNum(args, 2);
                Repository.rm(args[1]);
            }
            case "log" -> {
                assertArgsNum(args, 1);
                Repository.log();
            }
            case "global-log" -> {
                assertArgsNum(args, 1);
                Repository.global_log();
            }
            case "find" -> {
                assertArgsNum(args, 2);
                Repository.find(args[1]);
            }
            case "status" -> {
                assertArgsNum(args, 1);
                Repository.status();
            }
            case "checkout" -> {
                Repository repository = new Repository();
                switch (args.length) {
                    case 3 -> {
                        if (!args[1].equals("--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        /* * checkout -- [file name] */
                        repository.checkout(args[2]);
                    }
                    case 4 -> {
                        if (!args[2].equals("--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        /* * checkout [commit id] -- [file name] */
                        repository.checkout(args[1], args[3]);
                    }
                    case 2 ->
                        /* * checkout [branch name] */
                            repository.checkoutBranch(args[1]);
                    default -> {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                }
            }
            case "branch" -> {
                assertArgsNum(args, 2);
                Repository.branch(args[1]);
            }
            case "rm-branch" -> {
                assertArgsNum(args, 2);
                Repository.rmbranch(args[1]);
            }
            case "reset" -> {
                assertArgsNum(args, 2);
                Repository.reset(args[1]);
            }
            case "merge" -> {
                assertArgsNum(args, 2);
                Repository.merge(args[1]);
            }
        }
    }

    public static void assertArgsNum(String[] args, int num) {
        if (args.length != num) {
            System.out.printf("参数数量错误，应该为%d个\n", num);
            System.exit(0);
        }
    }
}
