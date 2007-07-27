package edu.psu.geovista.app.spreadsheet.functions;

import edu.psu.geovista.app.spreadsheet.exception.NoReferenceException;
import edu.psu.geovista.app.spreadsheet.exception.ParserException;
import edu.psu.geovista.app.spreadsheet.formula.Node;

/*
 * Description:
 * Date: Apr 1, 2003
 * Time: 9:42:45 PM
 * @author Jin Chen
 */

public class FunctionLog extends FunctionSP {

    protected Number doFun(Node node)throws ParserException,NoReferenceException {
        return new Float(Math.log(getSingleParameter(node)));
    }

    public String getUsage() {
       	return "LOG(value)";
    }

    public String getDescription() {
       return "Returns the logarithm of a number to the base e.";
    }
}
