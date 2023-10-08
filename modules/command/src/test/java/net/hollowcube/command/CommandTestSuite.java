package net.hollowcube.command;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("net.hollowcube.command")
public class CommandTestSuite {

    /*

    WOULD LIKE TO SUPPORT FULLY SERVER SIDE COMPLETION (optional, probably not all args will support this)
     - AS WELL AS CLIENT SIDE COMPLETION (where possible)



    VALID EXECUTION RESULTS
    - NO COMMAND FOUND (default to no command message)
    - UNKNOWN FAILURE (exception, etc)
    - SUCCESS

    VALID TAB COMPLETION RESULTS
    - NO COMMAND FOUND (do nothing)
    - COMPLETIONS (could be empty)
    - UNKNOWN FAILURE (exception, etc)


    FLOWS:
    - COMPLETION: PARSE, COMPLETIONS
    - EXECUTION: PARSE, EXECUTE

    TEST LIST
    - tab completion
      - ab
      - cd

    - execution
      - ab
      - cd


     */
}
